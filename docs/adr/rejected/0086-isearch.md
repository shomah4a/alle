# ADR 0086: インクリメンタルサーチ（i-search）

## ステータス

却下

## 却下理由

Emacsのi-searchはメインウィンドウにカーソルを残したまま、ミニバッファにはエコー表示のみ行うモデルである。
本ADRではMinibufferInputPrompterを拡張してi-searchを実装する方針としたが、
MinibufferInputPrompterは「ミニバッファにフォーカスを移して入力を受け付ける」設計であり、
i-searchの「メインウィンドウにフォーカスを残す」モデルとは根本的に異なることが判明した。

具体的な問題:
- i-searchではカーソル移動キーやRETなどi-search以外のキーで即座に検索を終了する
- 現在のMinibufferInputPrompterはRET確定/C-gキャンセルの2経路のみ
- i-search中のカーソルはメインウィンドウ側にあるべきだが、MinibufferInputPrompterはアクティブウィンドウをミニバッファに切り替える

i-searchはMinibufferInputPrompterとは独立した仕組みとして再設計が必要。

## コンテキスト

エディタにテキスト検索機能がない。
Emacsの `isearch-forward` (C-s) / `isearch-backward` (C-r) に相当するインクリメンタルサーチを導入する。

i-searchはミニバッファへの入力が変更されるたびにバッファ内を検索し、カーソルを移動・ハイライトする。
C-s/C-rで次/前のマッチに移動し、RETで確定、C-gでキャンセル（元の位置に戻る）。

## 決定

### ミニバッファプロンプトへのマイナーモード指定

i-searchはミニバッファ入力中に独自のキーバインド（C-s:次マッチ、C-r:前マッチ等）と、入力変更時のインクリメンタル検索動作が必要である。

`InputPrompter` にマイナーモードを指定できるオーバーロードを追加する。
ミニバッファのバッファにマイナーモードをenableすることで、i-search固有のキーバインドをマイナーモード経由で提供する。

この仕組みにより、i-search以外の将来的なプロンプトカスタマイズにも対応できる。

CommandLoopのキー解決優先順位:
1. ローカルキーマップ（RET確定、C-gキャンセル等のミニバッファ基本操作）
2. マイナーモード（i-search固有のキーバインド）
3. メジャーモード
4. グローバルキーマップ

### i-searchの実装構造

- `ISearchMode` (MinorMode) — i-search用キーマップと状態管理
- `ISearchForwardCommand` / `ISearchBackwardCommand` (Command) — i-search起動コマンド
- `BufferSearcher` — バッファ内テキスト検索エンジン

### 検索エンジン

初期実装では `buffer.getText()` でバッファ全体を文字列化し、`String.indexOf` / `String.lastIndexOf` で検索する。
大規模バッファでのパフォーマンス最適化（GapBufferのセグメント検索等）は、問題が顕在化した段階で対処する。

### ラップアラウンド

バッファ末尾（先頭）到達後、先頭（末尾）から再検索するラップアラウンドを実装する。
ラップアラウンド発生時は "Wrapped I-search" をメッセージ表示する。

### ハイライト管理

検索マッチ位置にFace (`ISEARCH_MATCH`) を付与してハイライトする。
確定・キャンセル時に確実にハイライトを除去する必要がある。

既存の `removeFace(start, end)` は指定範囲のすべてのFaceエントリを削除するため、シンタックスハイライトも巻き込んでしまう。
FaceName指定で特定のFaceのみを除去する `removeFaceByName(start, end, FaceName)` を導入する。

### ミニバッファ競合防止

i-search中にC-x等のプレフィックスキーがグローバルキーマップに到達し、find-file等のプロンプト付きコマンドが起動するとミニバッファが競合する。
i-searchマイナーモードのキーマップでC-xプレフィックスをno-opにバインドして塞ぐ。

### コードポイント単位の扱い

