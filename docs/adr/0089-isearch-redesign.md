# ADR 0089: インクリメンタルサーチ（i-search）再設計

## ステータス

承認

## コンテキスト

エディタにテキスト検索機能がない。
Emacsの `isearch-forward` (C-s) / `isearch-backward` (C-r) に相当するインクリメンタルサーチを導入する。

ADR 0086でMinibufferInputPrompterベースの実装を試行したが、以下の根本的な問題で却下された:

- i-searchはメインウィンドウにフォーカスを残す必要があるが、MinibufferInputPrompterはミニバッファにフォーカスを移す
- ローカルキーマップのdefaultCommandがマイナーモードより優先され、文字入力時にインクリメンタル検索が動作しない
- 非対象キーでの自然な終了ができない

本ADRではEmacsの `overriding-terminal-local-map` に相当する最上位キーマップの仕組みを導入し、これらの問題を解決する。

## 決定

### Overriding Keymap の導入

CommandLoopに最上位キーマップ（overriding keymap）の概念を導入する。

overriding keymapが設定されている場合、`processNormalKey()` はまずoverriding keymapでキーを解決する。バインドがあればそのコマンドを実行し、バインドがなければ終了コールバックを呼んでからoverriding keymapをクリアし、通常のキーマップ解決にフォールスルーする。

```
processNormalKey(key):
  if overridingKeymap != null:
    entry = overridingKeymap.lookup(key)
    if entry.isPresent():
      handleEntry(entry)
      return
    else:
      overridingKeymap.onUnboundKeyExit.run()
      overridingKeymap = null
      // fall through
  entry = resolveKey(key)
  ...
```

コマンドからoverriding keymapを操作するために `OverridingKeymapController` インターフェースをCommandContextに追加する。

### メインウィンドウフォーカスの維持

i-searchは `frame.activateMinibuffer()` を呼ばない。カーソルはメインウィンドウに残り、ミニバッファにはmessageBuffer経由で "I-search: query" をエコー表示する。

overriding keymapが有効な間は `processKey()` の先頭で行われる `clearShowingMessage()` をスキップし、エコー表示の消失を防ぐ。

### クエリの独自管理

ミニバッファのバッファへの挿入ではなく、ISearchSessionが内部的にクエリ文字列を保持する。文字入力・削除・表示すべてをISearchSessionが管理する。

### 非対象キーでの自然な終了

overriding keymapにバインドされていないキーが来た場合:
1. 終了コールバック（onUnboundKeyExit）が呼ばれ、i-searchが確定終了する（カーソルは現在位置に残る）
2. overriding keymapがクリアされる
3. 同じキーストロークが通常のキーマップ解決に回される

これにより、C-xなどのプレフィックスキーやカーソル移動キーでi-searchが自然に終了する。

### i-searchの実装構造

- `ISearchSession` — 検索状態管理（クエリ、方向、マッチ位置、ハイライト）
- `ISearchForwardCommand` / `ISearchBackwardCommand` — i-search起動コマンド（C-s / C-r）
- `ISearchSelfInsertCommand` — overriding keymapのdefaultCommand（クエリへの文字追加）
- `ISearchDeleteCharCommand` — DEL/Backspace（クエリ末尾削除）
- `ISearchConfirmCommand` — RET（検索確定）
- `ISearchCancelCommand` — C-g（キャンセル、元位置復帰）
- `BufferSearcher` — バッファ内テキスト検索エンジン

### 検索エンジン

`buffer.getText()` でバッファ全体を文字列化し、`String.indexOf` / `String.lastIndexOf` で検索する。
`String.indexOf` はchar単位のオフセットを返すため、BufferSearcher内でchar offset → codepoint offset の変換を行う。

### ラップアラウンド

バッファ末尾（先頭）到達後、先頭（末尾）から再検索するラップアラウンドを実装する。
ラップアラウンド発生時は "Wrapped I-search: query" をメッセージ表示する。

### ハイライト管理

検索マッチ位置にFace (`ISEARCH_MATCH`) を付与してハイライトする。
確定・キャンセル時にハイライトを除去するため、FaceName指定で特定のFaceのみを除去する `removeFaceByName` を導入する。

### 前回クエリでの再検索

i-search確定後、再度C-s/C-rを押すと空クエリの状態になる。
空クエリのままC-s/C-rを押すと前回のクエリで検索を再開する。
前回クエリはISearchSessionのstaticフィールドに保持する。

### キーバインド

- C-s → ISearchForwardCommand（i-search中はnext match）
- C-r → ISearchBackwardCommand（i-search中はprevious match）
- RET → ISearchConfirmCommand
- C-g → ISearchCancelCommand
- DEL/Backspace → ISearchDeleteCharCommand
- 印字可能文字 → ISearchSelfInsertCommand（defaultCommand）

## 変更されるADR

- ADR 0086: 本ADRで再設計を行った
