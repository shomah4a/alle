# ADR 0061: Pythonモードのスクリプト側実装

## ステータス

承認

## コンテキスト

モード登録API（ADR 0060）が実装され、スクリプト側からメジャーモードを定義・登録できるようになった。
コア機能の充足度を検証するベンチマークとして、Pythonモードをスクリプト側（Python/GraalPy）で実装する。

> スタイラーの実装方式について [ADR 0074](0074-parser-based-syntax-highlight.md) で更新あり。
> RegexStyler から パーサーベーススタイラーへの移行。モード自体のスクリプト側実装方針は維持。

## 決定

- Pythonモードは `alle/modes/python.py` に Python で実装する
- Java 側の `RegexStyler`, `StylingRule`, `Face` をPythonから直接利用する
- Java interop のラッパーを `alle/internal/styling.py` に配置し、宣言的なルール定義を可能にする
- 組み込みモードは `alle/modes/__init__.py` の `register_modes()` で一括登録し、`_init()` 完了後に呼び出す
- まずはシンタックスハイライトのみの最小実装とし、インデント・キー入力補助は後続タスクとする

## 根拠

- スクリプト側で実装することで、GraalPy-Java interop の実用性を検証できる
- `internal/styling.py` にラッパーを置くことで、今後の他モード実装でも再利用できる
- `alle/modes/` パッケージを `alle/mode.py`（基底クラス）と分離することで責務が明確になる
