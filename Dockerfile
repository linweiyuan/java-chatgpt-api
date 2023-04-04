FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY build/libs/java-chatgpt-api-1.0.0.jar app.jar
CMD ["java", "-Xms64m", "-Xmx64m", "-jar", "/app.jar"]