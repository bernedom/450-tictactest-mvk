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
docker buildx build . -t bernedom/tictactest:latest
```

### Build the same image from the repository root

```bash
docker buildx build -f .devcontainer/Dockerfile . -t bernedom/tictactest:latest
```

### Optional: run an interactive shell in the image

```bash
docker run --rm -it bernedom/tictactest:latest bash
```

## Push Container to GitHub Container Registry (GHCR)

### Requirements

- A GitHub Personal Access Token (PAT) with `write:packages` permission
- Your GitHub username

### 1. Log in to GHCR

```bash
echo "<YOUR_GITHUB_PAT>" | docker login ghcr.io -u <YOUR_GITHUB_USERNAME> --password-stdin
```

### 2. Tag the local image for GHCR

Replace `<OWNER>` with your GitHub user or organization name.

```bash
docker tag bernedom/tictactest:latest ghcr.io/bernedom/tictactest:latest
```

### 3. Push the image

```bash
docker push ghcr.io/bernedom/tictactest:latest
```

### Optional: also push a version tag

```bash
docker tag bernedom/tictactest:latest ghcr.io/bernedom/tictactest:v1.0.0
docker push ghcr.io/bernedom/tictactest:v1.0.0
```
