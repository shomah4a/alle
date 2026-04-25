# ADR 0129: サーバー/クライアント機能 (emacsclient 相当)

## ステータス

採用

## コンテキスト

`EDITOR="alle --client"` として git commit のメッセージ編集に使いたい。
Emacs の server-mode / emacsclient に相当する機能がない。

外部プロセスから起動中の alle にファイルオープンを依頼し、
ユーザーが編集を完了するまでブロックする仕組みが必要である。

## 決定

### 通信方式

Java 21 の `UnixDomainSocketAddress` を用いた Unix Domain Socket 通信を採用する。
TCP ローカルソケットはポート競合やセキュリティの考慮が必要なため不採用。

ソケットパスは `$XDG_RUNTIME_DIR/alle/server` (未設定時は `/tmp/alle-server-<USER>`)。

### 通信プロトコル

テキスト行指向のプロトコルを採用する。バイナリプロトコルほどの効率は不要で、デバッグ容易性を優先する。

```
クライアント → サーバー:  OPEN <absolute-path>\n
サーバー → クライアント:  OPENED <buffer-name>\n
                         FINISHED\n
                         ERROR <message>\n
```

`ServerProtocol` クラスで sealed interface によるリクエスト/レスポンスの型定義と、
テキスト行とのエンコード/デコードを提供する。

### マイナーモードによる管理

Emacs の server-mode に倣い、クライアントから依頼されたバッファに `server` マイナーモードを付与する。

- `ServerMinorMode`: `C-x #` に `server-edit` コマンドをバインド
- `ServerEditCommand`: バッファ保存 → クライアントに完了通知 → マイナーモード無効化 → バッファ kill

### スレッドモデル

既存の 3 スレッド構成 (入力/ロジック/描画) に、サーバーの accept スレッドを追加する。

accept スレッドからロジックスレッドへの安全なアクション委譲のため、
EditorThread に `BlockingQueue<Runnable>` を追加する。
ロジックスレッドのメインループ先頭で `drainTo` によりノンブロッキングに全件取り出して実行する。
既存の `BlockingQueue<KeyStroke>` には変更を加えない。

### セッション管理

バッファ変数 (`setVariable`/`getVariable`) は `Optional<Object>` を返すため、
型安全にセッションを取得できない (unchecked cast が必要)。

そのため `ServerManager` 側で `MutableMap<Path, ServerSession>` を管理する方式を採用する。
ファイルパスをキーにセッションを引く。

### エントリポイント

同一の `alle` コマンドで `--client` オプションにより分岐する。
別 JAR (alleclient) は作成しない。

- `alle --client <file>`: クライアントモード。ソケットに接続してファイルオープンを依頼し、完了まで待機
- `alle` (引数なし or ファイルパス): 通常のエディタ起動 + サーバーソケットリッスン開始

### サーバー未起動時のクライアントの振る舞い

接続失敗した場合、stderr にエラーメッセージを出力して終了コード 1 で exit する。
自動起動 (Emacs の `--alternate-editor`) は初期実装では対応しない。

### stale socket detection

エディタが異常終了した場合にソケットファイルが残留する可能性がある。
起動時に既存ソケットファイルへの接続テストを行い、応答がなければ stale として削除する。

### バッファ kill ロジックの共通化

`ServerEditCommand` がバッファを kill する際、`KillBufferCommand` の `doKill` と同等のロジック
(代替バッファ選択、ウィンドウ切り替え、*scratch* 再作成) が必要である。
`KillBufferCommand` の `doKill` をユーティリティクラスに切り出して共用する。

### スレッドセーフティ

`ServerManager` のセッション管理マップ (`MutableMap<Path, ServerSession>`) への操作は
すべてロジックスレッド (EditorThread) 上で実行する。
accept スレッドでセッションを作成した後、`sessions.put` を含むすべての操作を
`actionQueue` 経由でロジックスレッドに委譲することで、同期なしでスレッドセーフティを実現する。

## モジュール配置

- `alle-core/server/`: ServerProtocol, ServerSession, ServerMinorMode, ServerEditCommand, ServerManager
- `alle-tui`: EditorThread のアクションキュー追加
- `alle-app`: Main.java の --client 分岐、ClientMain

## 動作フロー

1. alle 起動時に ServerManager がソケットリッスン開始
2. `git commit` → `alle --client /path/to/COMMIT_EDITMSG` が起動される
3. ClientMain がソケットに接続し `OPEN <path>` を送信
4. accept スレッドがリクエストを受け、Runnable キュー経由でロジックスレッドにファイルオープンを依頼
5. ロジックスレッドが PathOpenService.open でファイルを開き、server マイナーモードを付与
6. ユーザーが `C-x #` (server-edit) → バッファ保存 → FINISHED 送信 → バッファ kill
7. ClientMain が FINISHED を受信して終了コード 0 で exit
8. git がファイル内容をコミットメッセージとして採用
