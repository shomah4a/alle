# ADR 0099: JavaScript / JSON モードの追加

## ステータス

承認

## コンテキスト

エディタで `.js`, `.json`, `.jsonl` ファイルを開いた際にシンタックスハイライトが効かない。
tree-sitter の Java バインディング (io.github.bonede) には JavaScript 用と JSON 用のパーサーが
存在するため、これらを利用してモードを追加する。

## 決定

### モード構成

- **JavaScriptMode**: `.js` ファイルに適用。tree-sitter-javascript を使用。
- **JsonMode**: `.json`, `.jsonl` ファイルに適用。tree-sitter-json を使用。

JavaScript と JSON は文法が異なるため、別々の tree-sitter パーサーを使い、
別々のモードとして実装する。

### tree-sitter バインディング

- `io.github.bonede:tree-sitter-javascript:0.25.0`
- `io.github.bonede:tree-sitter-json:0.24.8`

既存の tree-sitter-python と同じ提供元のバインディングを使用する。

### SyntaxAnalyzerRegistry の配置変更

従来 `Main.java` (alle-app) で `SyntaxAnalyzerRegistry.createWithBuiltins()` を呼び出し、
`EditorFacade` (alle-script) に渡していた。組み込みモードが tree-sitter を必要とするようになったため、
`EditorCore.create()` 内で registry を生成しフィールドに保持する。
`Main.java` は `core.syntaxAnalyzerRegistry()` 経由で取得する。

### DefaultCaptureMapping の拡張

JavaScript / JSON の highlights.scm で使用されるキャプチャ名のうち、
既存マッピングにないものを追加する。

- `variable.builtin` → `BUILTIN` (this, arguments 等)
- ~~`string.special.key` → `VARIABLE` (JSON のキー名)~~ → 削除済み。JSONキーの位置にある式が式自体の型（`@string` → `STRING`）でハイライトされるべきであるため、マッピングを削除しキャプチャをスキップさせる方針に変更した。

### Cスタイルインデント基盤

オートインデント・インデントサイクル・newline-and-indent を汎用的な
Cスタイルインデント基盤として Java で実装する。

- `CStyleIndentConfig`: 開き括弧・閉じ括弧文字をカスタマイズ可能な設定
- `CStyleIndentState`: インデントサイクルと newline-and-indent のロジック
- `CStyleIndentCommands`: モード名プレフィックス付きのコマンドを生成するファクトリ

Python mode の `PythonIndentState` (GraalPy) と同等の機能を Java で提供する。
括弧内のアライメントは tree-sitter の `enclosingBracket` を使用する。

キーバインド:
- `Enter` → `{mode}-newline-and-indent`
- `Tab` → `{mode}-indent-line`
- `Shift+Tab` → `{mode}-dedent-line`

### コメントコマンドの分離

従来 `comment-region` がトグル動作（コメント済みなら解除）をしていたが、
コメント専用に変更し、以下を新設した。

- `uncomment-region`: コメント解除専用
- `comment-or-uncomment-region`: トグル動作（旧 `comment-region` の振る舞い）

また `comment-dwim` をリージョン対応にし、リージョンがアクティブなら
`comment-or-uncomment-region` 相当の動作を行うようにした。

### TreeSitterAnalyzer のキャッシュ判定修正

Analyzer と Styler が同一 `TreeSitterSession` を共有する設計において、
Styler のパースでセッション内の旧 `TSTree` が `close()` された後、
Analyzer がテキスト比較でキャッシュヒットし、closed な `TSTree` を参照する
`cachedResult` を返す問題があった。

キャッシュ判定を `session.parse()` が返す `TSTree` のインスタンス比較に変更し、
セッション共有時のライフサイクル不整合を防ぐようにした。

### JavaScript の bracketTypes

`statement_block`, `class_body`, `switch_body` を追加した。
これらがないと `function() { ... }` 等のブロック内で
`enclosingBracket` が検出されず、括弧内インデントが効かない。

### インデントサイクル候補の改善

前行が開き括弧で終わる場合、`prevIndentLen + indentWidth` を候補に含めるようにした。
tree-sitter の bracket 検出が効かない場合のフォールバックとして機能する。

## 結果

- `.js`, `.json`, `.jsonl` ファイルでシンタックスハイライトが有効になる
- オートインデント・インデントサイクルが動作する
- JavaScript では `// ` によるコメント操作が動作する
- `SyntaxAnalyzerRegistry` が `EditorCore` に移動し、組み込みモードから利用可能になる
- Cスタイルインデント基盤は今後の言語（TypeScript, CSS 等）でも再利用可能
- コメントコマンドが用途別に分離され、`comment-dwim` がリージョン対応になる
- TreeSitterAnalyzer のセッション共有時のキャッシュ不整合が解消される
