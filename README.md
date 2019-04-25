# ECS Hands On

以下の手順に従うことで、このecs-sample-appをFargateにデプロイすることができます。  

## 前提事項

コンテナはAmazonLinux2をベースにしているためAWS EC2上でのデプロイが前提となります。

## ビルド用EC2を起動する

EC2インスタンスを起動します。  
インスタンスタイプはt2.micro, ディスクもデフォルト8GBで十分です。  
ロールはECSHandsonEC2Roleという名前で、AmazonEC2ContainerRegistryFullAccess,AmazonECS_FullAccessを付与しておいてください。  

## 準備

Dockerをインストール

```
yum -y install docker
systemctl restart docker
```

gitをインストール

```
yum -y install git
```

ソースを取得

```
git clone https://github.com/uzresk/aws-ecs-handson.git
```

プロジェクトに移動しておきます

```
cd aws-ecs-handson
```

## WEBサーバをDockerで起動する

Dockerfileを使ってコンテナをビルドします。

```
cd web
docker build -t uzresk/web:latest .
```

コンテナができていることを確認します

```
docker images
```
```
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
uzresk/web          latest              da061c3c1121        8 minutes ago       371MB
```

コンテナを起動してindex.htmlにアクセスしてみます

```
docker run --name web -itd -p 80:80 uzresk/web
```

curlコマンドでindex.htmlが返ってくることを確認しましょう。

```
curl http://localhost/index.html
```

## アプリケーションサーバをDockerで起動してみましょう

OpenJDKをインストールします。

```
yum -y install java-1.8.0-openjdk.x86_64 java-1.8.0-openjdk-devel.x86_64
```

mvnwを動かすための環境変数を設定します。

```
export JAVA_HOME=/usr/lib/jvm/java-openjdk/
```

appディレクトリに移動します。  
コンパイルしてパッケージングする（target/app-0.0.1-SNAPSHOT.jarが作成される）

```
chmod +x mvnw
./mvnw package
```

起動を確認しておきましょう。

```
java -jar target/app-0.0.1-SNAPSHOT.jar
curl http://localhost:8080/app/
```

```
<!DOCTYPE html>
<html>
<head>
        <meta charset="UTF-8" />
        <title>Sample Application</title>
</head>
<body>
<h1>Sample Application</h1>
<hr/>
IPAddress:
        <span>ip-10-0-0-10/10.0.0.10</span>
<hr/>
Cookie:

</div>
</body>
</html>
```

アプリケーションサーバのコンテナを作ってみます。

```
docker build -t uzresk/app:latest .
```

アプリケーションサーバのコンテナを起動してみましょう

```
docker run --name app -itd -p 8080:8080 -p 8009:8009 uzresk/app
```

コンテナで起動したWebサーバの動作を確認

```
curl http://localhost:8080/app/
```

コンテナのネットワーク内なのでIPアドレスが変わっていることが確認できると思います。

```
<!DOCTYPE html>
<html>
<head>
        <meta charset="UTF-8" />
        <title>Sample Application</title>
</head>
<body>
<h1>Sample Application</h1>
<hr/>
IPAddress:
        <span>8aef0e18c3e8/172.17.0.3</span>
<hr/>
Cookie:

</div>
</body>
```

## WEBサーバとアプリケーションサーバの連携

WEBサーバの設定で/appにアクセスするとajp://app:8009/app/にプロキシするように設定してありますが、  
WEBサーバとアプリケーションサーバのコンテナネットワークは別なので、このままでは連携できません。  
Dockerコンテナネットワークを使って連携できるようにしてみましょう。

先ほど起動したコンテナを削除します。

```
docker rm -f web
docker rm -f app
```

Dockerコンテナネットワークを作ります

```
docker network create sample-network
```

コンテナネットワークの詳細を確認するには以下のコマンドを実行します

```
docker network ls
docker network inspect sample-network
```

Dockerコンテナ・ネットワーク上でコンテナを起動します。

```
docker run -itd --name app --network sample-network -p 8080:8080 -p 8009:8009 uzresk/app
```

```
docker run -itd --name web --network sample-network -p 80:80 uzresk/web
```

動作確認してみましょう。  
WEBサーバ経由してAPサーバにアクセスできていることが確認できると思います。

```
curl http://localhost/app/
```

```
<!DOCTYPE html>
<html>
<head>
        <meta charset="UTF-8" />
        <title>Sample Application</title>
</head>
<body>
<h1>Sample Application</h1>
<hr/>
IPAddress:
        <span>c4475bff038e/172.18.0.2</span>
<hr/>
Cookie:

</div>
</body>
</html>
```

