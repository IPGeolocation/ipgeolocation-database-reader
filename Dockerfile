FROM openjdk:8-jre-alpine

RUN apk add --no-cache curl

RUN mkdir -p ~/conf/db-ipgeolocation

ENV JAVA_GC_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -XX:+UseG1GC" \
    JAVA_HEAP="10G" \
    JAVA_OPTS="" \
    API_KEY="" \
    DATABASE_VERSION="" \
    DATABASE_UPDATE_INTERVAL="" \
    DATABASE_TYPE="" \
    AUTO_UPDATE=false

RUN echo "{\"apiKey\":${API_KEY},\"database\":${DATABASE_VERSION},\"updateInterval\":${DATABASE_UPDATE_INTERVAL},\"databaseType\":${DATABASE_TYPE},\"autoFetchAndUpdateDatabase\":${AUTO_UPDATE}}"
RUN echo "{\"apiKey\":${API_KEY},\"database\":${DATABASE_VERSION},\"updateInterval\":${DATABASE_UPDATE_INTERVAL},\"databaseType\":${DATABASE_TYPE},\"autoFetchAndUpdateDatabase\":${AUTO_UPDATE}}" > ~/conf/db-ipgeolocation/database-config.json

EXPOSE 8080

COPY target/ipgeolocation-database-reader-*.war /opt/ipgeolocation-database-reader.war

HEALTHCHECK --start-period=5m CMD curl -f http://localhost:8080/ipGeo?ip=1.0.0.0

CMD exec java "-Xmx${JAVA_HEAP}" ${JAVA_GC_OPTS} ${JAVA_OPTS} -jar /opt/ipgeolocation-database-reader.war