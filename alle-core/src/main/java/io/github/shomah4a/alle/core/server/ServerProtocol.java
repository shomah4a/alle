package io.github.shomah4a.alle.core.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * サーバー/クライアント間の JSON Lines プロトコル。
 * 各メッセージは改行区切りの JSON オブジェクト1行で表現される。
 *
 * <pre>
 * リクエスト:  {"type":"open","path":"/tmp/COMMIT_EDITMSG"}
 * レスポンス:  {"type":"opened","buffer":"COMMIT_EDITMSG"}
 *             {"type":"finished"}
 *             {"type":"error","message":"something failed"}
 * </pre>
 */
public final class ServerProtocol {

    private static final Logger logger = Logger.getLogger(ServerProtocol.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
     * レスポンスを JSON 行にエンコードする。
     * 末尾の改行は含まない。
     */
    public static String encode(Response response) {
        var node = MAPPER.createObjectNode();
        switch (response) {
            case Response.Opened opened -> node.put("type", "opened").put("buffer", opened.bufferName());
            case Response.Finished finished -> node.put("type", "finished");
            case Response.ServerError error -> node.put("type", "error").put("message", error.message());
        }
        return node.toString();
    }

    /**
     * リクエストを JSON 行にエンコードする。
     * 末尾の改行は含まない。
     */
    public static String encodeRequest(Request request) {
        var node = MAPPER.createObjectNode();
        switch (request) {
            case Request.Open open -> node.put("type", "open").put("path", open.absolutePath());
        }
        return node.toString();
    }

    /**
     * JSON 行をリクエストにパースする。
     *
     * @param line 改行を含まない JSON 行
     * @return パース結果。不正な形式の場合はempty
     */
    public static Optional<Request> parseRequest(String line) {
        return parseJson(line).flatMap(node -> {
            var type = textField(node, "type");
            if (type.isEmpty()) {
                return Optional.empty();
            }
            if ("open".equals(type.get())) {
                var path = textField(node, "path");
                if (path.isEmpty() || path.get().isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new Request.Open(path.get()));
            }
            return Optional.empty();
        });
    }

    /**
     * JSON 行をレスポンスにパースする。
     *
     * @param line 改行を含まない JSON 行
     * @return パース結果。不正な形式の場合はempty
     */
    public static Optional<Response> parseResponse(String line) {
        return parseJson(line).flatMap(node -> {
            var type = textField(node, "type");
            if (type.isEmpty()) {
                return Optional.empty();
            }
            return switch (type.get()) {
                case "opened" -> textField(node, "buffer").map(Response.Opened::new);
                case "finished" -> Optional.of(new Response.Finished());
                case "error" -> textField(node, "message").map(Response.ServerError::new);
                default -> Optional.empty();
            };
        });
    }

    private static Optional<JsonNode> parseJson(String line) {
        try {
            return Optional.of(MAPPER.readTree(line));
        } catch (JsonProcessingException e) {
            logger.log(Level.FINE, "JSON パースに失敗: " + line, e);
            return Optional.empty();
        }
    }

    private static Optional<String> textField(JsonNode node, String fieldName) {
        var field = node.get(fieldName);
        if (field == null || !field.isTextual()) {
            return Optional.empty();
        }
        return Optional.of(field.asText());
    }
}
