FROM maven:3-openjdk-18-slim as builder

WORKDIR /app

ADD . /app

RUN mvn clean install

FROM maven:3-openjdk-18-slim

COPY --from=builder /app/target/InstagramLoader-1.0-SNAPSHOT-jar-with-dependencies.jar InstagramLoader.jar
ENTRYPOINT [ "java", "-jar", "InstagramLoader.jar"]
