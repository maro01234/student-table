import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

/** Java 17標準ライブラリだけで動く、Render向けのWeb版。 */
public class StudentScoreWeb {
    private static final Map<String, String> ASSETS = Map.of(
            "/", "index.html", "/index.html", "index.html",
            "/app.js", "app.js", "/style.css", "style.css");

    public static void main(String[] args) throws IOException {
        String configuredPort = System.getenv("PORT");
        int port = configuredPort == null || configuredPort.isBlank()
                ? 8080 : Integer.parseInt(configuredPort);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("PORT must be between 1 and 65535");
        }
        // Renderの外部リクエストを受けるため、localhost限定にしない。
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        var executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/", StudentScoreWeb::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            executor.shutdown();
        }));
        server.start();
        System.out.println("Student table listening on 0.0.0.0:" + port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                send(exchange, 405, "text/plain", "Method not allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/healthz")) {
                send(exchange, 200, "text/plain", "ok".getBytes(StandardCharsets.UTF_8));
                return;
            }
            if (path.equals("/api/students")) {
                String json = """
                        [{"name":"佐藤 花子","score":85.5},
                         {"name":"鈴木 太郎","score":72.0},
                         {"name":"高橋 美咲","score":91.3},
                         {"name":"田中 健","score":68.7},
                         {"name":"伊藤 葵","score":88.0}]
                        """;
                send(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
                return;
            }
            String asset = ASSETS.get(path);
            if (asset == null) {
                send(exchange, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
                return;
            }
            byte[] body;
            try (InputStream input = StudentScoreWeb.class.getResourceAsStream("/public/" + asset)) {
                // JAR実行と、java StudentScoreWeb.javaによる直接実行の両方に対応。
                body = input != null ? input.readAllBytes() : Files.readAllBytes(Path.of("public", asset));
            }
            String type = asset.endsWith(".js") ? "text/javascript"
                    : asset.endsWith(".css") ? "text/css" : "text/html";
            send(exchange, 200, type, body);
        }
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type + "; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; "
                + "base-uri 'none'; frame-ancestors 'none'");
        if (exchange.getRequestMethod().equals("HEAD")) {
            headers.set("Content-Length", Integer.toString(body.length));
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
