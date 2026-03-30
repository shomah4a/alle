# ADR 0087: FaceNameのセマンティック定義整理

## ステータス

承認

## コンテキスト

FaceNameはテキスト範囲の構文上の役割を表すセマンティック名であり、具体的な見た目の情報は持たない。
FaceThemeがFaceNameをFaceSpecに解決することで視覚属性が決まる。

現状、FaceNameにモード固有の定数（DIRED_HEADER, DIRED_DIRECTORY, DIRED_MARKED）が定義されている。
これはセマンティック名の設計意図に反する。セマンティック定義はグローバルに一つであるべきで、各モードのスタイラーはその中から適切なものを選択する形が正しい。

モードの役割再考（コマンドの名前空間化等）に先立ち、FaceNameをセマンティック定義として整理する。

## 決定

### セマンティック定義の原則

- FaceNameはモード非依存の汎用的なセマンティック概念のみ定義する
- 各モードのスタイラーは定義済みのセマンティック名から選択して使用する
- モード固有のFaceNameは定義しない

### モード固有FaceNameの廃止と移行

| 旧定数 | 移行先 | 理由 |
|--------|--------|------|
| DIRED_HEADER | HEADING（既存） | ヘッダ行は見出しのセマンティクスに該当 |
| DIRED_DIRECTORY | DIRECTORY（新規） | ディレクトリ表示は汎用的なセマンティック概念 |
| DIRED_MARKED | MARKED（新規） | マーク済み表示は汎用的なUI概念 |
| MINIBUFFER_PROMPT | PROMPT（新規） | プロンプト表示はミニバッファ固有ではなくshell-mode等でも使える汎用概念 |

### 新規セマンティック名

- DIRECTORY: ディレクトリを表すテキスト
- FILE: ファイルを表すテキスト（DIRECTORYとの対称性のため）
- MARKED: マーク・選択済みのテキスト
- PROMPT: プロンプト文字列（旧MINIBUFFER_PROMPTの汎用化）

## 影響

- FaceName.java: DIRED_* / MINIBUFFER_PROMPT 定数を削除、DIRECTORY/FILE/MARKED/PROMPT を追加
- DefaultFaceTheme.java: マッピング差し替え、設計意図のコメント追加
- TreeDiredRenderer.java: 参照先の変更（3箇所）
- MinibufferInputPrompter.java: MINIBUFFER_PROMPT → PROMPT
