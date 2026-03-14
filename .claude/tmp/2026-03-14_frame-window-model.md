# Allei - Frame/Windowモデル実装計画

## 実装スコープ

Frame/Windowのモデルを実装する。レンダリングは含まない。

## 設計方針

- WindowはBufferに対するビュー。point、選択状態、スクロール位置を持つ
- BufferからpointとカーソルDependentなメソッドを外す
- WindowTreeはimmutable（sealed interface + record）
- Frameがmutableで、操作のたびにWindowTreeを作り直して差し替える
- Minibuffer WindowはFrame固定、分割対象外
- 最後のWindowは削除不可

## 実装ステップ

### 1. 開発ブランチ作成

### 2. ADR追加
- Frame/Windowモデルの設計決定をADRとして記録

### 3. Buffer からpointを外す
- point, setPoint, insert(), deleteBackward(), deleteForward(), insertAt(), deleteAt() を削除
- Bufferの責務: TextModel保持、ファイルパス、ダーティフラグ
- 既存テスト修正

### 4. Window の実装 + テスト
- パッケージ: io.github.shomah4a.allei.core.window
- Buffer参照、point、スクロール位置（表示開始行）
- カーソル位置での挿入・削除等のメソッド（旧Bufferから移動）

### 5. WindowTree の実装 + テスト
- sealed interface WindowTree
- Leaf(Window), Split(Direction, ratio, first, second)
- 分割操作: 対象Leafを見つけてSplitに置き換えた新ツリーを返す
- 削除操作: Splitの片方を消して縮退した新ツリーを返す

### 6. Frame の実装 + テスト
- WindowTree保持、Minibuffer Window保持
- アクティブWindow管理
- 分割・削除のAPI

### 7. テスト全体実行・フォーマット確認
