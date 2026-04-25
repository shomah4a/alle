package io.github.shomah4a.alle.core.server;

import java.util.Optional;

/**
 * サーバー/クライアント間のテキスト行指向プロトコル。
 * 各メッセージは改行区切りの1行で表現される。
 */
public final class ServerProtocol {

    private ServerProtocol() {}

    /** クライアント → サーバーのリクエスト。 */
    public sealed interface Request {
        /** ファイルオープン要求。 */
        record Open(String absolutePath) implements Request {}
    }

    /** サーバー → クライアントのレスポンス。 */
    public sealed interface Response {
        /** ファイルオープン成功。 */
        record Opened(String bufferName) implements Response {}

        /** 編集完了通知。 */
        record Finished() implements Response {}

        /** エラー通知。 */
        record ServerError(String message) implements Response {}
    }

    /**
     * レスポンスをテキスト行にエンコードする。
     * 末尾の改行は含まない。
     */
    public static String encode(Response response) {
        return switch (response) {
            case Response.Opened opened -> "OPENED " + opened.bufferName();
            case Response.Finished finished -> "FINISHED";
            case Response.ServerError error -> "ERROR " + error.message();
        };
    }

    /**
     * リクエストをテキスト行にエンコードする。
     * 末尾の改行は含まない。
     */
    public static String encodeRequest(Request request) {
        return switch (request) {
            case Request.Open open -> "OPEN " + open.absolutePath();
        };
    }

    /**
     * テキスト行をリクエストにパースする。
     *
     * @param line 改行を含まない1行
     * @return パース結果。不正な形式の場合はempty
     */
    public static Optional<Request> parseRequest(String line) {
        if (line.startsWith("OPEN ")) {
            var path = line.substring("OPEN ".length());
            if (path.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Request.Open(path));
        }
        return Optional.empty();
    }

    /**
     * テキスト行をレスポンスにパースする。
     *
     * @param line 改行を含まない1行
     * @return パース結果。不正な形式の場合はempty
     */
    public static Optional<Response> parseResponse(String line) {
        if (line.startsWith("OPENED ")) {
            var bufferName = line.substring("OPENED ".length());
            return Optional.of(new Response.Opened(bufferName));
        }
        if ("FINISHED".equals(line)) {
            return Optional.of(new Response.Finished());
        }
        if (line.startsWith("ERROR ")) {
            var message = line.substring("ERROR ".length());
            return Optional.of(new Response.ServerError(message));
        }
        return Optional.empty();
    }
}
