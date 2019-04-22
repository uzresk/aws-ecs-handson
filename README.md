# Fargate Hands On

以下の手順に従うことで、このecs-sample-appをFargateにデプロイすることができます。  
所要時間：30分

## 前提事項

コンテナはAmazonLinux2をベースにしているためAWS EC2上でのデプロイが前提となります。

## ビルド用EC2を起動する

EC2インスタンスを起動します。  
インスタンスタイプはt2.micro, ディスクもデフォルト8GBで十分です。  
ロールはECSHandsonEC2Roleという名前で、AmazonEC2ContainerRegistryFullAccessだけ付与しておいてください。  

## アプリケーションのビルド・パッケージング

gitをインストール

```
yum -y install git
```

ソースを取得

```
git clone https://github.com/uzresk/aws-ecs-handson.git
```

OpenJDKをインストール

```
yum -y install java-1.8.0-openjdk.x86_64 java-1.8.0-openjdk-devel.x86_64
```

環境変数を設定

```
export JAVA_HOME=/usr/lib/jvm/java-openjdk/
```

コンパイルしてパッケージングする（target/app-0.0.1-SNAPSHOT.jarが作成される）

```
chmod +x mvnw
./mvnw package
```

起動を確認しておく

```
java -jar target/app-0.0.1-SNAPSHOT.jar
curl http://localhost:8080/
```

## コンテナの作成

Dockerをインストール

```
yum -y install docker
systemctl restart docker
```

Dockerfileをbuild

```
docker build .
```

こんなメッセージがでたら成功

```
Successfully built cb2ced2315c5
```

コンテナの起動

```
docker run -d --name sample-app -p 8080:8080 c2c52abb9936
```

コンテナで起動したWebサーバの動作を検証

```
curl http://localhost:8080
```

## コンテナをECRへPushする

ECRのリポジトリにログインする

```
`aws ecr get-login --region ap-northeast-1 --no-include-email`
```

リポジトリを作成する

```
aws ecr create-repository --repository-name ecs-handson-app --region ap-northeast-1
{
    "repository": {
        "registryId": "XXXXXXXXXXXX",
        "repositoryName": "ecs-handson",
        "repositoryArn": "arn:aws:ecr:ap-northeast-1:XXXXXXXXXXXX:repository/ecs-handson",
        "createdAt": 1554079560.0,
        "repositoryUri": "XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson"
    }
}
```

タグをつける

```
[root@ip-10-0-1-13 ecs-sample-app]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
<none>              <none>              ea6fda1533f8        6 seconds ago       635MB
amazonlinux         2.0.20190228        01da4f8f9748        4 weeks ago         162MB
```

```
docker tag ea6fda1533f8 XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app:latest
```


```
[root@ip-10-0-1-13 ecs-sample-app]# docker images
REPOSITORY                                                          TAG                 IMAGE ID            CREATED              SIZE
XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app   latest              ea6fda1533f8        About a minute ago   635MB
amazonlinux                                                         2.0.20190228        01da4f8f9748        4 weeks ago          162MB
```

pushする

```
docker push XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app:latest
```

## Application LoadBalancerの作成

名前：ecs-handson-alb  
ロードバランサのプロトコル：HTTP ポート：80  
VPC,Subnetを指定する  

セキュリティグループ：ECSHandsonSG


新しいターゲットグループ  
名前：ecs-handson-tg  
ターゲットの種類：IP  
プロトコル：HTTP  
ポート：8080  
ヘルスチェック：HTTP /  
ヘルスチェックの詳細設定：しきい値を2, 間隔を10秒に変更  
  
ターゲットの登録：そのまま何も押さずに「次へ」  

## ECSの作成

### クラスタの作成

ECS → クラスタの作成 → ネットワーキングのみ → 次のステップ  
クラスタ名：ecs-handson-cluster  

### タスクの作成

起動タイプの互換性の選択：FARGATE  
タスク定義名：ecs-handson-task  
タスクロール：ECSHandsonECSTaskRole  
タスクメモリ：1GB  
タスクCPU：0.5vCPU  
  
コンテナの追加  
コンテナ名：ecshandsoncontainer  
イメージ：XXXXXXXXXXXX.dkr.ecr.ap-northeast-1.amazonaws.com/ecs-handson-app:latest  
ポートマッピング：8080  

### サービスの作成

起動タイプ：Fargate  
タスク定義：ecs-handson-task  
プラットフォームのバージョン：LATEST  
クラスタ：ecs-handson-cluster  
サービス名：app  
タスクの数：1  
最小ヘルス数：100  
最大率：200  
  
クラスタVPC：任意  
セキュリティグループ：ALB:8080からのアクセスが許容できるように設定  
ELBタイプ：ApplicationLoadBalancer  
ELB名：ecs-handson-alb  
コンテナの選択：ELBへの追加  
リスナーポート：80:HTTP  
ターゲットグループ：ecs-handson-tg  

## 接続の確認

* ECSのサービス、タスクの確認
* ターゲットグループの登録済ターゲットの確認
* ApplicationLoadbalancerのDNS名を使ってブラウザで確認
* ログの確認

## 後片付け

放置すると課金されてしまうのでリソースを削除します。

* ECS → ecs-handson-cluster → サービスを選択してタスクの実行数を0にする
* ECS → ecs-handson-cluster → サービスを選択して削除
* ECS → ecs-handson-cluster → 画面右上「クラスターの削除」

* ECR → リポジトリを選択して削除

* ロードバランサ → ecs-handson-alb → 削除
* ターゲットグループ → ecs-handson-tg → 削除
