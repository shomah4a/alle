package io.github.shomah4a.alle.core.mode.modes.dired;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * diredバッフ��に追加するカスタムカラムの定義。
 * バッファ変数 "dired-custom-columns" に ListIterable&lt;DiredCustomColumn&gt; として格納する。
 *
 * @param header カラムヘッダ名
 * @param renderer パスからセル文字列を生成する関数
 */
public record DiredCustomColumn(String header, Function<Path, String> renderer) {}
