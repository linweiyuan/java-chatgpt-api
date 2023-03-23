./gradlew build

java -Djarmode=layertools -jar build/libs/java-chatgpt-api-1.0.0.jar extract --destination build/libs/extracted

docker-compose build

docker-compose up -d

docker-compose logs -f java-chatgpt-api