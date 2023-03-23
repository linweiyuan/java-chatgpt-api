# https://spring.io/guides/topicals/spring-boot-docker/

# cd build/libs/dependency; jar -xf ../java-chatgpt-api-1.0.0.jar

#FROM eclipse-temurin:17-jre-alpine
#VOLUME /tmp
#ARG DEPENDENCY=build/libs/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","com.linweiyuan.chatgptapi.ChatgptapiApplication"]


# java -Djarmode=layertools -jar build/libs/java-chatgpt-api-1.0.0.jar extract --destination build/libs/extracted

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG EXTRACTED=build/libs/extracted
COPY ${EXTRACTED}/dependencies/ ./
COPY ${EXTRACTED}/spring-boot-loader/ ./
COPY ${EXTRACTED}/snapshot-dependencies/ ./
COPY ${EXTRACTED}/application/ ./
ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]