# Java SE 17 Silver：学習内容と理解度

JavaのHTTPサーバーで、第1〜8章の章名・主な学習内容・理解度を表に表示します。

- 第1〜6章は分野別、第7・8章は模擬問題の横断確認。
- 理解度は未入力から開始し、0〜100％で自己評価できます。空欄にすると未入力へ戻せます。
- 章順・理解度順で並べ替え。未入力は理解度ソート時に末尾へ表示します。
- 学習内容は固定、理解度だけを編集します。
- 保存機能はなく、再読み込みすると理解度は未入力に戻ります。

## ローカルでWeb版を実行

JDK 17以降をインストールし、このフォルダーで実行します。

```sh
java StudentScoreWeb.java
```

ブラウザーで [http://localhost:8080](http://localhost:8080) を開きます。終了はターミナルでCtrl+Cです。

ポートを変える場合（macOS/Linux）：

```sh
PORT=10000 java StudentScoreWeb.java
```

## Renderへのデプロイ

このリポジトリをWeb Serviceとして接続し、次を設定します。

| 設定 | 値 |
|---|---|
| Language / Runtime | Docker |
| Branch | main |
| Root Directory | 空欄（リポジトリ直下） |
| Dockerfile Path | ./Dockerfile |
| Docker Build Context Directory | . |
| Docker Command | 空欄（DockerfileのCMDを使用） |
| Health Check Path | /healthz |

`Dockerfile`がJava 17でコンパイルし、画面のない環境でWebサーバーを起動します。サーバーは`0.0.0.0`で待ち受け、Renderが指定する環境変数`PORT`を使用します。既存サービスの修正に、新しいサービスの作成や有料プランへの変更は不要です。

変更を接続ブランチにpushし、自動デプロイが無効なら「Manual Deploy → Deploy latest commit」を実行します。

初回の失敗原因は`Dockerfile`の欠落でした。従来のSwing版はデスクトップ画面用であり、RenderのWeb ServiceとしてHTTPを提供できないため、Web版の起動クラスは`StudentScoreWeb`です。

参考：[Render Web Services](https://render.com/docs/web-services)、[Docker on Render](https://render.com/docs/docker)

## ファイル構成

| ファイル | 役割 |
|---|---|
| StudentScoreWeb.java | HTTPサーバー、章別学習内容のJSON API（/api/chapters）、ヘルスチェック |
| public/index.html | 表のHTML |
| public/app.js | 理解度の編集・検証・並べ替え |
| public/style.css | 表の見た目 |
| Dockerfile | Render用のビルド・起動設定 |
| StudentScoreTable.java | 元のSwing版（ローカルのデスクトップ専用） |
| tests/smoke_test.py | JARでの起動とHTTP応答の検証 |

## 検証

Python 3とJDK 17以降で実行します。

```sh
python3 tests/smoke_test.py
```

一時ディレクトリでJARを作り、画面なし・非標準ポート・プロジェクト外の作業ディレクトリで起動します。HTML、CSS、JavaScript、章別API、ヘルスチェック、HEAD、404、405を確認します。

元のSwing版を起動する場合：

```sh
java StudentScoreTable.java
```
