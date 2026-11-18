# tictactest as a Web App: Concept for IaC and Deployment Training

> Status: draft / working notes. Not committed on purpose.
> Branch: `rampup/tictactest-as-webapp`. Date: 2026-11-03

## 1. Goal

Turn the existing tictactest project into something students can **deploy as a web application**. This gives them a realistic object for practising **Infrastructure as Code (Terraform, optionally Ansible)** and progressively more complex deployments on **AWS Academy Learner Lab**.

Constraint: **reuse the `.jar` built by this project** instead of writing a new app.

## 2. Decisions so far

| # | Decision | Source |
|---|----------|--------|
| D1 | **Iteration 1:** wrap the *unchanged* jar in a **web terminal** | user |
| D2 | **Iteration 2:** use the jar as a **library** from a separate, tiny web module | user |
| D3 | Target platform is **AWS Academy Learner Lab** | user |
| D4 | Compute is primarily a **single EC2 instance** | user |
| D5 | **Persistence** optional, preferably **S3** ("or similar") | user |
| D6 | **Lambda variant** is an option: client keeps state, server only offers `isWin` / "computer move" (optionally preinitialised state) | user |
| D7 | IaC tool: **OpenTofu preferred**, Terraform also acceptable (same HCL). Optionally **Ansible** for configuration | user |
| D8 | Students work in their **own forks** of this repo, each with their own Learner Lab account | user |
| D9 | The web module lives **in this repo** as a Gradle subproject | user |
| D10 | **No HTTPS** needed. Plain HTTP (and `ws://` for the web terminal) is fine | user |
| D11 | Students already have a **smarter player** (`TicTacToePlayer` implementation). The web variants should be able to use it instead of `GreedyPlayer` | user |

## 3. Findings: current state of the project

### 3.1 The application

- Java 25 (Gradle toolchain), Gradle 9.7, no runtime dependencies. The jar is built with `./gradlew jar` and ends up at `build/libs/450-tictactest-mvk.jar`. Its `Main-Class` is `ch.bbw.m450.tictactoe.TicTacToeMain`.
- **The app is purely console-interactive:**
  - `HumanPlayer` reads moves with `Scanner(System.in).nextLine()`.
  - Output goes to `System.out` with **ANSI escape codes** (bold, grey). These look right in a real terminal or xterm.js and wrong in a plain HTML `<pre>`.
  - `main()` always plays Human (X, starts) vs. `GreedyPlayer` (O).
  - The game ends by printing `...and the winner is: CROSS|CIRCLE` or `it's a draw!`. Invalid input causes an uncaught exception (`NumberFormatException` or `IllegalStateException`) and the JVM exits.
- **What can be reused as a library** (all `public`, no I/O):
  - `TicTacToeMain.isWin(Stone[] board, Stone color)`: static and pure.
  - `GreedyPlayer.play(Stone[] board, Stone color)`: pure and deterministic ("top-left-most free field").
  - `TicTacToePlayer.Stone` enum and `TicTacToeMain.BOARD_SIZE`.
  - `TicTacToeMain.toString(Stone[])` exists but produces ANSI output, so it is not useful for HTML.
- **What cannot be reused for the web:** `TicTacToeMain.play(...)`. It is a blocking loop that owns the game state and prints to stdout. A web app needs a request/response model where state lives in the client, in the server session or in storage.
- `GreedyPlayer` is **deterministic**. For a given sequence of human moves, the whole game is reproducible. This matters for stateless designs and for "replay" based persistence.

### 3.2 Existing CI and infrastructure

- GitHub Actions: `ci.yml` builds and tests in the devcontainer image (`ghcr.io/bernedom/tictactest`). `devcontainer.yml` builds and pushes that image to GHCR. **No workflow publishes the jar yet** (no release or artifact upload). Something has to get the jar to the EC2 instance or to Lambda.
- `infra/` (untracked) contains leftover **OpenTofu** state (`registry.opentofu.org/hashicorp/aws 5.100.0`, tofu 1.13.2). There are **no `.tf` files left**. The backup state shows a previous experiment:
  - `aws_instance.app`: `t3.micro`, Amazon Linux 2023, `us-east-1b`, key pair `vockey` (the Learner Lab default key), tag `Name=tictactest`
  - `aws_security_group.app` (`tictactest-sg`): ingress **SSH only**, from a single `/32`
  - Data sources: `aws_ami.al2023`, `aws_vpc.default`
  - Current `terraform.tfstate` is empty, so the resources were destroyed.