## docker-composeを使ってWEB、APを連携させる

docker-composeを使うと2つのコンテナを合わせて動かすことができます。

docker-composeのインストール

```
curl -L "https://github.com/docker/compose/releases/download/1.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

```
chmod +x /usr/local/bin/docker-compose
```

```
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
```

インストール出来たらバージョンを確認

```
docker-compose --version
```

WEBとAPを合わせて動かすdocker-compose.ymlを任意の場所に保存します

```
version: '3'
services:
  web:
    image: uzresk/web
    ports:
      - 80:80
    container_name: web
    depends_on:
      - app
  app:
    image: uzresk/app
    ports:
      - 8080:8080
      - 8009:8009
    container_name: app
```

docker-composeを使ってWEB, APを一緒に起動します。 (-dをつけることでバックグラウンドで起動)

```
docker-compose up -d
```

curlで動作確認しておきましょう  
composeで起動したコンテナはコンテナ間通信が有効になっており、それぞれのサーバはcontainer_nameで指定した名前で名前解決できるようになっています。

```
curl http://localhost/app/
```

停止するときは以下のコマンドで行います

```
docker-compose down --rm all
```

参考：[Install Docker Compose | docker docs](https://docs.docker.com/compose/install/)


## Fargateにデプロイしてみよう

今回作った２つのコンテナをFargate上にデプロイして動かしてみたいと思います。

### WEBサーバの設定を変更する

Fargateではコンテナ間の通信がlocalhostで繋がっていますので、WEBサーバからAPサーバにプロキシするURLをlocalhostに変更します。

変更前

```http-proxy.conf
ProxyPass /app ajp://app:8009/app/
```

変更後

```http-proxy.conf
ProxyPass /app ajp://localhost:8009/app/
```

ビルドしておきます

```
docker build -t uzresk/web:latest .
```

### コンテナをECRへPushする

FargateからコンテナをPullするためにコンテナレジストリ（ECR）に置いておく必要があります。

ECRのリポジトリにログインする

```
`aws ecr get-login --region ap-northeast-1 --no-include-email`
```

リポジトリを作成する

```
aws ecr create-repository --repository-name ecs-handson-web --region ap-northeast-1
```

```
{
    "repository": {
        "registryId": "XXXXXXXXXXXX", 
        "repositoryName": "ecs-handson-web", 
        "repositoryArn": "arn:aws:ecr:ap-northeast-1:XXXXXXXXXXXX:repository/ecs-handson-web", 
        "createdAt": 1555996450.0, 
        "repositoryUri": "XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web"
    }
}
```

タグをつけてpushする

```
$ docker images
REPOSITORY                                                          TAG                 IMAGE ID            CREATED             SIZE
uzresk/web                                                          latest              ee7f5868385d        About an hour ago   371MB
```

```
$ docker tag uzresk/web:latest XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web:latest
```

```
$ docker images
REPOSITORY                                                          TAG                 IMAGE ID            CREATED             SIZE
XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web   latest              ee7f5868385d        About an hour ago   371MB
uzresk/web                                                          latest              ee7f5868385d        About an hour ago   371MB
```

pushする

```
docker push XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web:latest
```

ECRにログインしてコンテナがpushされていることを確認しましょう。  

また、uzresk/app:latestもecs-handson-app:latestというタグをつけてpushしておきましょう。  


### Application LoadBalancerの作成

名前：ecs-handson-alb  
ロードバランサのプロトコル：HTTP ポート：80  
VPC,Public Subnetを指定する  
セキュリティグループ：ECSHandsonSG(HTTP, 0.0.0.0のみ許可する)  

新しいターゲットグループ  
名前：ecs-handson-tg  
ターゲットの種類：IP  
プロトコル：HTTP  
ポート：8080  
ヘルスチェック：HTTP /  
ヘルスチェックの詳細設定：しきい値を2, 間隔を10秒に変更  
  
ターゲットの登録：そのまま何も押さずに「次へ」  

### ECSクラスタの作成

ECS → クラスタの作成 → ネットワーキングのみ → 次のステップ  
クラスタ名：ecs-handson-cluster  

### タスクの作成

起動タイプの互換性の選択：FARGATE  
タスク定義名：ecs-handson
タスクロール：ECSHandsonECSTaskRole  
タスクメモリ：1GB  
タスクCPU：0.5vCPU  
  
コンテナの追加  
コンテナ名：web  
イメージ：XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web:latest  
ポートマッピング：80  

### サービスの作成

起動タイプ：Fargate  
タスク定義：ecs-handson
プラットフォームのバージョン：LATEST    
クラスタ：ecs-handson-cluster  
サービス名：web
タスクの数：1  
最小ヘルス数：100  
最大率：200  
  
クラスタVPC：任意  
セキュリティグループ：ALB:80からのアクセスが許容できるように設定  
ヘルスチェックの猶予期間：10
ELBタイプ：ApplicationLoadBalancer  
ELB名：ecs-handson-alb  
コンテナの選択：ELBへの追加  
リスナーポート：80:HTTP  
ターゲットグループ：ecs-handson-tg  

### 接続の確認

* ECSのサービス、タスクの確認
* ターゲットグループの登録済ターゲットの確認
* ApplicationLoadbalancerのDNS名を使ってブラウザで確認
* ログの確認

### アプリケーションサーバをタスクに追加する

先ほど作ったタスク定義にコンテナを追加します。

タスク定義 - ecs-handson - 「新しいリビジョンの作成」
コンテナの追加  
コンテナ名： app 
イメージ：XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app:latest  
ポートマッピング：8009

サービスに新しいタスク定義を反映させます。

サービス - 更新を選択  
タスク定義 Revision：先ほど作ったリビジョンを選択  
新しいデプロイの強制：チェック  

少し待つとデプロイが完了しますので、/appにアクセスできることを確認します。

## ecs-cliとdocker-composeでデプロイ

先ほど作ったECSのサービスを削除しておきましょう。  

docker-composeを以下のように変更します。  

* イメージをECRから取得するように変更
* cloudwatchlogsの設定を追加

```
version: '3'
services:
  web:
    image: XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-web:latest
    ports:
      - 80:80
    logging:
      driver: awslogs
      options: 
        awslogs-group: aws-hands-on
        awslogs-region: ap-northeast-1
        awslogs-stream-prefix: web
  app:
    image: XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app:latest
    ports:
      - 8080:8080
      - 8009:8009
    logging:
      driver: awslogs
      options: 
        awslogs-group: aws-hands-on
        awslogs-region: ap-northeast-1
        awslogs-stream-prefix: app
