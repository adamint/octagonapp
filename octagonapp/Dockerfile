FROM openjdk:8-jre-slim

ARG dir

COPY "./${dir}/build/libs/*.jar" ./*.jar
COPY ./config ./config

ENTRYPOINT ["java", "-jar", "*.jar", "./config"]