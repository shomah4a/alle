package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Python側でCompletableFutureをラップし、awaitプロトコルを実装する検証。
 */
class PythonAwaitProtocolTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder("python").allowAllAccess(true).build();
        // Python側にJavaFutureラッパークラスを定義
        context.eval("python", JAVA_FUTURE_CLASS);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static final String JAVA_FUTURE_CLASS = """
            import asyncio

            class JavaFuture:
                def __init__(self, java_future):
                    self._future = java_future

                def result(self):
                    return self._future.join()

                def done(self):
                    return self._future.isDone()

                def cancelled(self):
                    return self._future.isCancelled()

                def add_done_callback(self, fn):
                    def callback(result):
                        fn(self)
                    self._future.thenAccept(callback)

                def exception(self):
                    if not self.done():
                        raise asyncio.InvalidStateError('Result is not set.')
                    if self._future.isCompletedExceptionally():
                        try:
                            self._future.join()
                        except Exception as e:
                            return e
                    return None

                def __await__(self):
                    if not self.done():
                        yield self
                    return self.result()

                def __repr__(self):
                    if self.done():
                        if self._future.isCompletedExceptionally():
                            return '<JavaFuture failed>'
                        return f'<JavaFuture done: {self._future.join()}>'
                    return '<JavaFuture pending>'
            """;

    @Test
    void 完了済みFutureをawaitで値を取得できる() {
        var future = CompletableFuture.completedFuture(42);
        context.getBindings("python").putMember("jf", future);

        context.eval("python", "import asyncio");
        context.eval("python", """
                async def test():
                    f = JavaFuture(jf)
                    return await f
                """);
        var result = context.eval("python", "asyncio.run(test())");
        assertEquals(42, result.asInt());
    }

    @Test
    void 同期的にresultで取得できる() {
        var future = CompletableFuture.completedFuture("hello");
        context.getBindings("python").putMember("jf", future);

        var result = context.eval("python", "JavaFuture(jf).result()");
        assertEquals("hello", result.asString());
    }

    @Test
    void doneで完了状態を確認できる() {
        var future = CompletableFuture.completedFuture(1);
        context.getBindings("python").putMember("jf", future);

        var result = context.eval("python", "JavaFuture(jf).done()");
        assertTrue(result.asBoolean());
    }

    @Test
    void add_done_callbackでコールバックが呼ばれる() {
        var future = CompletableFuture.completedFuture(10);
        context.getBindings("python").putMember("jf", future);

        context.eval("python", """
                results = []
                f = JavaFuture(jf)
                f.add_done_callback(lambda fut: results.append(fut.result()))
                """);
        // thenAccept は非同期なので少し待つ
        context.eval("python", "import time; time.sleep(0.1)");
        var result = context.eval("python", "results[0]");
        assertEquals(10, result.asInt());
    }
}