バッファのオフセットはコードポイント単位である。
`String.indexOf` はchar単位のオフセットを返すため、BufferSearcher内でchar offset → codepoint offset の変換を行う。

## 試行結果

上記方針に基づいて実装を行い、ビルド・全テスト通過まで確認した。
しかし以下の問題が実装中および事後レビューで判明し、方針自体を却下した。

### 試行中に発見された問題

1. **ローカルキーマップのdefaultCommand衝突**: MinibufferInputPrompterが作成するローカルキーマップのSelfInsertCommand（defaultCommand）が、i-searchマイナーモードのISearchSelfInsertCommand（defaultCommand）より優先される。CommandLoop.resolveKey()はローカルキーマップ→マイナーモードの順に解決するため、文字入力時にインクリメンタル検索が動作しなかった。Keymap.clearDefaultCommand()を追加して回避したが、本質的な設計の歪みを示していた。

2. **removeFaceの既存ハイライト破壊**: removeFace(start, end)は範囲内のすべてのFaceを削除するため、i-searchハイライト除去時にシンタックスハイライトも消える。RangeList.removeByValue()とremoveFaceByName()を追加して対処した。この部分は再設計でも流用可能。

### Emacsの実装調査で判明した仕様ズレ

Emacsのi-search実装（isearch.el）を調査した結果、以下の根本的な仕様ズレが判明した。

#### フォーカスモデル

| 項目 | Emacs | 本ADRの方針 |
|------|-------|-------------|
| カーソル位置 | メインウィンドウ | ミニバッファ |
| フォーカス | メインウィンドウのまま | activateMinibuffer()でミニバッファに移動 |
| 文字入力先 | i-searchがクエリ文字列を独自管理 | ミニバッファのバッファに挿入 |

#### キーマップ制御

Emacsでは `overriding-terminal-local-map` にi-search用キーマップをセットする。
これはEmacsのキーマップ検索順序で最上位に位置し、全キー入力をi-searchが横取りする。

本ADRではミニバッファのローカルキーマップ＋マイナーモードで制御する方針だったが、
これはミニバッファにフォーカスがある前提の設計であり、メインウィンドウにフォーカスを残すモデルでは機能しない。

#### 終了条件

| 項目 | Emacs | 本ADRの方針 |
|------|-------|-------------|
| RET | 検索確定（カーソルをマッチ位置に残す） | ミニバッファの入力確定（プロンプト終了） |
| カーソル移動キー | i-search終了→メインウィンドウでカーソル移動 | ミニバッファ内のカーソル移動 |
| その他の非対象キー | i-search終了→そのキーを通常通り実行 | C-xをno-opで塞ぐ（自然な終了ができない） |

Emacsでは `search-exit-option` 変数で制御され、デフォルトでは制御文字でi-searchを終了し、
そのキーを通常通り処理する。本ADRではこの挙動を実現する手段がなかった。

#### 再開フロー

Emacsではi-search確定後、再度C-sを押すと前回のクエリで検索を再開できる。
本ADRではInputHistoryに履歴を保存しているが、再開時に自動でクエリをセットする仕組みがなかった。

## 再設計に向けた示唆

Emacsの設計から読み取れるAlleへの適用方針:

1. **最上位キーマップの導入**: CommandLoop.resolveKey()の先頭で「overriding keymap」をチェックする仕組みを追加する。i-search開始時にセットし、終了時にクリアする。
2. **メインウィンドウにフォーカスを残す**: frame.activateMinibuffer()を呼ばない。ミニバッファにはメッセージ表示（messageBuffer相当）のみ。
3. **非対象キーでの自然な終了**: overriding keymapで未定義のキーが来たら、i-searchを終了してからそのキーを通常のキーマップ解決に回す。
4. **クエリの独自管理**: ミニバッファのバッファへの挿入ではなく、i-searchが内部的にクエリ文字列を保持し、ミニバッファにはエコー表示のみ行う。
5. **本ADRで実装したremoveFaceByName、BufferSearcher、SearchResult等は再利用可能**。
