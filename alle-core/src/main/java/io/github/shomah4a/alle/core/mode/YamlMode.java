package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.indent.YamlIndentCommands;
import io.github.shomah4a.alle.core.mode.indent.YamlIndentState;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzer;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.LanguageSupport;
import java.util.Optional;

/**
 * YAMLモード。.yml / .yaml ファイルに適用される。
 * tree-sitter-yaml によるシンタックスハイライト、構文解析、
 * ASTベースのオートインデント、コメント機能を提供する。
 */
public class YamlMode implements MajorMode {

    private static final int INDENT_WIDTH = 2;

    private static final ModeSettings DEFAULTS = ModeSettings.builder()
            .set(EditorSettings.INDENT_WIDTH, INDENT_WIDTH)
            .set(EditorSettings.COMMENT_STRING, "# ")
            .build();

    private final LanguageSupport languageSupport;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public YamlMode(LanguageSupport languageSupport) {
        this.languageSupport = languageSupport;
        var indentState = new YamlIndentState(INDENT_WIDTH, languageSupport.analyzer());
        var commands = YamlIndentCommands.create(indentState);
        this.keymap = createKeymap(commands);
        this.commandRegistry = createCommandRegistry(commands);
    }

    private static Keymap createKeymap(YamlIndentCommands.Commands commands) {
        var km = new Keymap("yaml-mode");
        km.bind(KeyStroke.of('\n'), commands.newlineAndIndent());
        km.bind(KeyStroke.of('\t'), commands.indentLine());
        km.bind(KeyStroke.shift('\t'), commands.dedentLine());
        return km;
    }

    private static CommandRegistry createCommandRegistry(YamlIndentCommands.Commands commands) {
        var registry = new CommandRegistry();
        registry.register(commands.indentLine());
        registry.register(commands.dedentLine());
        registry.register(commands.newlineAndIndent());
        return registry;
    }

    @Override
    public String name() {
        return "yaml";
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
