# ECS Hands On


##　ecs-cliとdocker-composeでデプロイ

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
