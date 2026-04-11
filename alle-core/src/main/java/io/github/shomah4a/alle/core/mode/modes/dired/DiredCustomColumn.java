package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.styling.FaceName;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

/**
 * diredバッファに追加するカスタムカラムの定義。
 * バッファ変数 "dired-custom-columns" に ListIterable&lt;DiredCustomColumn&gt; として格納する。
 */
public interface DiredCustomColumn {

    /**
     * カラムヘッダ名を返す。
     */
    String header();

    /**
     * 指定パスのセル文字列を生成する。
     */
    String renderCell(Path path);

    /**
     * 指定パスのセルに適用するfaceを返す。
     * face不要の場合はemptyを返す。
     */
    default Optional<FaceName> face(Path path) {
        return Optional.empty();
    }

    /**
     * テキストのみのカスタムカラムを生成するファクトリメソッド。
     */
    static DiredCustomColumn of(String header, Function<Path, String> renderer) {
        return new DiredCustomColumn() {
            @Override
            public String header() {
                return header;
            }

            @Override
            public String renderCell(Path path) {
                return renderer.apply(path);
            }
        };
    }
}
