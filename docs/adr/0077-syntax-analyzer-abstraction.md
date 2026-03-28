# ADR 0077: 構文解析器インターフェイスの抽象化

## ステータス

承認

## コンテキスト

TreeSitterによる構文解析はハイライト（SyntaxStyler/TreeSitterStyler）でのみ利用されている。
構文解析結果はインデント深さの検知や入力補助など、ハイライト以外でも有用であるが、
現在のTreeSitterStylerはスタイリングに特化しており、構文木への汎用アクセス手段がない。

TreeSitterに極度に依存した設計にしたくないため、パーサー非依存の構文解析インターフェイスを定義し、
TreeSitterはその一実装として位置づける。

### 関連ADR

- ADR 0074: パーサーベースシンタックスハイライトの導入

## 決定

### パッケージ

`io.github.shomah4a.alle.core.syntax` に新規配置する。
既存の `styling` パッケージはスタイリング専用のまま維持する。

### SyntaxNode record

構文木のノードを表す値型。

```java
public record SyntaxNode(
    String type,           // パーサー実装固有のノードタイプ名
    int startLine,
    int startColumn,
    int endLine,
    int endColumn,
    ListIterable<SyntaxNode> children
) {}
```

`type` はパーサー実装に依存する値（TreeSitterの場合はノードタイプ名）である。
利用側はこの値がパーサー実装固有であることを前提とする。

### SyntaxTree インターフェイス

構文解析結果を表す。パーサー非依存。

```java
public interface SyntaxTree {
    Optional<SyntaxNode> nodeAt(int line, int column);
    Optional<SyntaxNode> enclosingNodeOfType(int line, int column, String nodeType);
    Optional<SyntaxNode> enclosingBracket(int line, int column);
    SyntaxNode rootNode();
}
```

- `enclosingBracket` は `(`, `[`, `{` に対応するノードを探索する便利メソッド
- 言語固有の構文判定（コロン終了でインデント等）はモード側が `nodeAt` 等を使って行う

### SyntaxAnalyzer インターフェイス

```java
public interface SyntaxAnalyzer {
    SyntaxTree analyze(ListIterable<String> lines);
}
```

- 呼び出し側がドキュメントの行リストを渡し、構文木を取得する
- 実装側はキャッシュやインクリメンタルパースを内部で管理する

### TreeSitter実装

- `TreeSitterSyntaxTree`: TSTreeをラップしてSyntaxTreeを実装。括弧系ノードタイプの集合をコンストラクタで受け取る
- `TreeSitterAnalyzer`: SyntaxAnalyzerの実装。TSParser/TSTreeのライフサイクルを管理。括弧系ノードタイプの集合をコンストラクタで受け取り、TreeSitterSyntaxTreeに渡す

`enclosingBracket` で検索するノードタイプは言語ごとに異なる（Pythonでは `argument_list`, `list`, `dictionary` 等）。
この定義はSyntaxAnalyzerRegistryの言語登録時に指定する。

TreeSitterStylerとのパース共有は行わず、独立して管理する。
統合は後続タスクとする。

### SyntaxAnalyzerRegistry

言語名からSyntaxAnalyzerを生成するレジストリ。
ParserStylerRegistryと同構造。言語ごとの括弧系ノードタイプ定義もここで管理する。

### MajorMode インターフェイスの拡張

```java
default Optional<SyntaxAnalyzer> syntaxAnalyzer() {
    return Optional.empty();
}
```

defaultメソッド追加のため既存実装への影響はない。

### スクリプトAPI

- `mode.py`: MajorModeBaseに `syntax_analyzer()` メソッド追加（default: None）
- `internal/mode.py`: make_major_modeに `syntaxAnalyzer()` ブリッジ追加
- `internal/syntax.py`: SyntaxAnalyzerのPythonラッパー
- `EditorFacade`: SyntaxAnalyzer生成メソッド追加

### CommandBase へのコンテキスト渡し

`CommandBase.run()` に `ScriptCommandContext` を引数として渡すよう変更した。
これはCommandContextのスクリプト向けラッパーで、`activeWindow()` や `message()` を提供する。

- `command.py`: `run(self)` → `run(self, ctx)` に変更
- `internal/command.py`: `make_command` 内でJava CommandContextをScriptCommandContextにラップしてから渡す
- `ScriptCommandContext.java`: スクリプト向けコマンドコンテキスト

破壊的変更だが、現時点で外部ユーザーはいない。

### Python modeのインデント状態管理

グローバル変数によるインデントサイクル状態管理を廃止し、`PythonIndentState` クラスに集約した。
PythonModeのインスタンスメンバとして保持される。

コマンドは `create_indent_commands(state)` ファクトリで生成し、stateをクロージャでキャプチャする。
コマンド実行時はctx経由でウィンドウ・バッファを取得する。

### ベンチマーク: Python modeインデント改善

SyntaxTreeを利用してカッコ内にいるかを判定し、カッコの開始位置に基づいたインデントを提供する。
SyntaxAnalyzerが利用不可の場合は既存の正規表現ロジックにフォールバックする。

インデントの決定ロジック：
- 開きカッコ直後で改行 → 行インデント + INDENT_UNIT（不完全構文では正規表現フォールバック）
- カッコ内でコンテンツの後に改行 → コンテンツのカラム位置に揃える
- コロン終了 → indent + INDENT_UNIT
- dedentキーワード終了 → indent - INDENT_UNIT

### コマンド実行のundoトランザクション

CommandLoopでコマンド実行を一律withTransactionで囲み、1コマンド=1undo単位とした。
トランザクション内で変更がなければundoスタックに積まない（既存のwithTransaction仕様）。
各コマンド内の個別withTransaction呼び出しは削除した。

## 根拠

- 構文解析とスタイリングは異なる関心事であり、独立したインターフェイスとして分離すべき
- パーサー非依存のインターフェイスにより、将来のパーサー差し替えや別実装の追加が容易になる
- TreeSitterStylerとの統合を急がないことで、段階的に設計を検証できる
