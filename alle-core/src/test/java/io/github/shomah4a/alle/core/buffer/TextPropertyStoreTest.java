package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextPropertyStoreTest {

    private TextPropertyStore store;

    @BeforeEach
    void setUp() {
        store = new TextPropertyStore();
    }

    @Nested
    class readOnlyの設定と取得 {

        @Test
        void 設定した範囲内の位置がreadOnlyになる() {
            store.putReadOnly(0, 5);

            assertTrue(store.isReadOnly(0));
            assertTrue(store.isReadOnly(4));
        }

        @Test
        void 半開区間のend位置はreadOnlyにならない() {
            store.putReadOnly(0, 5);

            assertFalse(store.isReadOnly(5));
        }

        @Test
        void 範囲外の位置はreadOnlyにならない() {
            store.putReadOnly(5, 10);

            assertFalse(store.isReadOnly(4));
            assertFalse(store.isReadOnly(10));
        }
    }

    @Nested
    class readOnlyの解除 {

        @Test
        void 完全一致する範囲のreadOnlyを解除できる() {
            store.putReadOnly(0, 5);
            store.removeReadOnly(0, 5);

            assertFalse(store.isReadOnly(2));
        }

        @Test
        void 部分的に重なる範囲の解除でエントリが縮小される() {
            store.putReadOnly(0, 10);
            store.removeReadOnly(3, 7);

            assertTrue(store.isReadOnly(2));
            assertFalse(store.isReadOnly(5));
            assertTrue(store.isReadOnly(8));
        }
    }

    @Nested
    class hasReadOnly {

        @Test
        void readOnly範囲内への操作を検出する() {
            store.putReadOnly(0, 5);

            assertTrue(store.hasReadOnly(0, 3));
            assertTrue(store.hasReadOnly(3, 5));
        }

        @Test
        void readOnly範囲外への操作は検出しない() {
            store.putReadOnly(0, 5);

            assertFalse(store.hasReadOnly(5, 3));
            assertFalse(store.hasReadOnly(10, 1));
        }
    }

    @Nested
    class テキスト挿入時の範囲調整 {

        @Test
        void 範囲より後への挿入では範囲が変化しない() {
            store.putReadOnly(0, 5);
            store.adjustForInsert(10, 3);

            assertTrue(store.isReadOnly(4));
            assertFalse(store.isReadOnly(5));
        }

        @Test
        void 範囲より前への挿入で範囲全体がシフトする() {
            store.putReadOnly(5, 10);
            store.adjustForInsert(0, 3);

            assertFalse(store.isReadOnly(5));
            assertTrue(store.isReadOnly(8));
            assertTrue(store.isReadOnly(12));
            assertFalse(store.isReadOnly(13));
        }

        @Test
        void 範囲内部への挿入で範囲が拡大する() {
            store.putReadOnly(0, 10);
            store.adjustForInsert(5, 3);

            assertTrue(store.isReadOnly(0));
            assertTrue(store.isReadOnly(12));
            assertFalse(store.isReadOnly(13));
        }

        @Test
        void rearNonstickyで範囲末尾への挿入では範囲が拡大しない() {
            store.putReadOnly(0, 5);
            store.adjustForInsert(5, 3);

            assertTrue(store.isReadOnly(4));
            assertFalse(store.isReadOnly(5));
        }
    }

    @Nested
    class テキスト削除時の範囲調整 {

        @Test
        void 範囲より後の削除では範囲が変化しない() {
            store.putReadOnly(0, 5);
            store.adjustForDelete(10, 3);

            assertTrue(store.isReadOnly(4));
        }

        @Test
        void 範囲より前の削除で範囲全体がシフトする() {
            store.putReadOnly(5, 10);
            store.adjustForDelete(0, 3);

            assertTrue(store.isReadOnly(2));
            assertTrue(store.isReadOnly(6));
            assertFalse(store.isReadOnly(7));
        }

        @Test
        void 範囲を完全に含む削除でエントリが除去される() {
            store.putReadOnly(3, 7);
            store.adjustForDelete(0, 10);

            assertFalse(store.isReadOnly(0));
        }

        @Test
        void 範囲内部の削除で範囲が縮小する() {
            store.putReadOnly(0, 10);
            store.adjustForDelete(3, 4);

            assertTrue(store.isReadOnly(0));
            assertTrue(store.isReadOnly(5));
            assertFalse(store.isReadOnly(6));
        }
    }

    @Nested
    class clear {

        @Test
        void clearで全エントリが除去される() {
            store.putReadOnly(0, 5);
            store.putReadOnly(10, 20);
            store.clear();

            assertFalse(store.isReadOnly(2));
            assertFalse(store.isReadOnly(15));
        }
    }
}
