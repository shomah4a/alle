# ADR 0084: transient-mark-mode の導入

## ステータス

承認

## コンテキスト

alleエディタではマーク（リージョンの始点）を設定した後、明示的にクリアするまでマークが残り続ける。
リージョンがハイライト表示されないため、マークが設定されているかどうかがユーザーに分かりにくい。
また、意図せずマークが残った状態でリージョン操作コマンド（kill-region等）を実行してしまうリスクがある。

Emacsのtransient-mark-modeでは、マーク設定後にリージョンがハイライト表示され、
コマンド実行後にマークが自動的に非アクティブ化される。

## 決定

alleではtransient-mark-mode相当の動作をデフォルトかつ唯一の動作として実装する。

### 設計方針

- `markActive` のような中間状態フラグは導入しない
- コマンド実行後に `clearMark()` することで、mark == null をリージョン無効の唯一の表現とする
- マークを保持すべきコマンド（移動系、set-mark）は `keepsRegionActive()` で true を返す

### Commandインターフェースの拡張

`keepsRegionActive()` デフォルトメソッドを追加する（デフォルト false）。
移動系コマンドおよびset-markコマンドでオーバーライドして true を返す。

### CommandLoopの変更

コマンド実行完了後、`keepsRegionActive() == false` であれば `clearMark()` を呼び出す。

### リージョンハイライト

- `RenderSnapshotFactory` で mark != null のときリージョン範囲を行ごとに計算してスナップショットに含める
- `ScreenRenderer` でREVERSE（色反転）によりリージョンをハイライトする
- 背景色方式ではなくREVERSEを採用する理由: シンタックスハイライトの前景色と背景色が近くなりコントラストが失われる問題を回避するため

### keepsRegionActive() == true のコマンド

- set-mark
- forward-char, backward-char
- next-line, previous-line
- beginning-of-line, end-of-line
- scroll-up, scroll-down

### diredコマンドへの影響

`TreeDiredToggleMarkCommand` はコマンド実行中にregionを参照するため、
コマンド実行完了後の `clearMark()` で問題ない。

## 帰結

- マーク設定後にリージョンがハイライト表示され、ユーザーにリージョンの状態が視覚的に分かる
- コマンド実行後にマークが自動クリアされるため、意図しないリージョン操作のリスクが軽減される
- 新しい移動系コマンドを追加する際に `keepsRegionActive()` のオーバーライドが必要になる（忘れた場合はリージョンが即消えるだけで、fail-safe）
