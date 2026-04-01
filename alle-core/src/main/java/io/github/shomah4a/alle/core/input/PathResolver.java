package io.github.shomah4a.alle.core.input;

import java.nio.file.Path;

/**
 * ファイルパスの ~ (チルダ) とHOMEディレクトリ間の変換を提供する。
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
}
