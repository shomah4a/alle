# ADR 0071: TextPropertyStoreにface属性を追加する

## ステータス
Accepted

## コンテキスト

> **注**: face属性の型がFaceからFaceNameに変更された。詳細は [ADR-0073](0073-face-semantic-styling.md) を参照。

ADR 0070でpointGuard属性をTextPropertyStoreに追加したが、プロンプト部分のハイライト表示は未実装。
現在のスタイリングはSyntaxStyler（メジャーモード単位）のみであり、特定テキスト範囲にピンポイントでスタイルを設定する手段がない。

## 決定

### 1. TextPropertyStoreにface属性を追加する
- FaceRangeListとして新規クラスを作成する（既存のRangeListには手を加えない）
- 各範囲にFaceデータを持たせる: FaceRange(start, end, face)
- APIはreadOnly/pointGuardと同じパターン: putFace / removeFace / getFaceSpans
- テキスト挿入・削除時の範囲自動追従を実装する

### 2. 優先度ルール: 効果範囲の狭いものほど優先度が高い
- SyntaxStyler（メジャーモード単位、バッファ全体に適用）よりテキストプロパティface（特定範囲にピンポイント設定）が優先される
- 描画時にSyntaxStylerのスパンとテキストプロパティのスパンをマージし、重複箇所ではテキストプロパティが上書きする

### 3. ミニバッファを含む全バッファでface描画を有効にする
- face属性はTextPropertyStoreの汎用機能であり、描画をミニバッファに限定しない
- RenderSnapshotFactoryで通常ウィンドウ・ミニバッファ共にテキストプロパティfaceを反映する

## 理由

### 汎用的なスタイル設定の必要性
- ミニバッファプロンプトのハイライトが最初のユースケース
- 将来的にeshellのプロンプト、検索結果のハイライト等にも利用可能

### 優先度ルールの根拠
- 効果範囲の狭いものほど意図的・明示的な設定であるため、広範なスタイルより優先すべき
- Emacsのtext-property > font-lockの優先順位と概ね一致する

### FaceRangeListの分離
- RangeListは値なし（ブール的）の範囲管理であり、face付き範囲管理とは構造が異なる
- 既存のRangeListに手を加えず新規クラスとすることで、既存機能への影響をゼロにする
