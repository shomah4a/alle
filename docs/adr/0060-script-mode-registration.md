# ADR-0060: スクリプトからのモード登録API

## ステータス

承認

## コンテキスト

ADR-0046 でスクリプトからのコマンド定義・キーバインド設定の仕組みを導入した。
Python側にはモード基底クラス（`MajorModeBase` / `MinorModeBase`）と
Java側へのブリッジコード（`internal/mode.py`）が存在するが、
モードを登録するAPIが `EditorFacade` に存在せず、スクリプトからモードを登録できない。

モード登録を可能にすることで、ファイルタイプに応じたキーマップやハイライトの
カスタマイズがスクリプトだけで完結するようになる。

## 決定

### ModeRegistry の新設

`CommandRegistry` のパターンに倣い、名前→モードファクトリのレジストリを `alle-core` に新設する。

```java
public class ModeRegistry {
    void registerMajorMode(String name, Supplier<MajorMode> factory);
    void registerOrReplaceMajorMode(String name, Supplier<MajorMode> factory);
    Optional<Supplier<MajorMode>> lookupMajorMode(String name);
    ImmutableSet<String> registeredMajorModeNames();

    void registerMinorMode(String name, Supplier<MinorMode> factory);
    void registerOrReplaceMinorMode(String name, Supplier<MinorMode> factory);
    Optional<Supplier<MinorMode>> lookupMinorMode(String name);
    ImmutableSet<String> registeredMinorModeNames();
}
```

- スクリプトからの登録は `registerOrReplace` を使用（再評価時の上書きを許容）
- Java コードからの登録は `register` を使用（同名登録で例外）
- Eclipse Collections の `MutableMap` を使用

### AutoModeMap との統合方式

`AutoModeMap` 自体は変更しない。
`EditorFacade` が `ModeRegistry` と `AutoModeMap` の両方を保持し、
`registerAutoMode(extension, modeName)` メソッド内で仲介する。

```
registerAutoMode("py", "python-mode")
  → ModeRegistry.lookupMajorMode("python-mode") で Supplier 取得
  → AutoModeMap.register("py", supplier) に委譲
```

存在しないモード名が指定された場合は `IllegalArgumentException` で即座に失敗させる。

### EditorFacade へのAPI追加

```java
void registerMajorMode(Value modeFactory)   // Python ファクトリ → ModeRegistry 登録
void registerMinorMode(Value modeFactory)   // 同上
void registerAutoMode(String extension, String modeName)  // 拡張子マッピング
```

Python の `Value` から `Supplier<MajorMode>` への変換は、
`Value.execute()` で都度インスタンスを生成するラッパーで対応する。
GraalPy の型パラメータ消去問題を回避するため、
`value.as(Supplier.class)` ではなくラッパークラスを使用する。

### Python側API

```python
# メジャーモード登録（拡張子マッピングはオプション）
alle.register_major_mode(MyMode, extensions=["py", "pyw"])

# マイナーモード登録
alle.register_minor_mode(MyMinorMode)
```

`register_major_mode` は引数としてモードクラス（ファクトリとして機能）を受け取る。
`extensions` が指定された場合は `registerAutoMode` も自動呼び出しする。

## 影響

- `alle-core`: `ModeRegistry` 新規追加、`EditorCore` に統合
- `alle-script` (Java): `EditorFacade` のコンストラクタ引数追加（+2: ModeRegistry, AutoModeMap）
- `alle-script` (Python): `alle/__init__.py` に関数追加
- `alle-app`: `EditorFacade` 生成箇所の引数追加
