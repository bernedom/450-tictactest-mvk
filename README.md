# tictactest

A small Java Tic-Tac-Toe project built with Gradle.

- Language: Java
- Build tool: Gradle (wrapper included)
- Main class: `ch.bbw.m450.tictactoe.TicTacToeMain`

## Build Instructions (Local)

### Requirements

- Java 25 (the project uses Gradle toolchains and targets Java 25)

### Build the project

```bash
./gradlew clean build
```

### Run tests

```bash
./gradlew test
```

### Run the application

```bash
./gradlew run
```

### Build runnable JAR

```bash
./gradlew jar
```

The JAR is created in `build/libs/` and includes the `Main-Class` manifest entry.

## Docker Build Instructions (.devcontainer)

The development container image is defined in `.devcontainer/Dockerfile`.

### Build the image from the `.devcontainer` folder

```bash
cd .devcontainer
docker buildx build . -t bernedom/tictacttest:latest
```

### Build the same image from the repository root

```bash
docker buildx build -f .devcontainer/Dockerfile . -t bernedom/tictacttest:latest
```

### Optional: run an interactive shell in the image

```bash
docker run --rm -it bernedom/tictacttest:latest bash
```
