FROM openjdk:11
WORKDIR /webserver
COPY . .
ENV OPTS="-Duser.timezone=GMT -Dfile.encoding=UTF-8 -Denvironment.type=production"

RUN javac -sourcepath src -d bin -classpath bin/webserver.jar src/App.java
CMD exec java $OPTS -classpath bin:bin/webserver.jar App
