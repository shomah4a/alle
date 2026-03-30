package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import org.eclipse.collections.api.list.ListIterable;

/**
 * テキストプロパティの範囲管理を行う。
 * 各プロパティは半開区間 [start, end) で管理される。
 * rear-nonsticky: 範囲末尾位置への挿入はプロパティに含まれない。
 *
 * <p>属性種別ごとに独立したリストで管理するため、
 * 異なる属性の操作が互いに干渉しない。
 */
class TextPropertyStore {

    private final RangeList<RangeList.Flag> readOnlyRanges = new RangeList<>();
    private final RangeList<RangeList.Flag> pointGuardRanges = new RangeList<>();
    private final RangeList<FaceName> faceRanges = new RangeList<>();

    /**
     * 指定範囲にread-onlyを設定する。
     */
    void putReadOnly(int start, int end) {
        readOnlyRanges.put(start, end, RangeList.Flag.ON);
    }

    /**
     * 指定範囲のread-onlyを解除する。
     * 範囲が完全に含まれるエントリを除去し、部分的に重なるエントリは縮小する。
     */
    void removeReadOnly(int start, int end) {
        readOnlyRanges.remove(start, end);
    }

    /**
     * 指定位置がread-onlyかどうかを返す。
     * 半開区間 [start, end) で判定する（rear-nonsticky）。
     */
    boolean isReadOnly(int index) {
        return readOnlyRanges.contains(index);
    }

    /**
     * 指定範囲にread-onlyプロパティが設定されているかチェックする。
     * 範囲 [start, start + count) 内のいずれかの位置にread-onlyがあればtrue。
     */
    boolean hasReadOnly(int start, int count) {
        return readOnlyRanges.hasAny(start, count);
    }

    /**
     * 指定範囲にpointGuard（カーソル進入禁止）を設定する。
     */
    void putPointGuard(int start, int end) {
        pointGuardRanges.put(start, end, RangeList.Flag.ON);
    }

    /**
     * 指定範囲のpointGuardを解除する。
     */
    void removePointGuard(int start, int end) {
        pointGuardRanges.remove(start, end);
    }

    /**
     * 指定位置がpointGuard（カーソル進入禁止）かどうかを返す。
     */
    boolean isPointGuard(int index) {
        return pointGuardRanges.contains(index);
    }

    /**
     * 指定位置がpointGuard範囲内の場合、侵入方向と逆方向に押し戻した位置を返す。
     * forward（前方移動）でガードに入った場合はガードのend位置に、
     * backward（後方移動）でガードに入った場合はガードのstart位置に押し戻す。
     * ただし、押し戻し先自体がガード範囲内（start=0のケース等）の場合はend位置に押し出す。
     * 範囲外の場合は元の位置をそのまま返す。
     *
     * @param index 解決対象の位置
     * @param forward trueなら前方移動、falseなら後方移動
     */
    int resolvePointGuard(int index, boolean forward) {
        if (forward) {
            int end = pointGuardRanges.findEnd(index);
            return end >= 0 ? end : index;
        } else {
            int start = pointGuardRanges.findStart(index);
            if (start < 0) {
                return index;
            }
            int beforeGuard = start - 1;
            // 押し戻し先が無効（start=0）または別のガード範囲内の場合はend側に押し出す
            if (beforeGuard < 0 || pointGuardRanges.contains(beforeGuard)) {
                int end = pointGuardRanges.findEnd(index);
                return end >= 0 ? end : index;
            }
            return beforeGuard;
        }
    }

    /**
     * 指定範囲にface（表示スタイル）を設定する。
     */
    void putFace(int start, int end, FaceName faceName) {
        faceRanges.put(start, end, faceName);
    }

    /**
     * 指定範囲のfaceを除去する。
     */
    void removeFace(int start, int end) {
        faceRanges.remove(start, end);
    }

    /**
     * 指定範囲内で指定FaceNameを持つfaceのみを除去する。
     * 他のFaceNameのエントリには影響しない。
     */
    void removeFaceByName(int start, int end, FaceName faceName) {
        faceRanges.removeByValue(start, end, faceName);
    }

    /**
     * 指定範囲 [start, end) 内のface範囲をStyledSpanリストとして返す。
     */
    ListIterable<StyledSpan> getFaceSpans(int start, int end) {
        return faceRanges.getEntries(start, end).collect(e -> new StyledSpan(e.start(), e.end(), e.value()));
    }

    /**
     * テキスト挿入時の範囲調整。
     * 挿入位置より後の範囲をシフトする。
     */
    void adjustForInsert(int index, int length) {
        readOnlyRanges.adjustForInsert(index, length);
        pointGuardRanges.adjustForInsert(index, length);
        faceRanges.adjustForInsert(index, length);
    }

    /**
     * テキスト削除時の範囲調整。
     * 削除範囲に応じてエントリを縮小・除去する。
     */
    void adjustForDelete(int index, int count) {
        readOnlyRanges.adjustForDelete(index, count);
        pointGuardRanges.adjustForDelete(index, count);
        faceRanges.adjustForDelete(index, count);
    }

    /**
     * 全エントリを除去する。
     */
    void clear() {
        readOnlyRanges.clear();
        pointGuardRanges.clear();
        faceRanges.clear();
    }
}
