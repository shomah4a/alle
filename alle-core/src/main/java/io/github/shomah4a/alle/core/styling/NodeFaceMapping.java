package io.github.shomah4a.alle.core.styling;

import java.util.Optional;
import org.eclipse.collections.api.map.ImmutableMap;

/**
 * Tree-sitterクエリのキャプチャ名をFaceNameに対応付けるマッピング。
 *
 * <p>S式クエリで定義されたキャプチャ名（例: "keyword", "string", "comment"）を
 * エディタのセマンティック名（{@link FaceName}）に変換する。
 */
public final class NodeFaceMapping {

    private final ImmutableMap<String, FaceName> mapping;

    public NodeFaceMapping(ImmutableMap<String, FaceName> mapping) {
        this.mapping = mapping;
    }

    /**
     * キャプチャ名に対応するFaceNameを返す。
     *
     * @param captureName Tree-sitterクエリのキャプチャ名
     * @return 対応するFaceName（マッピングが存在しない場合はempty）
     */
    public Optional<FaceName> resolve(String captureName) {
        FaceName faceName = mapping.get(captureName);
        return Optional.ofNullable(faceName);
    }
}
