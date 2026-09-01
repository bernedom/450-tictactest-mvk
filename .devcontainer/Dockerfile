FROM alpine:3.24

RUN apk update \
    && apk add --no-cache \
    bash \
    bash-completion \
    curl \
    git \
    gradle \
    openjdk25-jdk \
    shadow

RUN groupadd -g 1000 builder \
    && useradd -m -k /etc/skel -u 1000 -g 1000 -s /bin/bash builder

# Install git shell extension
RUN curl -L https://raw.github.com/git/git/master/contrib/completion/git-prompt.sh > /home/builder/.bash_git && echo "source ~/.bash_git" >> /home/builder/.bashrc
RUN sed -Ei 's/(PS1=.*)(\\\[\\033\\[00m\\\]\\\$.*)/\1\\[\\033[01;33m\\]$(__git_ps1)\2/p' /home/builder/.bashrc
RUN chown builder:builder /home/builder/.bash_git /home/builder/.bashrc


ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /workspace
