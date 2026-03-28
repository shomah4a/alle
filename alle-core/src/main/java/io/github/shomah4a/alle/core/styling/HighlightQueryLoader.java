package io.github.shomah4a.alle.core.styling;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * クラスパスリソースからTree-sitterハイライトクエリを読み込むユーティリティ。
 *
 * <p>ビルド時にダウンロードされた {@code highlights.scm} を
 * {@code treesitter/<language>/highlights.scm} のパスから読み込む。
 */
public final class HighlightQueryLoader {

    private HighlightQueryLoader() {}

    /**
     * 指定言語のハイライトクエリをクラスパスリソースから読み込む。
     *
     * @param language 言語名（例: "python"）
     * @return クエリ文字列
     * @throws IllegalArgumentException リソースが見つからない場合
     */
    public static String load(String language) {
        String path = "treesitter/" + language + "/highlights.scm";
        try (InputStream is = HighlightQueryLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("ハイライトクエリが見つかりません: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("ハイライトクエリの読み込みに失敗しました: " + path, e);
        }
    }
}
