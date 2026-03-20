# ADR-0046: スクリプトからのコマンド定義・キーバインド設定・モード定義

## ステータス

提案中

## コンテキスト

ADR-0044 でスクリプトエンジン基盤を導入し、eval-expression で Python コードを実行できるようになった。
次のステップとして、Elisp のようにスクリプトからエディタの振る舞いを拡張できるようにする。

GraalPy では Python クラスから Java インターフェースを直接継承して実装できることが検証済みである。
これにより Java 側にアダプタクラスを用意する必要がなく、Python 側でインターフェースを
直接実装する方式を採用する。

## 決定

### Python 側で Java インターフェースを直接継承する

Command, MajorMode, MinorMode の各インターフェースを Python クラスで直接実装する。
`alle` パッケージに基底クラスを提供し、ボイラープレート（CompletableFuture のラップ等）を隠蔽する。

```python
import alle
from alle.command import AlleCommand

class MyCommand(AlleCommand):
    def name(self):
        return "my-command"

    def run(self):
        alle.message("hello")

alle.register_command(MyCommand())
```

Java 側にアダプタクラス（ScriptCommand 等）は作らない。

### EditorFacade に登録・実行用 API を追加する

- `registerCommand(Command)` — Command インスタンスをレジストリに登録
- `executeCommand(name)` — 名前でコマンドを実行
- `globalSetKey(keySpec, commandName)` — グローバルキーマップにキーバインドを追加

### キー表記は Emacs 風文字列で指定する

`"C-c C-c"`, `"M-x"`, `"C-x f"` のような文字列からキーストロークを解析する
KeySpecParser を `alle-core` の `keybind` パッケージに配置する。
スクリプトから KeyStroke オブジェクトを直接扱わせない。

### alle パッケージの構成

```
alle/
├── __init__.py      # モジュールレベルAPI（register_command, global_set_key 等）
├── futures.py       # JavaFuture ラッパー
├── window.py        # Window ラッパー
├── buffer.py        # Buffer ラッパー
├── command.py       # AlleCommand 基底クラス
└── mode.py          # AlleMajorMode / AlleMinorMode 基底クラス
```

### コマンド基底クラス (alle/command.py)

```python
import java
from java.util.concurrent import CompletableFuture

Command = java.type('io.github.shomah4a.alle.core.command.Command')

class AlleCommand(Command):
    """コマンドの基底クラス。
    サブクラスは name() と run() を実装する。
    run() は CompletableFuture のラップを自動化する。
    """
    def execute(self, ctx):
        try:
            self.run()
            return CompletableFuture.completedFuture(None)
        except Exception as e:
            return CompletableFuture.failedFuture(e)

    def run(self):
        raise NotImplementedError
```

### モード基底クラス (alle/mode.py)

```python
import java
from java.util import Optional

MajorMode = java.type('io.github.shomah4a.alle.core.mode.MajorMode')
MinorMode = java.type('io.github.shomah4a.alle.core.mode.MinorMode')

class AlleMajorMode(MajorMode):
    """メジャーモードの基底クラス。"""
    def keymap(self):
        return Optional.empty()

class AlleMinorMode(MinorMode):
    """マイナーモードの基底クラス。"""
    def keymap(self):
        return Optional.empty()
```

### CommandRegistry の上書き登録対応

現在の `CommandRegistry.register()` は同名コマンドで例外を投げる。
スクリプトからのコマンド再定義を可能にするため、上書き登録用メソッドを追加する。

## 影響

- `alle-core`: KeySpecParser 新規追加、CommandRegistry に上書き登録メソッド追加
- `alle-script`: EditorFacade にメソッド追加
- `alle` パッケージ: command.py, mode.py 新規追加、__init__.py に関数追加

## 設計上の注意点

### executeCommand の CommandContext

`executeCommand` で他のコマンドを呼ぶ場合、CommandContext の構築が必要。
EditorFacade に CommandContext 構築に必要な依存（InputPrompter 等）を注入する。

### プレフィックスキーの自動解決

`"C-x f"` のような複数キー表記の場合、既存のプレフィックスキーマップを検索し、
存在しなければ新規作成してバインドする。

### スレッド安全性

コマンド登録・キーバインド設定はエンジンスレッドから呼ばれる前提。
ScriptCommand の execute() も同じスレッドで GraalVM Context にアクセスするため安全。