- ⚠️ `infra/` holds `terraform.tfstate*` and `.terraform/`. Add these to `.gitignore` before anything in `infra/` gets committed.
- The old state came from **OpenTofu**, which matches D7. The examples in this doc use `tofu`. Everything works the same with `terraform` as long as a student doesn't switch tools within one state: the lock file and provider registry differ (`registry.opentofu.org` vs `registry.terraform.io`). Because each student has their own fork (D8), allowing either tool is low-risk.
- `settings.gradle` is **empty**, so the root project name, and with it the jar name `450-tictactest-mvk.jar`, comes from the **checkout directory name**. In student forks it will differ. Set `rootProject.name = 'tictactest'` when adding the `web` subproject (D9) so the IaC can rely on a fixed jar path.

### 3.4 The students' smarter player (D11)

- It implements `TicTacToePlayer`, so all web variants can use it through the interface without knowing the class.
- **Iteration 1 (web terminal)** runs `main()`, which hardcodes `new GreedyPlayer()`. If a student wired their player into `main()`, the web terminal uses it automatically. Otherwise, a small, optional change: let `main()` choose the opponent from a system property or env var (e.g. `-Dplayer=...`), defaulting to `GreedyPlayer`.
- **Iterations 2 and 3** pick the player via configuration: env var `TICTACTEST_PLAYER=<fully qualified class name>`, loaded with `Class.forName(...).getDeclaredConstructor().newInstance()`, defaulting to `GreedyPlayer`. Students can then switch the opponent through IaC (systemd `Environment=` or the Lambda `environment {}` block) without a code change. This is a nice "configuration vs. code" example.
- Caveats:
  - A smarter player may be **non-deterministic** (random tie-breaks) or **slow** (minimax). This affects Lambda timeout and memory and breaks replay-based persistence.
  - A player with **internal state** (e.g. learning across games) is a natural use case for S3 persistence and for "preinitialised" Lambda state.

### 3.3 AWS Academy Learner Lab constraints (relevant for the design)

| Constraint | Consequence for the design |
|------------|----------------------------|
| No custom IAM roles or policies | Use the existing **`LabRole`** and **`LabInstanceProfile`** via `data "aws_iam_role"` / `data "aws_iam_instance_profile"`. Never `resource "aws_iam_role"`. |
| Session-based, temporary credentials (~4 h); `~/.aws/credentials` changes every session | Students re-copy credentials each session. Terraform state must survive this, so keep it local or use an S3 backend. |
| EC2 instances are **stopped** when the session ends and restarted with the next one | Public IP changes unless you use an **Elastic IP**. The app must start on boot (systemd). In-memory state is lost, which is a good motivation for S3 persistence. |
| Regions limited (typically `us-east-1`, `us-west-2`) | Hard-code or validate `region` in variables. |
| Limited instance types and budget | `t3.micro` / `t3.small` is enough. JVM-per-session (iteration 1) limits concurrent players. |
| No domain or ACM certificate in practice | Plain HTTP on the public IP/DNS (D10: HTTPS not needed). |

*(Verify the exact service list in the current Learner Lab readme. It changes from time to time.)*

## 4. Solution approaches

### 4.1 Iteration 1: web terminal around the unchanged jar (D1)

```
Browser (xterm.js) ──WebSocket──► ttyd :7681 ──spawns per connection──► java -jar tictactest.jar
                                    (optionally behind nginx :80)
```

