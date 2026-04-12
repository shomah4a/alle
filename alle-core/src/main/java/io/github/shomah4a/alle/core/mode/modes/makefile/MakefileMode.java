package io.github.shomah4a.alle.core.mode.modes.makefile;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import java.util.Optional;

/**
 * Makefileモード。Makefile, GNUmakefile, *.mk ファイルに適用される。
 * RegexStylerベースのシンタックスハイライト、タブ文字ベースの
 * オートインデント・サイクルインデントを提供する。
 */
public class MakefileMode implements MajorMode {

    private static final ModeSettings DEFAULTS = ModeSettings.builder()
            .set(EditorSettings.COMMENT_STRING, "# ")
            .set(EditorSettings.TAB_WIDTH, 8)
            .set(EditorSettings.INDENT_TABS_MODE, true)
            .build();

    private final MakefileStyler styler;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public MakefileMode() {
        this.styler = new MakefileStyler();
        var indentState = new MakefileIndentState();
        var commands = MakefileIndentCommands.create(indentState);
        this.keymap = createKeymap(commands);
        this.commandRegistry = createCommandRegistry(commands);
    }

    private static Keymap createKeymap(MakefileIndentCommands.Commands commands) {
        var km = new Keymap("makefile-mode");
        km.bind(KeyStroke.of('\n'), commands.newlineAndIndent());
        km.bind(KeyStroke.of('\t'), commands.indentLine());
        km.bind(KeyStroke.shift('\t'), commands.dedentLine());
        return km;
    }

    private static CommandRegistry createCommandRegistry(MakefileIndentCommands.Commands commands) {
        var registry = new CommandRegistry();
        registry.register(commands.indentLine());
        registry.register(commands.dedentLine());
        registry.register(commands.newlineAndIndent());
        return registry;
    }

    @Override
    public String name() {
        return "makefile";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    @Override
    public Optional<SyntaxStyler> styler() {
        return Optional.of(styler);
    }

    @Override
    public ModeSettings settingDefaults() {
        return DEFAULTS;
    }

    @Override
    public Optional<CommandRegistry> commandRegistry() {
        return Optional.of(commandRegistry);
    }
}
