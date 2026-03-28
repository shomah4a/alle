# ADR 0073: Faceのセマンティック/修飾分離とテーマシステムの導入

## ステータス

提案

## コンテキスト

現在のFace（ADR 0028で導入）は、セマンティックな意味（KEYWORD, COMMENT等）と修飾属性（BOLD_FACE, ITALIC_FACE等）が同じ型・同じ定数として混在している。

問題点:
- `Face.BOLD_FACE` は「太字にしたいだけ」だが `foreground="default"` という具体的な色情報を持つ。重ね掛け時に外側の色を上書きしてしまう
- スタイラーが見た目の情報（色、装飾）を直接返しており、テーマの差し替えができない
- HTMLで `<font color="blue"><b>` と書いているのと同じ状態。意味と表現が分離されていない

ADR 0071で「効果範囲の狭いものほど優先」というルールを導入済みだが、合成時に色の「未指定」を表現できないため正しく動作しない。

> **注**: ADR 0028のFace設計、ADR 0071のテキストプロパティface属性はこのADRで置き換える。

## 決定

### 1. 概念の分離: FaceName / FaceSpec / FaceTheme

CSSの class / style宣言 / stylesheet の関係に対応する3つの概念を導入する。カスケーディング（CSS の C）は導入しない。

#### FaceName（セマンティック名）

テキスト範囲の構文上の役割を表す値オブジェクト。見た目の情報を一切持たない。
nameとdescriptionを持ち、equalsとhashCodeはnameのみで判定する。

enumではなくclassとすることで、スクリプトやプラグインから新しいセマンティック名を自由に追加できる。
組み込みの名前は定数として提供する。

```java
public final class FaceName {
    private final String name;
    private final String description;

    public FaceName(String name, String description) { ... }
    // equals/hashCode は name のみで判定

    public static final FaceName DEFAULT =
        new FaceName("default", "テキストのデフォルトスタイル");
    public static final FaceName HEADING =
        new FaceName("heading", "見出し");
    public static final FaceName CODE =
        new FaceName("code", "コードブロック・インラインコード");
    public static final FaceName LINK =
        new FaceName("link", "ハイパーリンク・参照リンク");
    public static final FaceName STRING =
        new FaceName("string", "文字列リテラル");
    public static final FaceName COMMENT =
        new FaceName("comment", "コメント");
    public static final FaceName KEYWORD =
        new FaceName("keyword", "プログラミング言語のキーワード");
    public static final FaceName TABLE =
        new FaceName("table", "テーブル要素");
    public static final FaceName LIST_MARKER =
        new FaceName("list-marker", "リストマーカー");
    public static final FaceName STRONG =
        new FaceName("strong", "強調テキスト（Markdownの**太字**等）");
    public static final FaceName EMPHASIS =
        new FaceName("emphasis", "強勢テキスト（Markdownの*斜体*等）");
    public static final FaceName DELETION =
        new FaceName("deletion", "削除テキスト（Markdownの~~取り消し線~~等）");
    public static final FaceName MINIBUFFER_PROMPT =
        new FaceName("minibuffer-prompt", "ミニバッファのプロンプト文字列");
}
```

- `STRONG` は「強調」という意味であり、太字にするかはテーマの責務
- `EMPHASIS` は「強勢」、`DELETION` は「削除」
- スクリプトから `new FaceName("decorator", "Pythonデコレータ")` のように拡張可能

#### FaceSpec（視覚属性の部分定義）

具体的な見た目を表す値オブジェクト。各属性はnull許容で「未指定（下位レイヤに委ねる）」を表現できる。

```java
public record FaceSpec(
    @Nullable String foreground,
    @Nullable String background,
    ImmutableSet<FaceAttribute> attributes
)
```

- foreground/background が null = 「この属性は指定しない。合成時に下位の値を維持する」
- attributes が空 = 「装飾属性を追加しない」

#### FaceTheme（テーマ）

FaceName → FaceSpec のマッピング。テーマを差し替えることで同じセマンティクスに異なる見た目を割り当てられる。

```java
public interface FaceTheme {
    FaceSpec resolve(FaceName name);
}
```

### 2. 合成ルール: 単純な後勝ち

