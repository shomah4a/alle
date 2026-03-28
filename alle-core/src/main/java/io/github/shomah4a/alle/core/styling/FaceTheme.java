package io.github.shomah4a.alle.core.styling;

/**
 * FaceNameをFaceSpecに解決するテーマ。
 * テーマを差し替えることで同じセマンティクスに異なる見た目を割り当てられる。
 */
public interface FaceTheme {

    /**
     * FaceNameに対応するFaceSpecを返す。
     * 未知のFaceNameに対してはデフォルトのFaceSpecを返す。
     */
    FaceSpec resolve(FaceName name);
}
