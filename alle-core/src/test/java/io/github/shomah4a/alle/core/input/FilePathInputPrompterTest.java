package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FilePathInputPrompterTest {

    private static final Path HOME = Path.of("/home/user");

    private AtomicReference<InputHistory> capturedHistory;
    private CompletableFuture<PromptResult> innerFuture;
    private FilePathInputPrompter filePathInputPrompter;

    @BeforeEach
    void setUp() {
        capturedHistory = new AtomicReference<>();
        innerFuture = new CompletableFuture<>();

        // InputPrompterのスタブ: 渡されたhistoryを記録する
        InputPrompter stubPrompter = new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                capturedHistory.set(history);
                return innerFuture;
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message,
                    String initialValue,
                    InputHistory history,
                    Completer completer,
                    InputUpdateListener updateListener) {
                capturedHistory.set(history);
                return innerFuture;
            }
        };

        DirectoryLister emptyLister = directory -> Lists.immutable.empty();
        filePathInputPrompter = new FilePathInputPrompter(stubPrompter, emptyLister, HOME);
    }

    @Nested
    class ヒストリ連携 {

        @Test
        void 外部のヒストリがInputPrompterに渡される() {
            var externalHistory = new InputHistory();
            externalHistory.add("~/docs/a.txt");
            externalHistory.add("~/docs/b.txt");

            var unused = filePathInputPrompter.prompt("Find file: ", "/home/user/work", externalHistory);

            assertSame(externalHistory, capturedHistory.get());
        }

        @Test
        void 確定時にシャドウ除去済みの値が外部ヒストリに追加される() {
            var externalHistory = new InputHistory();

            var unused = filePathInputPrompter.prompt("Find file: ", "/home/user", externalHistory);
            innerFuture.complete(new PromptResult.Confirmed("~/docs/test.txt"));

            assertEquals(1, externalHistory.size());
            assertEquals("~/docs/test.txt", externalHistory.get(0));
        }

        @Test
        void キャンセル時に外部ヒストリに追加されない() {
            var externalHistory = new InputHistory();

            var unused = filePathInputPrompter.prompt("Find file: ", "/home/user", externalHistory);
            innerFuture.complete(new PromptResult.Cancelled());

            assertEquals(0, externalHistory.size());
        }
    }

    @Nested
    class パス変換 {

        @Test
        void 確定値はチルダが展開された絶対パスで返る() {
            var history = new InputHistory();

            var future = filePathInputPrompter.prompt("Find file: ", "/home/user", history);
            innerFuture.complete(new PromptResult.Confirmed("~/docs/test.txt"));

            var result = future.join();
            var confirmed = assertInstanceOf(PromptResult.Confirmed.class, result);
            assertEquals("/home/user/docs/test.txt", confirmed.value());
        }

        @Test
        void シャドウ付き入力はシャドウ除去後にチルダ展開される() {
            var history = new InputHistory();

            var future = filePathInputPrompter.prompt("Find file: ", "/home/user", history);
            // "/home/user/work//tmp/test.txt" → シャドウ境界は"/home/user/work/"の部分
            innerFuture.complete(new PromptResult.Confirmed("~/work//tmp/test.txt"));

            var result = future.join();
            var confirmed = assertInstanceOf(PromptResult.Confirmed.class, result);
            assertEquals("/tmp/test.txt", confirmed.value());
        }
    }

    @Nested
    class ignoreCase対応 {

        @Test
        void supplierがtrueを返すとShadowAwareCompleter経由でケース無視マッチが効く() {
            var capturedCompleter = new AtomicReference<Completer>();
            InputPrompter capturingPrompter = new InputPrompter() {
                @Override
                public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                    return innerFuture;
                }

                @Override
                public CompletableFuture<PromptResult> prompt(
                        String message,
                        String initialValue,
                        InputHistory history,
                        Completer completer,
                        InputUpdateListener updateListener) {
                    capturedCompleter.set(completer);
                    return innerFuture;
                }
            };
            DirectoryLister lister = directory -> Lists.immutable.of(
                    new DirectoryEntry.File(Path.of("/tmp/Src.txt"), FileAttributes.EMPTY),
                    new DirectoryEntry.File(Path.of("/tmp/bar.txt"), FileAttributes.EMPTY));
            var prompter = new FilePathInputPrompter(capturingPrompter, lister, HOME, () -> true);

            var unused = prompter.prompt("Find file: ", "/home/user", new InputHistory());

            var completer = capturedCompleter.get();
            assertNotNull(completer);
            // 入力 "/tmp/src" は "/tmp/Src.txt" にケース無視マッチする
            var candidates = completer.complete("/tmp/src");
            assertEquals(1, candidates.size());
            assertEquals("/tmp/Src.txt", candidates.get(0).value());
        }

        @Test
        void supplierがfalseを返すとケース敏感のままになる() {
            var capturedCompleter = new AtomicReference<Completer>();
            InputPrompter capturingPrompter = new InputPrompter() {
                @Override
                public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                    return innerFuture;
                }

                @Override
                public CompletableFuture<PromptResult> prompt(
                        String message,
                        String initialValue,
                        InputHistory history,
                        Completer completer,
                        InputUpdateListener updateListener) {
                    capturedCompleter.set(completer);
                    return innerFuture;
                }
            };
            DirectoryLister lister = directory -> Lists.immutable.of(
                    new DirectoryEntry.File(Path.of("/tmp/Src.txt"), FileAttributes.EMPTY));
            var prompter = new FilePathInputPrompter(capturingPrompter, lister, HOME, () -> false);

            var unused = prompter.prompt("Find file: ", "/home/user", new InputHistory());

            var completer = capturedCompleter.get();
            assertNotNull(completer);
            var candidates = completer.complete("/tmp/src");
            assertEquals(0, candidates.size());
        }
    }
}