カスケーディングや優先度の概念は導入しない。影響範囲のスコープが小さいものが後に適用され、単純に後勝ちする。

```
合成(base, overlay):
    fg   = overlay.fg   != null ? overlay.fg   : base.fg
    bg   = overlay.bg   != null ? overlay.bg   : base.bg
    attrs = base.attrs ∪ overlay.attrs
```

- foreground/background: 後のレイヤがnon-nullなら上書き、nullなら下位を維持
- attributes: 和集合（外側のBOLDが内側のSTRONGで消えるのは直感に反するため）

### 3. スタイラーの責務変更

スタイラーはFaceNameのみを返す。見た目の情報を一切知らない。

変更前:
```java
// スタイラーがFace（色+属性）を直接返す
new StylingRule.PatternMatch(pattern, Face.BOLD_FACE)
```

変更後:
```java
// スタイラーがFaceName（意味）だけを返す
new StylingRule.PatternMatch(pattern, FaceName.STRONG)
```

### 4. 処理の流れ

```
テキスト
  → スタイラーが FaceName の範囲リストを返す（core層）
  → ScreenRenderer 内部で:
    → FaceTheme が各 FaceName を FaceSpec に解決
    → 同一位置に複数の FaceSpec があれば後勝ちで合成
    → FaceResolver が FaceSpec の色名を具体的なターミナル色に解決
```

外部インターフェースに露出するのは FaceName のみ。FaceSpec は描画の内部実装詳細である。

### 5. カスタマイズポイント

FaceTheme と FaceResolver を ScreenRenderer に外部から注入する。

```java
public ScreenRenderer(Screen screen, FaceTheme faceTheme, FaceResolver faceResolver)
```

- **FaceTheme の差し替え**: カラースキームの変更（ユーザーのテーマカスタマイズ）
- **FaceResolver の差し替え**: ターミナル環境ごとの色解決の変更

両者は独立にカスタマイズ可能。テーマの永続化・ユーザー設定UIはこのADRのスコープ外だが、注入ポイントを用意しておくことで将来の拡張を阻害しない。

### 6. デフォルトテーマ

現在のFace定数に相当するデフォルトマッピング:

| FaceName | foreground | background | attributes |
|---|---|---|---|
| DEFAULT | "default" | "default" | {} |
| HEADING | "yellow" | null | {BOLD} |
| CODE | "green" | null | {} |
| LINK | "cyan" | null | {UNDERLINE} |
| LIST_MARKER | "magenta" | null | {} |
| COMMENT | "black_bright" | null | {} |
| KEYWORD | "blue" | null | {BOLD} |
| STRING | "green" | null | {} |
| TABLE | "cyan" | null | {} |
| NUMBER | "cyan" | null | {} |
| ANNOTATION | "magenta" | null | {} |
| STRONG | null | null | {BOLD} |
| EMPHASIS | null | null | {ITALIC} |
| DELETION | null | null | {STRIKETHROUGH} |
| MINIBUFFER_PROMPT | "cyan" | null | {BOLD} |

STRONG/EMPHASIS/DELETIONはforeground/backgroundがnullであり、外側のセマンティクスの色を維持する。

## 理由

### セマンティクスと見た目の分離
- スタイラーは「これは何であるか」だけを記述し、「どう見えるか」はテーマに委ねる
- テーマの差し替えでカラースキーム変更が可能になる
- ターミナルの能力に応じた見た目の調整（BOLDが使えない環境でSTRONGを色変えにする等）が可能

### 単純な後勝ち
- CSSのカスケーディングとspecificityは人間にとって認知負荷が高い
- 「影響範囲の小さいものが勝つ」は直感的で予測可能
- ADR 0071の「効果範囲の狭いものほど優先」ルールと一致する

### FaceSpecのnull許容
- 「色を指定しない」を型で表現できる
- 合成時に「下位の色を維持する」が自然に実現できる
- 現在のFaceの `foreground="default"` が「デフォルト色を使う」と「指定しない」を区別できない問題を解決する

## 帰結

- 既存のFace型、StylingRule、スタイラー、FaceResolver、テキストプロパティfaceに変更が必要
- FaceNameの追加は今後のモード拡張時に必要になる（Pythonモード等）
- テーマの永続化・ユーザー設定はこのADRのスコープ外とする
