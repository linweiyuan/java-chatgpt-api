FROM ubuntu

RUN apt update \
    && apt install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_19.x | bash - \
    && apt update \
    && apt remove -y curl \
    && apt install -y openjdk-17-jre nodejs libgtk-3-0 libdbus-glib-1-2 \
    && apt clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY . .

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=true
ENV PLAYWRIGHT_NODEJS_PATH=/usr/bin/node
ENV CHATGPT=true

RUN ./gradlew installFirefox \
    && ./gradlew build \
    && rm -rf ~/.gradle

CMD ["java", "-jar", "build/libs/java-chatgpt-api-1.0.0.jar"]