**How it works:** [ttyd](https://github.com/tsl0922/ttyd) serves a web page with xterm.js. For **every browser connection** it starts the configured command in a pseudo-terminal and streams stdin/stdout over a WebSocket. ANSI colours render correctly.

```bash
ttyd -W -p 7681 java -jar /opt/tictactest/tictactest.jar
```

- `-W` / `--writable` is **required**. Without it, ttyd is read-only and the player cannot type.
- Run **the jar directly as the command, never a shell** (`bash`, `login`). Otherwise the web page becomes a public shell on the instance.
- When the game ends the JVM exits and ttyd closes the session. The browser shows "reconnect", which starts a new game.

**Alternatives to ttyd:**

| Tool | Notes |
|------|-------|
| ttyd | Static binary (GitHub release), full TTY, xterm.js included. **Recommended.** |
| GoTTY | Similar but less maintained |
| websocketd | Pipes **lines** of stdin/stdout over WebSocket, no TTY. Needs a small custom HTML/JS page. ANSI codes need stripping or interpretation. Interesting for teaching the "Unix process as web service" idea. |
| Docker + ttyd image | Run `tsl0922/ttyd` with the jar mounted. Introduces containers on EC2. |

**Pros:** zero code change; works with the jar as-is; a fast first success for students.
**Cons:** one JVM per player (~50–100 MB RAM each, so only a handful of parallel games on `t3.micro`); stateful WebSocket connections (relevant later for load balancers); no "real" HTTP API.

**Persistence in iteration 1 (optional, keeps the jar unchanged):**
A wrapper script is used as the ttyd command instead of `java -jar` directly:

```bash
#!/usr/bin/env bash
# /opt/tictactest/play.sh
log=$(mktemp)
java -jar /opt/tictactest/tictactest.jar | tee "$log"
result=$(tail -n 2 "$log" | grep -Eo 'winner is: [A-Z]+|draw' || echo aborted)
aws s3 cp "$log" "s3://$BUCKET/games/$(date +%Y/%m/%d)/$(uuidgen).log" \
  --metadata result="$result"
```

- Uses `LabInstanceProfile` for S3 access, so no keys on the instance.
- Caveat: piping through `tee` makes stdout a pipe instead of a TTY. That doesn't matter here because the app doesn't check `isatty`. Use `script -q -c ... "$log"` if it ever does.
- Gives students a reason for: an S3 bucket in Terraform, bucket policy and lifecycle rules, instance profile wiring, and later an "S3 → stats" exercise (Athena, or a Lambda triggered by S3 events).

### 4.2 Iteration 2: tiny web module using the jar as a library (D2)

New Gradle subproject `web/` **in this repo** (D9) that depends on the game code via `implementation project(':')`. The CLI jar itself stays unchanged. `./gradlew :web:jar` produces a second, self-contained fat jar that bundles the root project's classes.

```groovy
// settings.gradle
rootProject.name = 'tictactest'
include 'web'
```

The `/api/move` endpoint uses the **configured player** (see 3.4), not hardcoded `GreedyPlayer`.

**Recommendation:** use the **JDK-built-in `com.sun.net.httpserver.HttpServer`**, so there are no framework dependencies and the result is a single fat jar of a few KB. Serialise JSON by hand or with a tiny library. Javalin is the next step up if routing and JSON should be nicer. Spring Boot is overkill here.

**API proposal (stateless, client holds the board):**

| Method | Path | Body | Response | Library call |
|--------|------|------|----------|--------------|
| `POST` | `/api/win` | `{"board":["X",null,...], "color":"X"}` | `{"win":true}` | `TicTacToeMain.isWin` |
| `POST` | `/api/move` | `{"board":[...], "color":"O"}` | `{"position":4, "board":[...], "win":false, "draw":false}` | `GreedyPlayer.play` + `isWin` |
| `GET` | `/` | | static `index.html` + JS | |
| `GET` | `/health` | | `200 OK` | for ALB / monitoring |

The web module validates input itself: board length 9, the position is free, it is the right player's turn. The library code assumes well-behaved callers.

**Optional stateful variant (for persistence exercises):**

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/games` | creates a game, returns `id` |
| `POST` | `/api/games/{id}/moves` | human move; the server applies the computer move and stores the state |
| `GET` | `/api/games/{id}` | state |

Storage backends, in order of teaching value:
1. **In memory.** Lost on restart or Learner Lab session end. That loss is the motivation for the next step.
2. **S3**, one JSON object per game (`games/{id}.json`). Simple, cheap, and works with `LabRole`. Not transactional, so last write wins. That's acceptable for one player per game and a good discussion point.
3. **DynamoDB** as the "proper" choice for frequently updated small records. Can be compared against S3 in class.

### 4.3 Iteration 3 (optional): serverless / Lambda variant (D6)

```
Browser ──► S3 static website (index.html + JS, holds game state)
        └─► Lambda Function URL / API Gateway ──► handler ──► isWin / GreedyPlayer.play
```

- **Client keeps the state**, as you suggested. The Lambda is stateless and only answers "is this a win?" and "what does the computer play?". This maps 1:1 to the iteration 2 API, so the same frontend can be reused.
- **Runtime:** AWS Lambda supports a managed **`java25`** runtime (since Nov 2025), so the project's Java 25 toolchain fits without downgrading.
- **Handler:** a thin `web-lambda` module (or a second class in the web module) with `RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>`, depending on the tictactest jar. Technically Lambda can call a POJO method directly. But `Stone[]` / enum deserialisation is awkward, so a thin adapter is cleaner.
- **IAM:** `role = data.aws_iam_role.lab.arn` (`LabRole`).
- **Preinitialised state**, interpreted two ways:
  1. **Init phase:** static initialiser or constructor loads state, e.g. a scenario or start board, or later a smarter player's lookup table, from **S3 at cold start**. It is then reused across warm invocations.
  2. **SnapStart** (supported on `java25`): Lambda snapshots the initialised JVM and restores it, giving fast cold starts. A good exercise on "what does 'state' mean in serverless", including the pitfalls of state captured in a snapshot (random seeds, connections).
- **Frontend hosting:** S3 static website (Block Public Access must be relaxed and a bucket policy added), or serve `index.html` from the Lambda itself. CloudFront is optional. CORS must be configured on the Function URL / API Gateway if frontend and API are on different origins.
- To verify in Learner Lab: Lambda Function URLs and API Gateway HTTP APIs are available; S3 public website hosting is allowed.

### 4.4 Overview / comparison

| | It. 1: web terminal | It. 2: web module on EC2 | It. 3: Lambda + S3 |
|---|---|---|---|
| Jar change | none | none (used as lib) | none (used as lib) |
| New code | shell script, unit file | ~150 LOC Java + HTML/JS | handler + HTML/JS |
| Protocol | WebSocket (stateful) | HTTP/JSON | HTTP/JSON |
| Game state | in the JVM process | client, server memory, or S3 | client (+ S3 for init) |
| Scaling | 1 JVM per player | 1 JVM for all | per request |
| IaC focus | EC2, SG, user_data/Ansible, systemd | + S3, instance profile, EIP, (ALB) | Lambda, Function URL/API GW, S3 website, IAM via LabRole |

## 5. Deployment and IaC concepts

### 5.1 Getting the jar to the target ("artifact delivery")

This is a central teaching topic. Options, from simplest to most "real":

1. **Terraform uploads it to S3** (`aws_s3_object` with `source = "../build/libs/...jar"` and `etag = filemd5(...)`). The EC2 instance pulls it in `user_data` via `LabInstanceProfile`. The same bucket can also be used for game persistence and Lambda code.
2. **GitHub Release asset.** A new workflow builds the jar on tag and attaches it. The instance downloads it with `curl`. This needs a new workflow and is a good CI/CD bridge.
3. **Ansible `copy`** from the student's machine. Very direct, but couples the deployment to a local build.
4. **Container image** (GHCR, jar plus ttyd or JRE). Docker on EC2 pulls it. This is already partly there via the devcontainer workflow.

**Suggestion:** start with option 1, which is pure Terraform and self-contained. Introduce option 2 when linking CI and deployment.

### 5.2 OpenTofu layout (proposal)

```
infra/
  versions.tf        # terraform + aws provider pin
  providers.tf       # region (us-east-1), default_tags
  variables.tf       # my_ip, instance_type, jar_path, enable_persistence ...
  data.tf            # aws_ami (AL2023), default VPC, LabRole, LabInstanceProfile
  s3.tf              # artifact + game bucket
  ec2.tf             # instance, SG (22 from my_ip, 80/7681 from 0.0.0.0/0), EIP
  user_data.sh.tftpl # Java 25 (Corretto), ttyd, systemd unit
  lambda.tf          # (iteration 3)
  outputs.tf         # url, public_dns, bucket name, ansible inventory
```

Notes:
- **Java 25 on AL2023:** install Amazon Corretto 25 (`dnf install java-25-amazon-corretto-headless`). Verify the package name on the current AMI.
- **ttyd** is not in the AL2023 repositories, so download the static binary from GitHub Releases (pin the version and checksum).
- Use **`user_data_replace_on_change = true`** so changes to user_data recreate the instance. This is a nice "immutable infrastructure" discussion.
- **Elastic IP** so the URL survives Learner Lab session restarts.
- **State (D8: one fork and one Learner Lab account per student):** local state in the student's checkout is enough. **Never commit it.** Add `infra/.terraform/` and `infra/*.tfstate*` to `.gitignore` in the upstream repo so forks inherit it, while keeping `.terraform.lock.hcl` committed. An S3 backend is an optional stretch exercise (chicken-and-egg: the bucket must exist first). OpenTofu ≥ 1.10 and Terraform ≥ 1.10 support `use_lockfile = true`, so no DynamoDB lock table is needed.
- **Globally unique bucket names:** every student creates buckets in their own account, but S3 names are global. Use `bucket_prefix = "tictactest-"` or a `random_id` suffix.
- **CI deploy from the fork?** Learner Lab credentials expire every session, so storing them as GitHub secrets is fragile and needs updating each session. Recommendation: CI builds and publishes the jars (GitHub Release or artifact), and students run `tofu apply` locally.
- **No HTTPS (D10):** the security group opens port 80 (nginx → ttyd/web module) or the app port directly. ttyd uses `ws://`, which browsers allow on plain-HTTP pages.

### 5.3 OpenTofu + Ansible split (optional)

| Layer | Tool | Responsibility |
|-------|------|----------------|
| Provisioning | OpenTofu | VPC/SG, EC2, EIP, S3, Lambda. **Output an Ansible inventory** (`local_file` from template) |
| Configuration | Ansible | Install Corretto and ttyd, deploy the jar, systemd unit, nginx reverse proxy, app updates |

- Teaching contrast: **`user_data` (bake at boot, immutable)** vs. **Ansible (converge a running host, mutable)**. Both can be shown on the same app.
- Ansible connects with `vockey` (`labsuser.pem` from the Learner Lab "AWS Details" page). The SG only allows SSH from `var.my_ip`.
- Ansible handlers restart the service when a new jar is deployed. This is a good "zero-downtime?" discussion point that leads to ALB + ASG if wanted later.

### 5.4 Possible learning path

1. **EC2 + web terminal** via Terraform + `user_data`. Goal: game reachable in the browser.
2. **Ansible** takes over configuration. The same result, but updates without recreating the instance.
3. **S3 persistence**: game logs (iteration 1 wrapper) or game state (iteration 2) via `LabInstanceProfile`.
4. **Web module** (iteration 2) on the same infra. Swap only the artifact and systemd unit.
5. **Serverless** (iteration 3): Lambda + S3 website, same frontend, compare cost, state and operations.
6. *(stretch)* ALB + ASG with 2 instances: shows why WebSocket/ttyd sessions or in-memory state break and stateless/S3 designs don't.

## 6. Open questions

Resolved: ~~Terraform vs. OpenTofu~~ (D7), ~~shared repo vs. forks~~ (D8), ~~location of the web module~~ (D9), ~~HTTPS~~ (D10), ~~smarter player~~ (D11).

Still open:
- [ ] **Frontend:** plain HTML/JS (recommended, no build step) or something bigger?
- [ ] **Player selection in iteration 1:** do students' forks already wire their smart player into `main()`, or should upstream add the `-Dplayer=` / env var switch (3.4)? The second changes the CLI code slightly but keeps it backwards compatible.
- [ ] **Smarter player characteristics:** deterministic? Runtime per move? Internal state? This decides the Lambda timeout and memory and whether replay-based persistence works.
- [ ] **Jar distribution:** add a release workflow (build on tag, attach `tictactest.jar` and `tictactest-web.jar`)? Recommended, since forks inherit it.
- [ ] Add `infra/*.tfstate*` and `infra/.terraform/` to `.gitignore` upstream (before students sync their forks).

## 7. References

- AWS Lambda Java 25 runtime: https://aws.amazon.com/about-aws/whats-new/2025/11/aws-lambda-java-25
- Lambda SnapStart: https://docs.aws.amazon.com/lambda/latest/dg/snapstart-activate.html
- ttyd man page (`-W`/`--writable`): https://www.mankier.com/1/ttyd
- ttyd: https://github.com/tsl0922/ttyd
- websocketd: https://github.com/joewalnes/websocketd

## 8. Running the stack locally (iteration 1)

Prerequisites: `ttyd`, `java` (25), `python3`, `unzip`, and `gh` for the download. The nix flake provides ttyd.

1. Download the jar from a GitHub release into `release/`. Don't build it locally. The folder is git-ignored and must contain exactly one jar.
   ```bash
   gh release download <tag> --pattern '*.jar' --dir release
   ```
2. Start the stack:
   ```bash
   scripts/run-local.sh
   ```
   This starts two servers, both bound to localhost only:
   - **ttyd** on `:7681`. It runs `java -jar release/<jar>`: writable, at most 4 clients, never a shell.
   - **python3 http.server** on `:8000`. It serves `frontend/` plus a generated `version.txt`, staged in `build/local-web/`.
3. Open <http://localhost:8000/> and play. Enter fields as 0–8. When a game ends, press Enter in the terminal to start a new one.
4. Check the running version: `curl http://localhost:8000/version.txt`.
5. Stop with `Ctrl+C`. If one server exits, the other is stopped too.

Configuration through environment variables:

| Variable | Default | Meaning |
|----------|---------|---------|
| `JAR_DIR` | `release/` | Folder containing the released jar |
| `TTYD_PORT` | `7681` | Port of the web console |
| `WEB_PORT` | `8000` | Port of the frontend |
| `MAX_CLIENTS` | `4` | Max. concurrent games (one JVM each) |

If you change `TTYD_PORT`, also change `consoleUrl` in `frontend/config.js`. That file is the only per-environment file. On the remote host, nginx uses its own copy.
