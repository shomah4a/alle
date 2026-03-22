package io.github.shomah4a.alle.core.styling;

import java.util.Optional;

/**
 * スタイリングの行間状態。アクティブなリージョンルールへの参照を保持する。
 * リージョン外の場合は NONE を使用する。
 *
 * @param activeRegion 現在アクティブなリージョンルール（リージョン外の場合は empty）
 */
public record StylingState(Optional<StylingRule.RegionMatch> activeRegion) {

    public static final StylingState NONE = new StylingState(Optional.empty());

    public boolean isInRegion() {
        return activeRegion.isPresent();
    }
}
