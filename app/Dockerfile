FROM amazonlinux:2.0.20190228

LABEL maintainer "uzresk"

# install packages
RUN yum -y install glibc-langpack-ja \
                   shadow-utils \
                   java-1.8.0-openjdk.x86_64 \
                   java-1.8.0-openjdk-devel.x86_64

# set locale ja_JP.UTF-8, timezone
ENV LANG=ja_JP.utf8
ENV LC_ALL ja_JP.utf8
RUN rm -f /etc/localtime
RUN ln -fs /usr/share/zoneinfo/Asia/Tokyo /etc/localtime

VOLUME /tmp

ARG JAR_FILE
ENV USER_NAME sample

RUN useradd -ms /bin/bash $USER_NAME
COPY target/app-0.0.1-SNAPSHOT.jar /app.jar
RUN chown ${USER_NAME} /app.jar

USER ${USER_NAME}
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]