FROM alpine:3.24

RUN apk update \
    && apk add --no-cache \
    bash \
    bash-completion \
    git \
    gradle \
    openjdk25-jdk


ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /workspace
