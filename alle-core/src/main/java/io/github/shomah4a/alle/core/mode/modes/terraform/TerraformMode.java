package io.github.shomah4a.alle.core.mode.modes.terraform;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
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
 * Terraform / HCL モード。.tf / .tfvars / .hcl ファイルに適用される。
 * tree-sitter-hcl によるシンタックスハイライト、構文解析、
 * Cスタイルオートインデントを提供する（ADR 0136参照）。
 */
public class TerraformMode implements MajorMode {

    private static final int INDENT_WIDTH = 2;

    private static final ModeSettings DEFAULTS = ModeSettings.builder()
            .set(EditorSettings.INDENT_WIDTH, INDENT_WIDTH)
            .set(EditorSettings.COMMENT_STRING, "# ")
            .build();

    private static final CStyleIndentConfig INDENT_CONFIG = new CStyleIndentConfig(
            INDENT_WIDTH, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));

    private final LanguageSupport languageSupport;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public TerraformMode(LanguageSupport languageSupport) {
        this.languageSupport = languageSupport;
        var indentState = new CStyleIndentState(INDENT_CONFIG, languageSupport.analyzer());
        var commands = CStyleIndentCommands.create("terraform", indentState);
        this.keymap = createKeymap(commands);
        this.commandRegistry = createCommandRegistry(commands);
    }

    private static Keymap createKeymap(CStyleIndentCommands.Commands commands) {
        var km = new Keymap("terraform-mode");
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
        return "terraform";
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
