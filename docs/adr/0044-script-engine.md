# ADR-0044: スクリプトエンジン基盤の導入

## ステータス

提案中

## コンテキスト

Emacs の Elisp のように、ユーザーがスクリプトでエディタを拡張できる仕組みが必要である。
JVM 上で動作するスクリプトエンジンを検討した結果、以下の結論に至った。

- Kotlin Script: Alpha 段階、JetBrains が戦略的縮小中。不採用
- GraalJS + TypeScript: 型チェックは tsc で可能だが、Java ↔ JS 間の型情報が断絶する。.d.ts の整備コストが高い。不採用
- JShell: プロセス内統合 API がなく、static フィールド経由のハックが必要。不採用
- GraalPy (GraalVM Python): 強い型付け、`from java.util import ArrayList` 構文で Java クラスを直接利用可能、Polyglot API によるプロセス内統合が設計レベルでサポートされている。Oracle が活発にメンテナンス中

## 決定

### スクリプト実行基盤として GraalVM Polyglot API を採用する

初期の言語実装として GraalPy（Python 3.12 互換）を提供する。

### 言語エンジンをプラガブルにする

GraalVM Polyglot API は多言語対応であるため、薄い抽象層を介して将来的に他の言語（JavaScript, Ruby 等）にも対応可能な設計とする。

### スクリプト API はファサードクラス経由で提供する

Java の内部クラスを直接公開せず、スクリプト用のファサードオブジェクトを Context にバインドする。
これにより、内部リファクタリングがスクリプト API に影響しないようにする。

### スクリプト実行はエンジンスレッドで行う

既存の 3 スレッド構成（入力・エンジン・描画）において、スクリプト実行はエンジンスレッドで行う。
GraalPy の GIL 制約と Context のスレッド安全性制約の両方を、この設計で自然に満たす。

## 影響

- 新規モジュール `alle-script` の追加
- GraalVM SDK および GraalPy の依存追加
- EditorCore にスクリプトエンジン初期化の追加
- `eval-expression` コマンドの追加（M-: 相当）

## 今後の検討事項

- スクリプトファイルの自動読み込み（init.py 等）
- パッケージ管理（pip 統合 or 独自）
- 複数 Context による並列実行
- サンドボックス（allowIO, allowAllAccess の制御）
