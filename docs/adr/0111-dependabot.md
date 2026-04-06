# ADR 0111: Dependabot による依存関係の自動更新

## ステータス

採用

## コンテキスト

CI（ADR 0110）の整備が完了し、ビルドとリントが自動実行される環境が整った。
依存ライブラリやGitHub Actionsのバージョンを継続的に最新に保つため、Dependabotを導入する。

## 決定

### 対象エコシステム

- `gradle`: プロジェクトの依存ライブラリ
- `github-actions`: CI で使用するアクション

### 更新スケジュール

- weekly（毎週）

### グルーピング戦略

- minor + patch の更新は1つのPRにまとめる
- major の更新はエコシステムごとに1つのPRにまとめる

### 自動マージ

- minor / patch の更新はCIが通ったら自動マージする
- major の更新は手動レビューとする
- 自動マージには `dependabot/fetch-metadata` で update-type を判定し、major を除外する
- ワークフローのトリガーは `pull_request_target` を使用する（Dependabotの secrets アクセスのため）

### 前提条件（リポジトリ設定）

以下の設定がリポジトリ側で必要:

- Settings > General > Allow auto-merge を有効化
- Branch protection rule で `build` と `lint` を required status checks に設定

これらが未設定の場合、CI結果を待たずにマージされるリスクがある。

## 影響

- 依存ライブラリのバージョンが自動的に最新に保たれる
- minor/patch は自動マージにより運用負荷が低減される
- major の破壊的変更は手動レビューにより安全性を確保する
