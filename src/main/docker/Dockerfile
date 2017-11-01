FROM java:8-jre-alpine
VOLUME /tmp
ADD pmsl-web-stateful-0.0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'
RUN sh -c 'mkdir /defstor'
RUN sh -c 'mkdir /insstor'
ENV JAVA_OPTS="-DdefinitionStoragePath=/defstor -DinstanceStoragePath=/insstor"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
