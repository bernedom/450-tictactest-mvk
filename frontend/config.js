// Environment-specific configuration. This file is the ONLY place that
// differs between environments (local, EC2/nginx, ...). Replace it per
// deployment instead of changing index.html.
window.TICTACTEST_CONFIG = {
  // Where the ttyd web console is reachable (embedded via <iframe>)
  consoleUrl: "http://localhost:7681/",
  // Where the running backend version can be fetched (plain text)
  backendVersionUrl: "version.txt",
};