```

サービスの設定をecs-params.ymlに記載します  
subnetとsgは環境に合わせて修正してください。

```
version: 1
task_definition:
  ecs_network_mode: awsvpc
  task_execution_role: ecsTaskExecutionRole
  task_size:
    cpu_limit: 1024
    mem_limit: 2GB
run_params:
  network_configuration:
    awsvpc_configuration:
      subnets:
        - subnet-XXXXXXXX
        - subnet-XXXXXXXX
      security_groups:
        - sg-xxxxxxxxxxx
      assign_public_ip: DISABLED
```

ecs-cliをインストールします。

```
curl -o /usr/local/bin/ecs-cli https://s3.amazonaws.com/amazon-ecs-cli/ecs-cli-linux-amd64-latest
```

```
chmod +x /usr/local/bin/ecs-cli
```

```
ln -s /usr/local/bin/ecs-cli /usr/bin/ecs-cli
```

クラスタはすでに作成してあるのでecs-cliで設定しましょう

```
ecs-cli configure --region ap-northeast-1 --cluster ecs-hanson-cluster --default-launch-type FARGATE --config-name ecs-hanson
```

ecs-cliを使ってタスク定義・サービスの設定をしてFargateのサービスを起動します。  
TARGETGROUP_ARNは適宜変更してください。  
サービス名は実行するディレクトリ名になります。  

```
ecs-cli compose --file docker-compose.yml --ecs-params ecs-params.yml service up \
--deployment-max-percent 200 \
--deployment-min-healthy-percent 100 \
--target-group-arn TARGETGROUP_ARN
--container-name web \
--container-port 80 \
--launch-type FARGATE \
--health-check-grace-period 120 \
--create-log-groups \
--timeout 10
```

起動したらALBのDNS名でアクセスして動作を確認してみましょう。

サービスを削除するときは以下のコマンドです

```
ecs-cli compose --file docker-compose.yml --ecs-params ecs-params.yml service delete
```

## 後片付け

放置すると課金されてしまうのでリソースを削除します。

* ECS → ecs-handson-cluster → 画面右上「クラスターの削除」
* ECR → リポジトリを選択して削除
* ロードバランサ → ecs-handson-alb → 削除
* ターゲットグループ → ecs-handson-tg → 削除
