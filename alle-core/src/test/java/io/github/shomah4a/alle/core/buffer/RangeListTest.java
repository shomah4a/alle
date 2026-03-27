package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RangeListTest {

    private RangeList list;

    @BeforeEach
    void setUp() {
        list = new RangeList();
    }

    @Nested
    class 範囲の追加と判定 {

        @Test
        void 追加した範囲内の位置がcontainsでtrueになる() {
            list.put(3, 8);

            assertTrue(list.contains(3));
            assertTrue(list.contains(7));
        }

        @Test
        void 半開区間のend位置はcontainsでfalseになる() {
            list.put(3, 8);

            assertFalse(list.contains(8));
        }

        @Test
        void 範囲外の位置はcontainsでfalseになる() {
            list.put(3, 8);

            assertFalse(list.contains(2));
            assertFalse(list.contains(8));
        }

        @Test
        void 複数範囲を追加して各範囲が独立して判定される() {
            list.put(0, 3);
            list.put(7, 10);

            assertTrue(list.contains(1));
            assertFalse(list.contains(5));
            assertTrue(list.contains(8));
        }

        @Test
        void 重複する範囲を追加すると既存範囲が置換される() {
            list.put(0, 5);
            list.put(3, 8);

            // 旧範囲[0,5)は除去され、新範囲[3,8)のみ
            assertFalse(list.contains(1));
            assertTrue(list.contains(5));
        }
    }

    @Nested
    class 範囲の除去 {

        @Test
        void 完全一致する範囲を除去できる() {
            list.put(3, 8);
            list.remove(3, 8);

            assertFalse(list.contains(5));
        }

        @Test
        void 部分的に重なる除去でエントリが分割される() {
            list.put(0, 10);
            list.remove(3, 7);

            assertTrue(list.contains(2));
            assertFalse(list.contains(5));
            assertTrue(list.contains(8));
        }

        @Test
        void 左端が重なる除去で範囲が縮小される() {
            list.put(3, 10);
            list.remove(0, 5);

            assertFalse(list.contains(4));
            assertTrue(list.contains(5));
        }

        @Test
        void 右端が重なる除去で範囲が縮小される() {
            list.put(0, 8);
            list.remove(5, 10);

            assertTrue(list.contains(4));
            assertFalse(list.contains(5));
        }
    }

    @Nested
    class hasAny {

        @Test
        void 範囲と重なるクエリでtrueを返す() {
            list.put(3, 8);

            assertTrue(list.hasAny(5, 3));
            assertTrue(list.hasAny(0, 5));
            assertTrue(list.hasAny(7, 5));
        }

        @Test
        void 範囲と重ならないクエリでfalseを返す() {
            list.put(3, 8);

            assertFalse(list.hasAny(8, 3));
            assertFalse(list.hasAny(0, 3));
        }
    }

    @Nested
    class findStartとfindEnd {

        @Test
        void 範囲内の位置からstartを返す() {
            list.put(3, 8);

            assertEquals(3, list.findStart(5));
        }

        @Test
        void 範囲内の位置からendを返す() {
            list.put(3, 8);

            assertEquals(8, list.findEnd(5));
        }

        @Test
        void 範囲外の位置からはマイナス1を返す() {
            list.put(3, 8);

            assertEquals(-1, list.findStart(10));
            assertEquals(-1, list.findEnd(10));
        }

        @Test
        void 複数範囲がある場合に正しい範囲のstartとendを返す() {
            list.put(0, 3);
            list.put(7, 10);

            assertEquals(0, list.findStart(1));
            assertEquals(3, list.findEnd(1));
            assertEquals(7, list.findStart(8));
            assertEquals(10, list.findEnd(8));
        }
    }

    @Nested
    class テキスト挿入時の範囲調整 {

        @Test
        void 範囲より前への挿入で範囲全体がシフトする() {
            list.put(5, 10);
            list.adjustForInsert(0, 3);

            assertFalse(list.contains(5));
            assertTrue(list.contains(8));
            assertTrue(list.contains(12));
            assertFalse(list.contains(13));
        }

        @Test
        void 範囲内部への挿入で範囲が拡大する() {
            list.put(0, 10);
            list.adjustForInsert(5, 3);

            assertTrue(list.contains(0));
            assertTrue(list.contains(12));
            assertFalse(list.contains(13));
        }

        @Test
        void rearNonstickyで範囲末尾への挿入では範囲が拡大しない() {
            list.put(0, 5);
            list.adjustForInsert(5, 3);

            assertTrue(list.contains(4));
            assertFalse(list.contains(5));
        }

        @Test
        void 範囲より後への挿入では範囲が変化しない() {
            list.put(0, 5);
            list.adjustForInsert(10, 3);

            assertTrue(list.contains(4));
            assertFalse(list.contains(5));
        }
    }

    @Nested
    class テキスト削除時の範囲調整 {

        @Test
        void 範囲より前の削除で範囲全体がシフトする() {
            list.put(5, 10);
            list.adjustForDelete(0, 3);

            assertTrue(list.contains(2));
            assertTrue(list.contains(6));
            assertFalse(list.contains(7));
        }

        @Test
        void 範囲を完全に含む削除でエントリが除去される() {
            list.put(3, 7);
            list.adjustForDelete(0, 10);

            assertFalse(list.contains(0));
        }

        @Test
        void 範囲内部の削除で範囲が縮小する() {
            list.put(0, 10);
            list.adjustForDelete(3, 4);

            assertTrue(list.contains(0));
            assertTrue(list.contains(5));
            assertFalse(list.contains(6));
        }

        @Test
        void 範囲の前半が削除される() {
            list.put(5, 10);
            list.adjustForDelete(3, 4);

            // 削除[3,7): 範囲[5,10)の前半が削除 → [3, 10-4) = [3, 6)
            assertTrue(list.contains(3));
            assertTrue(list.contains(5));
            assertFalse(list.contains(6));
        }

        @Test
        void 範囲の後半が削除される() {
            list.put(0, 8);
            list.adjustForDelete(5, 5);

            // 削除[5,10): 範囲[0,8)の後半が削除 → [0, 5)
            assertTrue(list.contains(4));
            assertFalse(list.contains(5));
        }

        @Test
        void 範囲より後の削除では範囲が変化しない() {
            list.put(0, 5);
            list.adjustForDelete(10, 3);

            assertTrue(list.contains(4));
        }
    }

    @Nested
    class clear {

        @Test
        void clearで全範囲が除去される() {
            list.put(0, 5);
            list.put(10, 20);
            list.clear();

            assertFalse(list.contains(2));
            assertFalse(list.contains(15));
        }
    }
}
