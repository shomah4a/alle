package io.github.shomah4a.alle.core.search;

/**
 * i-searchの検索履歴を保持する。
 * ISearchForwardCommandとISearchBackwardCommandで共有し、
 * 前回のクエリによる再検索を実現する。
 */
public class ISearchHistory {

    private String lastQuery = "";

    public String getLastQuery() {
        return lastQuery;
    }

    public void setLastQuery(String query) {
        this.lastQuery = query;
    }
}
