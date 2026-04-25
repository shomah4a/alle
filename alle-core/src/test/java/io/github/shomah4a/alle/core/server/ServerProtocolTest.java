package io.github.shomah4a.alle.core.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerProtocolTest {

    // ── Request パース ──

    @Test
    void OPENリクエストをパースできる() {
        var result = ServerProtocol.parseRequest("OPEN /tmp/COMMIT_EDITMSG");
        assertTrue(result.isPresent());
        var open = assertInstanceOf(ServerProtocol.Request.Open.class, result.get());
        assertEquals("/tmp/COMMIT_EDITMSG", open.absolutePath());
    }

    @Test
    void OPENリクエストのパスにスペースを含む場合もパースできる() {
        var result = ServerProtocol.parseRequest("OPEN /tmp/my file.txt");
        assertTrue(result.isPresent());
        var open = assertInstanceOf(ServerProtocol.Request.Open.class, result.get());
        assertEquals("/tmp/my file.txt", open.absolutePath());
    }

    @Test
    void OPENの後にパスがない場合はemptyを返す() {
        var result = ServerProtocol.parseRequest("OPEN ");
        assertTrue(result.isEmpty());
    }

    @Test
    void 不正なリクエスト行はemptyを返す() {
        assertTrue(ServerProtocol.parseRequest("UNKNOWN command").isEmpty());
        assertTrue(ServerProtocol.parseRequest("").isEmpty());
    }

    // ── Response パース ──

    @Test
    void OPENEDレスポンスをパースできる() {
        var result = ServerProtocol.parseResponse("OPENED COMMIT_EDITMSG");
        assertTrue(result.isPresent());
        var opened = assertInstanceOf(ServerProtocol.Response.Opened.class, result.get());
        assertEquals("COMMIT_EDITMSG", opened.bufferName());
    }

    @Test
    void FINISHEDレスポンスをパースできる() {
        var result = ServerProtocol.parseResponse("FINISHED");
        assertTrue(result.isPresent());
        assertInstanceOf(ServerProtocol.Response.Finished.class, result.get());
    }

    @Test
    void ERRORレスポンスをパースできる() {
        var result = ServerProtocol.parseResponse("ERROR server shutting down");
        assertTrue(result.isPresent());
        var error = assertInstanceOf(ServerProtocol.Response.ServerError.class, result.get());
        assertEquals("server shutting down", error.message());
    }

    @Test
    void 不正なレスポンス行はemptyを返す() {
        assertTrue(ServerProtocol.parseResponse("UNKNOWN response").isEmpty());
        assertTrue(ServerProtocol.parseResponse("").isEmpty());
    }

    // ── エンコード ──

    @Test
    void Openedレスポンスをエンコードできる() {
        var encoded = ServerProtocol.encode(new ServerProtocol.Response.Opened("COMMIT_EDITMSG"));
        assertEquals("OPENED COMMIT_EDITMSG", encoded);
    }

    @Test
    void Finishedレスポンスをエンコードできる() {
        var encoded = ServerProtocol.encode(new ServerProtocol.Response.Finished());
        assertEquals("FINISHED", encoded);
    }

    @Test
    void Errorレスポンスをエンコードできる() {
        var encoded = ServerProtocol.encode(new ServerProtocol.Response.ServerError("something failed"));
        assertEquals("ERROR something failed", encoded);
    }

    @Test
    void Openリクエストをエンコードできる() {
        var encoded = ServerProtocol.encodeRequest(new ServerProtocol.Request.Open("/tmp/test.txt"));
        assertEquals("OPEN /tmp/test.txt", encoded);
    }

    // ── ラウンドトリップ ──

    @Test
    void リクエストのエンコードとパースが往復できる() {
        var original = new ServerProtocol.Request.Open("/home/user/test.txt");
        var encoded = ServerProtocol.encodeRequest(original);
        var parsed = ServerProtocol.parseRequest(encoded);
        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
    }

    @Test
    void Openedレスポンスのエンコードとパースが往復できる() {
        var original = new ServerProtocol.Response.Opened("test.txt");
        var encoded = ServerProtocol.encode(original);
        var parsed = ServerProtocol.parseResponse(encoded);
        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
    }

    @Test
    void Finishedレスポンスのエンコードとパースが往復できる() {
        var original = new ServerProtocol.Response.Finished();
        var encoded = ServerProtocol.encode(original);
        var parsed = ServerProtocol.parseResponse(encoded);
        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
    }

    @Test
    void Errorレスポンスのエンコードとパースが往復できる() {
        var original = new ServerProtocol.Response.ServerError("connection lost");
        var encoded = ServerProtocol.encode(original);
        var parsed = ServerProtocol.parseResponse(encoded);
        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
    }
}
