# ADR-0052: InputPrompterのInputHistory必須化

## ステータス
承認済み

## コンテキスト
ADR-0043 では InputHistory をオプショナルとし、prompt メソッドのオーバーロードで history なしの呼び出しも許容していた。ADR-0051 で全コマンドにヒストリを適用した結果、history なしで prompt を呼ぶ箇所がなくなった。オプショナルのままでは今後のコマンド追加時にヒストリ渡し忘れが発生しうる。

## 決定

### InputPrompter の基本メソッド変更
- 旧: `prompt(String message)` (history なし)
- 新: `prompt(String message, InputHistory history)` (history 必須)

### 引数順の統一
全オーバーロードの引数順を `(message, [initialValue], history, [completer])` に統一した。

### オーバーロード構成
```java
// abstract
prompt(String message, InputHistory history)

// default
prompt(String message, InputHistory history, Completer completer)

// default
prompt(String message, String initialValue, InputHistory history, Completer completer)
```

### MinibufferInputPrompter の簡素化
history が必須となったことで以下の簡素化を行った:
- promptInternal, createMinibufferKeymap, MinibufferConfirmCommand の history パラメータから @Nullable を除去
- history の null チェックを除去
- ヒストリナビゲーションのキーバインド (M-p/M-n/Arrow Up/Arrow Down) は常に設定

## 結果
- 新しいコマンド追加時に InputHistory の渡し忘れがコンパイルエラーとして検出される
- MinibufferInputPrompter 内の条件分岐が減り、コードが簡素化された
- ADR-0043 の「history はオプショナル」という設計判断を覆す変更である
