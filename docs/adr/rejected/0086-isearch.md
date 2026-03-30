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
