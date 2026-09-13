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
        System.out.println("Java Silver study table listening on 0.0.0.0:" + port);
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
            if (path.equals("/api/chapters")) {
                String json = """
                        [
                          {"chapter":1,"title":"Javaの概要と簡単なJavaプログラムの作成","topics":["javac・javaによるコンパイルと実行、ソースファイルモード","mainメソッドとコマンドライン引数","パッケージ宣言・import・完全修飾クラス名・クラスパス"],"understanding":null},
                          {"chapter":2,"title":"Javaの基本データ型と文字列の操作","topics":["プリミティブ型・リテラル・変数の初期値・スコープ・var","StringとStringBuilder、不変性、==とequals、文字列プール","文字列の各種メソッド・テキストブロック","配列・多次元配列・clone、ArrayList・Arrays.asList・List.of"],"understanding":null},
                          {"chapter":3,"title":"演算子と制御構造","topics":["拡大・縮小変換、演算時の型昇格、複合代入","前置・後置、評価順序、短絡評価、ビット演算・シフト","if・switch文と式・フォールスルー・yield","while・do-while・for・拡張for、break・continue・ラベル"],"understanding":null},
                          {"chapter":4,"title":"クラスの定義とインスタンスの使用","topics":["参照型・static・フィールド・ローカル変数・値渡し","オーバーロード・可変長引数・戻り値","コンストラクタ・this・super・初期化順序・GC","instanceofのパターンマッチングと有効範囲","レコードの構成要素・アクセサー・標準／コンパクトコンストラクタ"],"understanding":null},
                          {"chapter":5,"title":"継承とインタフェースの使用","topics":["継承・抽象クラス・ポリモーフィズム・キャスト","オーバーライドとオーバーロード、戻り値とチェック例外の制約","アクセス修飾子、フィールドとstaticメソッドの隠蔽","インタフェースのabstract・default・static・privateメソッド","sealed・permits・non-sealedによる継承制御"],"understanding":null},
                          {"chapter":6,"title":"例外処理","topics":["Throwable・Exception・RuntimeException・Errorの分類","チェック例外の処理義務、throw・throws、catchの順序・マルチキャッチ","returnとfinallyの実行順序、戻り値・例外の上書き","try-with-resources・AutoCloseable・逆順クローズ・抑制された例外","NullPointerException・ClassCastExceptionなどの発生条件"],"understanding":null},
                          {"chapter":7,"title":"模擬問題①","topics":["継承とコンストラクタ、ポリモーフィズムとオーバーロードの組み合わせ","defaultメソッドの競合、Stringと==の判定","多次元配列と二重ループ、例外・finally・自動クローズの順序","パッケージ・クラスパスを含む第1〜6章の横断確認"],"understanding":null},
                          {"chapter":8,"title":"模擬問題②","topics":["オートボクシング・アンボクシング・ラッパー型の比較","オーバーロード解決とvarの型推論","レコードと可変コレクション、配列のcloneと参照の共有","拡張forと参照型、同名変数とスコープ","コンパイル可否 → 実行経路 → 実行時例外 → 最終結果の確認"],"understanding":null}
                        ]
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
