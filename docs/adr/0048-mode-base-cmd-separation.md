# ADR-0048: ModeのCommandBase/_Cmdパターン適用

## ステータス

承認済み

## コンテキスト

GraalPyではJavaインターフェースを継承したPythonクラスに対して、`__init__`引数の受け渡しやインスタンスへの属性追加ができない制約がある。

コマンド定義ではこの制約に対応するため、ADR-0047で以下の分離パターンを導入済みである。

- `CommandBase`: 純粋Python ABCとしてユーザーが継承する基底クラス
- `_Cmd`: Javaの`Command`インターフェースを実装する内部クラス。クロージャで`CommandBase`の値をキャプチャして生成する

モード定義（`AlleMajorMode`, `AlleMinorMode`）は現在JavaのMajorMode/MinorModeインターフェースを直接継承しており、同じ制約を受ける構造になっている。

## 決定

### 1. mode.pyの純粋Python ABC化

`AlleMajorMode`/`AlleMinorMode`を`MajorModeBase`/`MinorModeBase`にリネームし、Java依存を除去する。

- `name()`: 抽象メソッド
- `keymap()`: `None`を返すデフォルト実装
- `highlighter()`: `None`を返すデフォルト実装（MajorModeBaseのみ）

### 2. internal/mode.pyの新規作成

`make_major_mode()`/`make_minor_mode()`関数を提供し、クロージャで`MajorModeBase`/`MinorModeBase`の値をキャプチャしたJavaインターフェース実装を生成する。

- Python側の`None`は`Optional.empty()`に変換
- 値がある場合は`Optional.of()`でラップ

### 3. internal/facade.pyへのwrap関数追加

`_wrap_major_mode()`/`_wrap_minor_mode()`を追加する。モード登録APIは別タスクで実装する。

### 4. keybindは対象外

`keybind.py`の`ctrl()`/`meta()`/`key()`はファクトリ関数であり、Javaクラスの継承を伴わないためGraalPy制約の影響を受けない。今回のスコープには含めない。

## 結果

- モード定義がコマンド定義と同じ分離パターンに統一される
- ユーザー向けAPIからJava依存が除去され、純粋Pythonとして扱える
- ADR-0047で定義された公開API `alle.mode: AlleMajorMode, AlleMinorMode` は `MajorModeBase, MinorModeBase` に変更される
