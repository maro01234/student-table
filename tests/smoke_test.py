"""JDKとPython標準ライブラリだけで、デプロイ用JARの動作を確認する。"""
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as directory:
    temp = Path(directory)
    build = temp / "build"
    build.mkdir()
    jar = temp / "app.jar"
    subprocess.run(["javac", "-encoding", "UTF-8", "--release", "17", "-d", str(build),
                    str(root / "StudentScoreWeb.java")], check=True)
    subprocess.run(["jar", "--create", "--file", str(jar), "--main-class", "StudentScoreWeb",
                    "-C", str(build), ".", "-C", str(root), "public"], check=True)
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        port = probe.getsockname()[1]
    env = dict(os.environ, PORT=str(port))
    process = subprocess.Popen(["java", "-Djava.awt.headless=true", "-jar", str(jar)],
                               cwd=temp, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    base = f"http://127.0.0.1:{port}"
    try:
        for attempt in range(100):
            try:
                with urlopen(base + "/healthz", timeout=1) as response:
                    assert response.read() == b"ok"
                break
            except (URLError, TimeoutError):
                if process.poll() is not None:
                    raise RuntimeError(process.stdout.read().decode())
                time.sleep(0.1)
        else:
            raise RuntimeError("Server did not become ready")
        for path, content_type, marker in [
            ("/", "text/html", "章ごとの学習内容と理解度"),
            ("/style.css", "text/css", ".sheet"),
            ("/app.js", "text/javascript", "async function load"),
        ]:
            with urlopen(base + path) as response:
                assert response.status == 200
                assert response.headers.get_content_type() == content_type
                assert marker in response.read().decode("utf-8")
        with urlopen(base + "/api/chapters") as response:
            chapters = json.load(response)
            assert [row["chapter"] for row in chapters] == list(range(1, 9))
            assert chapters[0]["title"] == "Javaの概要と簡単なJavaプログラムの作成"
            assert chapters[7]["title"] == "模擬問題②"
            assert all(row["understanding"] is None and len(row["topics"]) >= 3 for row in chapters)
        with urlopen(Request(base + "/", method="HEAD")) as response:
            assert response.status == 200 and response.read() == b""
        for path, method, expected in [("/missing", "GET", 404), ("/", "POST", 405),
                                        ("/../README.md", "GET", 404)]:
            try:
                urlopen(Request(base + path, method=method))
                raise AssertionError(f"Expected {expected}")
            except HTTPError as error:
                assert error.code == expected
        print("PASS: headless startup, PORT, packaged assets, API, health, HEAD, 404, 405")
    finally:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
