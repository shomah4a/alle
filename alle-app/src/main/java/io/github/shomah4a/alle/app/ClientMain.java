package io.github.shomah4a.alle.app;

import io.github.shomah4a.alle.core.server.ServerManager;
import io.github.shomah4a.alle.core.server.ServerProtocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * alle --client モードのエントリポイント。
 * 起動中の alle サーバーに接続し、ファイルオープンを依頼して編集完了を待つ。
 * EDITOR 環境変数から呼ばれることを想定する。
 */
final class ClientMain {

    private ClientMain() {}

    /**
     * クライアントモードを実行する。
     *
     * @param filePath 開くファイルのパス
     */
    static void run(String filePath) {
        var absolutePath = Path.of(filePath).toAbsolutePath().normalize().toString();
        var socketPath = ServerManager.resolveSocketPath();
        var address = UnixDomainSocketAddress.of(socketPath);

        try (var channel = SocketChannel.open(address)) {
            var writer = new PrintWriter(
                    new OutputStreamWriter(Channels.newOutputStream(channel), StandardCharsets.UTF_8), true);
            var reader =
                    new BufferedReader(new InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8));

            // OPEN リクエストを送信
            writer.println(ServerProtocol.encodeRequest(new ServerProtocol.Request.Open(absolutePath)));

            // レスポンスを待つ
            String line;
            while ((line = reader.readLine()) != null) {
                var responseOpt = ServerProtocol.parseResponse(line);
                if (responseOpt.isEmpty()) {
                    continue;
                }
                var response = responseOpt.get();
                switch (response) {
                    case ServerProtocol.Response.Opened opened -> {
                        // ファイルが開かれた。FINISHED を待つ。
                    }
                    case ServerProtocol.Response.Finished finished -> {
                        // 編集完了。正常終了。
                        return;
                    }
                    case ServerProtocol.Response.ServerError error -> {
                        System.err.println("alle server error: " + error.message());
                        System.exit(1);
                    }
                }
            }
            // サーバーが接続を閉じた
            System.err.println("alle server closed connection unexpectedly");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("alle: cannot connect to server at " + socketPath + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
