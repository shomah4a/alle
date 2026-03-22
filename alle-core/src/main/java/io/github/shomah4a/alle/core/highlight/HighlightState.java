package io.github.shomah4a.alle.core.highlight;

import java.util.Optional;

/**
 * ハイライトの行間状態。アクティブなリージョンルールへの参照を保持する。
 * リージョン外の場合は NONE を使用する。
 *
 * @param activeRegion 現在アクティブなリージョンルール（リージョン外の場合は empty）
 */
public record HighlightState(Optional<HighlightRule.RegionMatch> activeRegion) {

    public static final HighlightState NONE = new HighlightState(Optional.empty());

    public boolean isInRegion() {
        return activeRegion.isPresent();
    }
}
