package io.github.shomah4a.alle.core.input;

import java.nio.file.Path;

/**
 * ファイルパスの ~ (チルダ) とHOMEディレクトリ間の変換、およびシャドウ境界の検出を提供する。
 */
public final class PathResolver {

    private PathResolver() {}

    /**
     * 絶対パスのHOME部分を ~ に置換して表示用文字列を生成する。
     * HOMEディレクトリ配下でないパスはそのまま返す。
     */
    public static String collapseTilde(String path, Path homeDirectory) {
        String home = homeDirectory.toString();
        if (path.equals(home)) {
            return "~";
        }
        String homeWithSlash = home + "/";
        if (path.startsWith(homeWithSlash)) {
            return "~/" + path.substring(homeWithSlash.length());
        }
        return path;
    }

    /**
     * ~ で始まるパスをHOMEの絶対パスに展開する。
     * ~ で始まらないパスはそのまま返す。
     */
    public static String expandTilde(String path, Path homeDirectory) {
        if (path.equals("~")) {
            return homeDirectory.toString();
        }
        if (path.startsWith("~/")) {
            return homeDirectory + path.substring(1);
        }
        return path;
    }

    /**
     * ファイルパス入力文字列のシャドウ境界位置を返す。
     * シャドウとは、新しいルート指定（/~ や //）によって無効化された先行パス部分のこと。
     * 戻り値は有効パスの開始位置であり、0の場合はシャドウなし。
     *
     * <p>検出パターン:
     * <ul>
     *   <li>{@code /~} — チルダが新しいルートとなり、それ以前がシャドウ</li>
     *   <li>{@code //} — 2つ目のスラッシュが新しいルートとなり、それ以前がシャドウ</li>
     * </ul>
     */
    public static int findShadowBoundary(String input) {
        for (int i = input.length() - 1; i > 0; i--) {
            char c = input.charAt(i);
            char prev = input.charAt(i - 1);
            if (c == '~' && prev == '/') {
                return i;
            }
            if (c == '/' && prev == '/') {
                return i;
            }
        }
        return 0;
    }
}
