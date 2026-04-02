package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CoreEditingCommandsTest {

    private static final SettingsRegistry SETTINGS = createSettings();

    private Frame frame;
    private BufferFacade buffer;
    private BufferManager bufferManager;

    private static SettingsRegistry createSettings() {
        var registry = new SettingsRegistry();
        registry.register(EditorSettings.INDENT_WIDTH);
        registry.register(EditorSettings.COMMENT_STRING);
        return registry;
    }

    @BeforeEach
    void setUp() {
        buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
    }

    private CommandContext createContext() {
        return TestCommandContextFactory.create(frame, bufferManager);
    }

    @Nested
    class IndentDedentBackspace {

        @Test
        void 行頭の空白領域でインデント単位削除する() {
            buffer.insertText(0, "        x = 1");
            frame.getActiveWindow().setPoint(8);

            new IndentDedentBackspaceCommand().execute(createContext()).join();

            assertEquals("    x = 1", buffer.getText());
        }

        @Test
        void 行頭以外では1文字削除する() {
            buffer.insertText(0, "abc");
            frame.getActiveWindow().setPoint(3);

            new IndentDedentBackspaceCommand().execute(createContext()).join();

            assertEquals("ab", buffer.getText());
        }
    }

    @Nested
    class CommentDwim {

        @Test
        void コメントが挿入される() {
            buffer.insertText(0, "    x = 1");
            frame.getActiveWindow().setPoint(4);

            new CommentDwimCommand().execute(createContext()).join();

            assertEquals("    # x = 1", buffer.getText());
        }

        @Test
        void コメントが解除される() {
            buffer.insertText(0, "    # x = 1");
            frame.getActiveWindow().setPoint(6);

            new CommentDwimCommand().execute(createContext()).join();

            assertEquals("    x = 1", buffer.getText());
        }

        @Test
        void リージョンがアクティブなら選択範囲をコメントアウトする() {
            buffer.insertText(0, "x = 1\ny = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(11);

            new CommentDwimCommand().execute(createContext()).join();

            assertEquals("# x = 1\n# y = 2", buffer.getText());
        }

        @Test
        void リージョンが全行コメント済みならコメント解除する() {
            buffer.insertText(0, "# x = 1\n# y = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(15);

            new CommentDwimCommand().execute(createContext()).join();

            assertEquals("x = 1\ny = 2", buffer.getText());
        }
    }

    @Nested
    class IndentRegion {

        @Test
        void 選択範囲がインデントされる() {
            buffer.insertText(0, "x = 1\ny = 2\nz = 3");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(17);

            new IndentRegionCommand().execute(createContext()).join();

            assertEquals("    x = 1\n    y = 2\n    z = 3", buffer.getText());
        }
    }

    @Nested
    class DedentRegion {

        @Test
        void 選択範囲がデデントされる() {
            buffer.insertText(0, "    x = 1\n    y = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(19);

            new DedentRegionCommand().execute(createContext()).join();

            assertEquals("x = 1\ny = 2", buffer.getText());
        }
    }

    @Nested
    class CommentRegion {

        @Test
        void 選択範囲がコメントアウトされる() {
            buffer.insertText(0, "x = 1\ny = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(11);

            new CommentRegionCommand().execute(createContext()).join();

            assertEquals("# x = 1\n# y = 2", buffer.getText());
        }

        @Test
        void 既にコメント済みの行にもコメントが追加される() {
            buffer.insertText(0, "# x = 1\n# y = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(15);

            new CommentRegionCommand().execute(createContext()).join();

            assertEquals("# # x = 1\n# # y = 2", buffer.getText());
        }
    }

    @Nested
    class UncommentRegion {

        @Test
        void 選択範囲のコメントが解除される() {
            buffer.insertText(0, "# x = 1\n# y = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(15);

            new UncommentRegionCommand().execute(createContext()).join();

            assertEquals("x = 1\ny = 2", buffer.getText());
        }

        @Test
        void コメントされていない行には何もしない() {
            buffer.insertText(0, "x = 1\ny = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(11);

            new UncommentRegionCommand().execute(createContext()).join();

            assertEquals("x = 1\ny = 2", buffer.getText());
        }
    }

    @Nested
    class CommentOrUncommentRegion {

        @Test
        void 未コメント行がコメントアウトされる() {
            buffer.insertText(0, "x = 1\ny = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(11);

            new CommentOrUncommentRegionCommand().execute(createContext()).join();

            assertEquals("# x = 1\n# y = 2", buffer.getText());
        }

        @Test
        void 全行コメント済みなら解除される() {
            buffer.insertText(0, "# x = 1\n# y = 2");
            frame.getActiveWindow().setMark(0);
            frame.getActiveWindow().setPoint(15);

            new CommentOrUncommentRegionCommand().execute(createContext()).join();

            assertEquals("x = 1\ny = 2", buffer.getText());
        }
    }
}
