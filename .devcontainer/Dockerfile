FROM eclipse-temurin:25-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    bash \
    bash-completion \
    ca-certificates \
    curl \
    git \
    passwd \
    sed \
    && rm -rf /var/lib/apt/lists/*

RUN if id -u ubuntu >/dev/null 2>&1; then usermod -l builder ubuntu; fi
RUN if id -u builder >/dev/null 2>&1; then groupmod -n builder ubuntu; fi
RUN if id -u builder >/dev/null 2>&1; then usermod -d /home/builder -m builder; fi

# Install git shell extension
RUN curl -fsSL https://raw.githubusercontent.com/git/git/master/contrib/completion/git-prompt.sh -o /home/builder/.bash_git \
    && echo "source ~/.bash_git" >> /home/builder/.bashrc

RUN sed -Ei 's/(PS1=.*)(\\\[\\033\\[00m\\\]\\\$.*)/\1\\[\\033[01;33m\\]$(__git_ps1)\2/p' /home/builder/.bashrc \
    && chown builder:builder /home/builder/.bash_git /home/builder/.bashrc

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace