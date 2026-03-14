# Allei - 設計決定事項

## 概要

Emacs ライクな TUI テキストエディタを Java で実装する。

## 名前

- **Allei** (アッレイ)
- 由来: 古事記の編纂者・稗田阿礼(ひえだのあれ) + イタリア語で「できた」の意

## 基本方針

- TUI ファーストで開発し、入出力・レンダリング層を抽象化して将来的に GUI 対応可能にする
- Emacs の基本概念(キーバインド、バッファ、ウィンドウ、モード)を踏襲
- 拡張言語は KTS (Kotlin Script) を採用。将来的に GraalVM 経由で他言語も検討可能

## テキストモデル

- インターフェースで抽象化し、実装を差し替え可能にする
- 初期実装は Gap Buffer
- 将来的に Piece Table, Rope も選択肢

## モジュール構成

```
libs/
  text-model/          # 汎用テキストデータ構造(Gap Buffer等)

allei-core/            # バッファ、ウィンドウ等のドメインモデル
allei-command/         # コマンドシステム
allei-mode/            # メジャー/マイナーモード
allei-render-api/      # レンダリング抽象層(interface)
allei-tui/             # TUI実装(JLine3ベース)
allei-scripting/       # GraalVM拡張言語
allei-app/             # エントリポイント、組み立て
```

## パッケージ

- ルート: `io.github.shomah4a.allei`
- 各サブモジュールごとにサブパッケージを作る

## ビルドツール

- Gradle (Kotlin DSL / .gradle.kts)

## アーキテクチャ層

```
拡張言語 (GraalJS / KTS)
    |
コマンドシステム          (M-x で呼べる単位)
    |
モード (メジャー/マイナー)
    |
キーバインドエンジン      (キーマップのチェイン)
    |
バッファ / ウィンドウ管理
    |
テキストモデル (Gap Buffer等)
    |
レンダリング抽象層        (interface で切り離し)
    |
TUI実装 / (将来) GUI実装
```

## 初期スコープ

1. テキストモデル(Gap Buffer実装)
2. バッファ管理(ファイル読み込み・保存)
3. TUI レンダリング(JLine3ベース、抽象層あり)
4. 基本キーバインド(カーソル移動、文字入力、C-x C-s で保存、C-x C-c で終了)
5. ミニバッファ(M-x でコマンド入力)

## 最初の着手

- バッファのモデルと IO 周りの実装 + テスト
