# ADR-0003: パッケージ命名規約

## ステータス

承認済み

## コンテキスト

Javaのパッケージ名を決定する必要がある。
モジュール構成とパッケージ名の対応関係を明確にしたい。

## 決定

- ルートパッケージ: `io.github.shomah4a.allei`
- 各モジュールごとにサブパッケージを作る
  - `allei-core` → `io.github.shomah4a.allei.core.*`
  - `libs/gap-buffer` → `io.github.shomah4a.allei.libs.gapbuffer`
- libsのモジュールもパッケージ名に`libs`を含める

## 帰結

- パッケージ名からモジュール所属が明確になる
- 逆ドメイン名の慣例に従いつつ、プロジェクト固有の構造を反映する
