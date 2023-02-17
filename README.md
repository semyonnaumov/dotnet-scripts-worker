# dotnet-scripts-worker

Part of [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts) project. For complete reference
address [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts).

Service, responsible for running scripts in Docker-in-Docker (or Docker-out-of-Docker) containers and reporting results
to [schedulers](https://github.com/semyonnaumov/dotnet-scripts-scheduler).

⚠️ Full description is currently available only in Russian.

## 1. Описание

Модуль, который непосредственно запускает скрипты. Воркеры используют Docker для запуска задач в контейнерах,
запускаемых из образа `sandbox` с .Net рантаймом (Dockerfile для этого образа находится в
`sandbox/linux-amd64-dotnet-7`). В данный момент имеется только один тип песочницы -
`linux-amd64-dotnet-7`. По запросу на вычисление, полученному из кафки от планировщика, воркер поднимает новый
контейнер, конфигурирует NuGet в контейнере для работы с кастомными фидами и запускает интерпретатор скрипта.
После остановки контейнера (естественного
или принудительного) воркер извлекает результаты, и удаляет контейнер.

Воркер читает задачи на запуск из топика, соответствующего его типу, сообщает в `running` о запущенных задачах
и по завершению (успешному или с ошибкой) скрипта отправляет в топик `finished` результаты запусков.

Dockerfile воркера позволяет собрать его в образ с докером-внутри-докера (DinD) для запуска контейнеров со скриптами.
Для включения и выключения режима контроля ресурсов контейнера со скриптом приложение использует переменную
окружения `WORKER_ENABLE_RESOURCE_LIMITS` (`true` по умолчанию). Пока что при запуске в режиме DinD в приложении нельзя
ограничивать ресурсы контейнера, и оно будет корректно работать только с `WORKER_ENABLE_RESOURCE_LIMITS=false`.

## 2. Запуск dotnet-scripts-worker

Для запуска приложения нужно поднять набор контейнеров с кафкой из локального `docker-compose.yaml`:

```bash
docker compose up -d
```

Запускать приложение следует, указав в переменной окружения `WORKER_JOB_FILES_HOST_DIR` директорию, которую
приложение будет использовать для создания временных файлов. По умолчанию это `/tmp/scripts`.
После этого можно собрать и запустить приложение:

```bash
WORKER_JOB_FILES_HOST_DIR=$HOME/tmp
./gradlew bootRun
```

⚠️ Внимание! Для запуска приложения на Windows нужно задать переменную
окружения `WORKER_DOCKER_HOST=tcp://localhost:2376`

⚠️ При таком запуске приложение будет использовать локальный докер для поднятия контейнеров со скриптами.

Можно также запускать приложение в Docker-контейнере в режиме DinD. Для этого нужно собрать образ приложения:

```bash
docker build -t dotnet-scripts-worker:dind .
```

И запустить из него контейнер с флагом `--privileged` и подключить к сети из docker-compose.yaml:

```bash
docker run --privileged --name dotnet-scripts-worker \
    --network=dotnet-scripts-worker_dsw-network -it --rm \
    -e WORKER_KAFKA_BROKER_URL=kafka-broker-1:9092 \
    -e WORKER_ENABLE_RESOURCE_LIMITS=false \
    dotnet-scripts-worker:dind
```

⚠️ Запуск воркера совместно с планировщиком описан в [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts).

## 3. Взаимодействие с приложением

Воркер взаимодействует с планировщиками посредством kafka.

Для отправки сообщений воркеру нужно подключиться к контейнеру с брокером кафки и запустить в нем консольного
продюсера для топика `pending-linux-amd64-dotnet-7`:

```bash
docker exec -it dsw-kafka-broker-1 sh
```

```bash
kafka-console-producer --bootstrap-server localhost:9092 \
	--topic pending-linux-amd64-dotnet-7 --property "parse.key=true" \
	--property "key.separator=:"
```

В качестве ключа для отправки можно использовать что угодно, ключ не читается приложением, но используется кафкой для
определения партиции, в которую отправится сообщение (приложение использует идентификатор джобы в качестве ключа,
когда отправляет сообщения). Структура сообщений определяется DTO-классами
пакета `com.naumov.dotnetscriptsworker.dto`.

Пример сообщения с задачей для запуска скрипта:

```bash
key1:{"jobId": "7f000001-863c-17b2-8186-3cdeb62e0000","script": "Console.WriteLine(\"Hello from script\");","jobConfig": {"nugetConfigXml": "<?xml version=\"1.0\" encoding=\"utf-8\"?><configuration><packageSources><add key=\"NuGet official package source\" value=\"https://nuget.org/api/v2/\" /></packageSources><activePackageSource><add key=\"All\" value=\"(Aggregate source)\" /></activePackageSource></configuration>"}}
```

Для того чтобы посмотреть сообщения, отправляемые воркером, нужно запустить консольные консьюмеры
для топиков `running` и  `finished` с консьюмер-группой `console`:

```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
	--topic running --from-beginning --group console
```

```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
	--topic finished --from-beginning --group console
```