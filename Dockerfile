FROM openjdk:17-alpine
VOLUME /tmp
COPY build/libs/chatgptapi-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]