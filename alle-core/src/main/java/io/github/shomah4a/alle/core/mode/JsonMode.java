package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.indent.CStyleIndentCommands;
import io.github.shomah4a.alle.core.mode.indent.CStyleIndentConfig;
import io.github.shomah4a.alle.core.mode.indent.CStyleIndentState;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzer;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.LanguageSupport;
import java.util.Optional;
import org.eclipse.collections.api.factory.Sets;

/**
 * JSONモード。.json / .jsonl ファイルに適用される。
 * tree-sitter-json によるシンタックスハイライト、構文解析、
 * Cスタイルオートインデントを提供する。
 */
public class JsonMode implements MajorMode {

    private static final int INDENT_WIDTH = 2;

    private static final ModeSettings DEFAULTS = ModeSettings.builder()
            .set(EditorSettings.INDENT_WIDTH, INDENT_WIDTH)
            .build();

    private static final CStyleIndentConfig INDENT_CONFIG =
            new CStyleIndentConfig(INDENT_WIDTH, Sets.immutable.with('[', '{'), Sets.immutable.with(']', '}'));

    private final LanguageSupport languageSupport;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public JsonMode(LanguageSupport languageSupport) {
        this.languageSupport = languageSupport;
        var indentState = new CStyleIndentState(INDENT_CONFIG, languageSupport.analyzer());
        var commands = CStyleIndentCommands.create("json", indentState);
        this.keymap = createKeymap(commands);
        this.commandRegistry = createCommandRegistry(commands);
    }

    private static Keymap createKeymap(CStyleIndentCommands.Commands commands) {
        var km = new Keymap("json-mode");
        km.bind(KeyStroke.of('\n'), commands.newlineAndIndent());
        km.bind(KeyStroke.of('\t'), commands.indentLine());
        km.bind(KeyStroke.shift('\t'), commands.dedentLine());
        return km;
    }

    private static CommandRegistry createCommandRegistry(CStyleIndentCommands.Commands commands) {
        var registry = new CommandRegistry();
        registry.register(commands.indentLine());
        registry.register(commands.dedentLine());
        registry.register(commands.newlineAndIndent());
        return registry;
    }

    @Override
    public String name() {
        return "json";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    @Override
    public Optional<SyntaxStyler> styler() {
        return Optional.of(languageSupport.styler());
    }

    @Override
    public Optional<SyntaxAnalyzer> syntaxAnalyzer() {
        return Optional.of(languageSupport.analyzer());
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
