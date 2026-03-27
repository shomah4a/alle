package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.styling.Face;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaceRangeListTest {

    private FaceRangeList list;

    @BeforeEach
    void setUp() {
        list = new FaceRangeList();
    }

    @Nested
    class face範囲の追加と取得 {

        @Test
        void 追加した範囲のfaceSpansを取得できる() {
            list.put(0, 5, Face.BOLD_FACE);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
        }

        @Test
        void 複数範囲を追加して各範囲を取得できる() {
            list.put(0, 3, Face.BOLD_FACE);
            list.put(5, 8, Face.KEYWORD);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(1).start());
        }

        @Test
        void クエリ範囲外のfaceは取得されない() {
            list.put(0, 3, Face.BOLD_FACE);
            list.put(10, 15, Face.KEYWORD);

            var spans = list.getFaceSpans(4, 9);
            assertTrue(spans.isEmpty());
        }

        @Test
        void クエリ範囲と部分的に重なるfaceはクランプされて返される() {
            list.put(0, 10, Face.BOLD_FACE);

            var spans = list.getFaceSpans(3, 7);
            assertEquals(1, spans.size());
            assertEquals(3, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
        }

        @Test
        void 重複する範囲を追加すると既存の非重複部分が保持され新範囲で分割される() {
            list.put(0, 5, Face.BOLD_FACE);
            list.put(3, 8, Face.KEYWORD);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            // [0,3) は元のBOLD_FACEが保持される
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            // [3,8) は新しいKEYWORDで上書き
            assertEquals(3, spans.get(1).start());
            assertEquals(8, spans.get(1).end());
            assertEquals(Face.KEYWORD, spans.get(1).face());
        }

        @Test
        void 既存範囲の中央に新範囲を追加すると3分割される() {
            list.put(0, 10, Face.BOLD_FACE);
            list.put(3, 7, Face.KEYWORD);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(3, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            assertEquals(3, spans.get(1).start());
            assertEquals(7, spans.get(1).end());
            assertEquals(Face.KEYWORD, spans.get(1).face());
            assertEquals(7, spans.get(2).start());
            assertEquals(10, spans.get(2).end());
            assertEquals(Face.BOLD_FACE, spans.get(2).face());
        }

        @Test
        void 完全に同じ範囲を上書きすると置換される() {
            list.put(0, 5, Face.BOLD_FACE);
            list.put(0, 5, Face.KEYWORD);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(1, spans.size());
            assertEquals(Face.KEYWORD, spans.get(0).face());
        }

        @Test
        void 結果はstart順にソートされる() {
            list.put(5, 8, Face.KEYWORD);
            list.put(0, 3, Face.BOLD_FACE);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(1).start());
        }
    }

    @Nested
    class face範囲の除去 {

        @Test
        void 完全一致する範囲を除去できる() {
            list.put(0, 5, Face.BOLD_FACE);
            list.remove(0, 5);

            assertTrue(list.getFaceSpans(0, 10).isEmpty());
        }

        @Test
        void 部分的に重なる除去でエントリが分割される() {
            list.put(0, 10, Face.BOLD_FACE);
            list.remove(3, 7);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(7, spans.get(1).start());
            assertEquals(10, spans.get(1).end());
            // 分割後もfaceは保持される
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            assertEquals(Face.BOLD_FACE, spans.get(1).face());
        }
    }

    @Nested
    class putとremoveの組み合わせ {

        @Test
        void putで分割された範囲の一部をremoveで除去できる() {
            list.put(0, 10, Face.BOLD_FACE);
            list.put(3, 7, Face.KEYWORD);
            // [0,3,BOLD) + [3,7,KEYWORD) + [7,10,BOLD) の状態
            list.remove(3, 7);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            assertEquals(7, spans.get(1).start());
            assertEquals(10, spans.get(1).end());
            assertEquals(Face.BOLD_FACE, spans.get(1).face());
        }

        @Test
        void removeで全範囲を除去すると空になる() {
            list.put(0, 10, Face.BOLD_FACE);
            list.put(3, 7, Face.KEYWORD);
            list.remove(0, 10);

            assertTrue(list.getFaceSpans(0, 10).isEmpty());
        }

        @Test
        void 複数のputの後にremoveで中央を除去すると両端が残る() {
            list.put(0, 5, Face.BOLD_FACE);
            list.put(5, 10, Face.KEYWORD);
            list.remove(3, 7);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            assertEquals(7, spans.get(1).start());
            assertEquals(10, spans.get(1).end());
            assertEquals(Face.KEYWORD, spans.get(1).face());
        }

        @Test
        void putで上書き後にremoveしても元のfaceは復活しない() {
            list.put(0, 10, Face.BOLD_FACE);
            list.put(0, 10, Face.KEYWORD);
            list.remove(3, 7);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(Face.KEYWORD, spans.get(0).face());
            assertEquals(Face.KEYWORD, spans.get(1).face());
        }

        @Test
        void 隣接する範囲にputしてremoveで境界をまたいで除去する() {
            list.put(0, 5, Face.BOLD_FACE);
            list.put(5, 10, Face.KEYWORD);
            list.remove(4, 6);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(4, spans.get(0).end());
            assertEquals(Face.BOLD_FACE, spans.get(0).face());
            assertEquals(6, spans.get(1).start());
            assertEquals(10, spans.get(1).end());
            assertEquals(Face.KEYWORD, spans.get(1).face());
        }
    }

    @Nested
    class テキスト挿入時の範囲調整 {

        @Test
        void 範囲より前への挿入で範囲全体がシフトする() {
            list.put(5, 10, Face.BOLD_FACE);
            list.adjustForInsert(0, 3);

            var spans = list.getFaceSpans(0, 20);
            assertEquals(1, spans.size());
            assertEquals(8, spans.get(0).start());
            assertEquals(13, spans.get(0).end());
        }

        @Test
        void 範囲内部への挿入で範囲が拡大する() {
            list.put(0, 10, Face.BOLD_FACE);
            list.adjustForInsert(5, 3);

            var spans = list.getFaceSpans(0, 20);
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(13, spans.get(0).end());
        }

        @Test
        void rearNonstickyで範囲末尾への挿入では範囲が拡大しない() {
            list.put(0, 5, Face.BOLD_FACE);
            list.adjustForInsert(5, 3);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(5, spans.get(0).end());
        }
    }

    @Nested
    class テキスト削除時の範囲調整 {

        @Test
        void 範囲より前の削除で範囲全体がシフトする() {
            list.put(5, 10, Face.BOLD_FACE);
            list.adjustForDelete(0, 3);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(1, spans.size());
            assertEquals(2, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
        }

        @Test
        void 範囲を完全に含む削除でエントリが除去される() {
            list.put(3, 7, Face.BOLD_FACE);
            list.adjustForDelete(0, 10);

            assertTrue(list.getFaceSpans(0, 10).isEmpty());
        }

        @Test
        void 範囲内部の削除で範囲が縮小する() {
            list.put(0, 10, Face.BOLD_FACE);
            list.adjustForDelete(3, 4);

            var spans = list.getFaceSpans(0, 10);
            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(6, spans.get(0).end());
        }
    }

    @Nested
    class clear {

        @Test
        void clearで全範囲が除去される() {
            list.put(0, 5, Face.BOLD_FACE);
            list.put(10, 15, Face.KEYWORD);
            list.clear();

            assertTrue(list.getFaceSpans(0, 20).isEmpty());
        }
    }
}
