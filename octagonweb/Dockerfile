FROM openjdk:8-jre-slim

EXPOSE 80

ARG dir

COPY "./${dir}/build/libs/*.jar" ./*.jar
COPY ./config ./config

ENTRYPOINT ["java", "-jar", "*.jar", "./config"]