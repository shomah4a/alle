package io.github.shomah4a.alle.core.textmodel;

/**
 * テキストモデルの契約。
 * エディタのバッファが内部的に使用するテキストデータ構造のインターフェース。
 * 位置指定はすべてコードポイント単位で行う。
 * 改行コードはLF({@code \n})に正規化されていることを前提とする。
 */
public interface TextModel {

    /**
     * テキストの長さをコードポイント数で返す。
     */
    int length();

    /**
     * 指定位置のコードポイントを返す。
     *
     * @param index コードポイント単位のインデックス
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     */
    int codePointAt(int index);

    /**
     * 指定位置に文字列を挿入する。
     *
     * @param index コードポイント単位の挿入位置
     * @param text  挿入する文字列
     * @throws IndexOutOfBoundsException indexが範囲外の場合
     */
    void insert(int index, String text);

    /**
     * 指定位置から指定コードポイント数を削除する。
     *
     * @param index コードポイント単位の開始位置
     * @param count 削除するコードポイント数
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    void delete(int index, int count);

    /**
     * 指定範囲の部分文字列を返す。
     *
     * @param start 開始位置(含む、コードポイント単位)
     * @param end   終了位置(含まない、コードポイント単位)
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    String substring(int start, int end);

    /**
     * 行数を返す。空テキストの場合は1を返す。
     */
    int lineCount();

    /**
     * 指定オフセットが属する行のインデックスを返す。
     * 改行文字上のオフセットはその行の末尾として扱う。
     *
     * @param offset コードポイント単位のオフセット(0〜length()の範囲)
     * @throws IndexOutOfBoundsException offsetが範囲外の場合
     */
    int lineIndexForOffset(int offset);

    /**
     * 指定行の先頭オフセットをコードポイント単位で返す。
     *
     * @param lineIndex 0始まりの行インデックス
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    int lineStartOffset(int lineIndex);

    /**
     * 指定行のテキストを返す(改行文字を含まない)。
     *
     * @param lineIndex 0始まりの行インデックス
     * @throws IndexOutOfBoundsException lineIndexが範囲外の場合
     */
    String lineText(int lineIndex);

    /**
     * 全テキストを文字列として返す。
     */
    String getText();
}
