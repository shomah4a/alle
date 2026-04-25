## collectionの扱い

java.util のコレクションクラス類は使わずに Eclipse Collections を使うこと。
java.util のコレクションはmutabilityが型レベルでチェックできないので使用してはいけない。

Mutable Collectio はクラスのprivateフィールドやメソッドのローカル変数でのみ使うこと。
インターフェイスとしてmutable collectionを露出させることは基本的に許可されない。
返り値などでは Immutable なインターフェイスを返す。

例えばメソッド内での返り値の組み立てでは MutableList を使う。
返り値では ListIterable を使う。

toImmutable による変換はコピーのオーバーヘッドがあるので、getterなどで単に参照するだけであれば使用しないようにすること。
MutableList を返す場合は ListIterable を使うなど。
変換そのものが目的である場合はその限りではない。

インターフェイスや返り値に Mutable なコレクションが出てきた場合は設計を疑う必要があるので、ユーザーに確認すること。

## ロガーの扱い

`java.util.logging.Logger` を直接使用してはならない。
ログ出力には `io.github.shomah4a.alle.core.Loggable` インターフェースを `implements` し、`logger()` メソッドを使うこと。

```java
// NG: java.util.logging を直接使用
private static final Logger logger = Logger.getLogger(MyClass.class.getName());
logger.log(Level.WARNING, "message", e);

// OK: Loggable を implements して logger() を使う
public class MyClass implements Loggable {
    void doSomething() {
        logger().warn("message", e);
    }
}
```

SLF4J のログレベル対応:
- `Level.SEVERE` → `logger().error()`
- `Level.WARNING` → `logger().warn()`
- `Level.INFO` → `logger().info()`
- `Level.FINE` → `logger().debug()`
- `Level.FINER`, `Level.FINEST` → `logger().trace()`
