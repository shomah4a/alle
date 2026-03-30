# ADR 0091: モード初期化ロジックのEditorCoreからの分離

## ステータス

承認

## コンテキスト

EditorCore.createCommandRegistry() がグローバルコマンド登録に加え、TreeDired・Occurなどモード固有のコマンド群・キーマップ・CommandResolver登録を一手に担っており、約150行に肥大化している。モードが増えるたびにEditorCoreが膨張し、モード固有のimportも集中する。

## 決定

各モードの初期化ロジックを、そのモードが属するパッケージ側のstaticメソッドに移動する。

- TreeDired: diredパッケージに初期化クラスを追加
- Occur: occurパッケージに初期化クラスを追加
- 各staticメソッドはグローバルレジストリへ登録するコマンド（TreeDiredCommand / OccurCommand）を返す
- commandResolver.registerModeCommands() もstaticメソッド内で完結させる
- createTreeDiredKeymap() / createOccurKeymap() もモード側に移動する

インターフェイスやインスタンス生成は行わず、staticメソッドによる分割とする。

## 対象外

TextMode/MarkdownModeの登録はcreate()メソッド内にあり、createCommandRegistry()の肥大化とは無関係のため今回はスコープ外とする。

## 結果

- EditorCore.createCommandRegistry() はグローバルコマンド登録と各モード初期化メソッドの呼び出しのみになる
- モード固有のimportがEditorCoreから除去される
- 新モード追加時はモードパッケージ側に初期化クラスを作り、EditorCoreでは1行呼ぶだけで済む
