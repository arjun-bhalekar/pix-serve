FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

RUN addgroup -S pixserve \
    && adduser -S pixserve -G pixserve \
    && mkdir -p /data/pixserve \
    && chown -R pixserve:pixserve /app /data/pixserve

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV BASE_DIR_PATH=/data/pixserve

EXPOSE 8082

VOLUME ["/data/pixserve"]

USER pixserve

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
