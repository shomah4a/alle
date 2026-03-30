package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzer;
import java.util.Optional;

/**
 * メジャーモード。バッファのファイルタイプに応じて1つだけ有効になる。
 * モード固有のキーマップとシンタックススタイリングを提供する。
 */
public interface MajorMode {

    /**
     * モード名を返す。モードラインに表示される。
     */
    String name();

    /**
     * モード固有のキーマップを返す。
     * キーバインドを持たない場合はemptyを返す。
     */
    Optional<Keymap> keymap();

    /**
     * シンタックススタイラーを返す。
     * スタイリング不要の場合はemptyを返す。
     */
    default Optional<SyntaxStyler> styler() {
        return Optional.empty();
    }

    /**
     * 構文解析器を返す。
     * 構文解析が不要の場合はemptyを返す。
     */
    default Optional<SyntaxAnalyzer> syntaxAnalyzer() {
        return Optional.empty();
    }

    /**
     * モード固有の設定デフォルト値を返す。
     * カスタムポイントを持たない場合は空のModeSettingsを返す。
     */
    default ModeSettings settingDefaults() {
        return ModeSettings.empty();
    }

    /**
     * モード固有のコマンドレジストリを返す。
     * モード固有のコマンドを持たない場合はemptyを返す。
     */
    default Optional<CommandRegistry> commandRegistry() {
        return Optional.empty();
    }
}
