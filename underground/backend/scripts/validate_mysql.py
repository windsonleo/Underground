"""Run the packaged app against an isolated local MySQL 8.4 instance.

Uses a fresh data directory, random credentials and loopback-only ports.
Never connects to an existing MySQL service. Logs remain under target/.
Build the jar with mvn verify first. Optional --browser runs the frontend E2E suite.
"""
import argparse
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

BACKEND = Path(__file__).resolve().parents[1]
FRONTEND = BACKEND.parent / "frontend"
FLAGS = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0


def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_ready(check, process, label):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"{label} exited with {process.returncode}; inspect logs")
        try:
            if check():
                return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(0.5)
    raise RuntimeError(f"Timed out waiting for {label}")


class Client:
    def __init__(self, base):
        self.base = base
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))

    def request(self, path, method="GET", data=None, expected=200, csrf=True, form=False):
        headers = {}
        if method != "GET" and csrf:
            token = self.request("/api/auth/csrf")
            headers[token["headerName"]] = token["token"]
        body = None
        if data is not None:
            body = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded" if form else "application/json"
        req = urllib.request.Request(self.base + path, data=body, headers=headers, method=method)
        try:
            response = self.http.open(req, timeout=5)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            actual = response.status
            raw = response.read()
            assert actual == expected, f"{method} {path}: expected {expected}, got {actual}"
            return json.loads(raw) if raw and "application/json" in response.headers.get("Content-Type", "") else raw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql-bin", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--browser", action="store_true")
    args = parser.parse_args()
    if not args.java_home:
        parser.error("--java-home or JAVA_HOME is required")
    suffix = ".exe" if os.name == "nt" else ""
    mysqld = args.mysql_bin / ("mysqld" + suffix)
    mysql = args.mysql_bin / ("mysql" + suffix)
    java = args.java_home / "bin" / ("java" + suffix)
    jar = BACKEND / "target/underground-backend-0.1.0-SNAPSHOT.jar"
    for required in (mysqld, mysql, java, jar):
        if not required.is_file():
            raise RuntimeError(f"Missing {required}")
    run = BACKEND / "target" / ("mysql-validation-" + secrets.token_hex(6))
    run.mkdir(parents=True)
    data = run / "data"
    db_port, app_port = port(), port()
    while app_port == db_port:
        app_port = port()
    password, root_password = secrets.token_hex(24), secrets.token_hex(24)
    init = run / "init.sql"
    init.write_text(
        f"ALTER USER 'root'@'localhost' IDENTIFIED BY '{root_password}';\n"
        "CREATE DATABASE underground_validation CHARACTER SET utf8mb4;\n"
        f"CREATE USER 'underground'@'localhost' IDENTIFIED BY '{password}';\n"
        "GRANT ALL ON underground_validation.* TO 'underground'@'localhost';\n", encoding="utf-8")
    env = {**os.environ, "JAVA_HOME": str(args.java_home), "DB_USERNAME": "underground",
           "DB_PASSWORD": password,
           "DB_URL": f"jdbc:mysql://127.0.0.1:{db_port}/underground_validation?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectionTimeZone=UTC",
           "SESSION_COOKIE_SECURE": "false", "PORT": str(app_port), "OPENAPI_ENABLED": "false"}
    # Never inherit a test profile or an unrelated Spring configuration.
    for key in list(env):
        if key.startswith("SPRING_"):
            del env[key]
    sql_env = {**env, "MYSQL_PWD": password}
    sql_args = [str(mysql), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                f"--port={db_port}", "--user=underground", "--batch", "--skip-column-names"]

    def query(sql):
        return subprocess.run(sql_args + ["--execute", sql], env=sql_env, capture_output=True,
                              text=True, timeout=5, creationflags=FLAGS)

    mysql_process = app_process = None
    mysql_log = (run / "mysql.log").open("w", encoding="utf-8")
    app_log = (run / "app.log").open("w", encoding="utf-8")
    base = f"http://127.0.0.1:{app_port}"
    command = [str(mysqld), "--no-defaults", f"--basedir={args.mysql_bin.parent}",
               f"--datadir={data}", "--console"]

    def start_app():
        process = subprocess.Popen([str(java), "-jar", str(jar), "--server.address=127.0.0.1",
                                    "--debug=false", "--logging.level.root=INFO"],
                                   cwd=BACKEND, env=env, stdout=app_log, stderr=subprocess.STDOUT,
                                   creationflags=FLAGS)
        wait_ready(lambda: Client(base).request("/api/auth/csrf"), process, "Spring Boot")
        return process

    try:
        print(subprocess.check_output([str(mysqld), "--version"], text=True, creationflags=FLAGS).strip(), flush=True)
        subprocess.run(command + ["--initialize-insecure"], stdout=mysql_log, stderr=subprocess.STDOUT,
                       check=True, timeout=90, creationflags=FLAGS)
        mysql_process = subprocess.Popen(command + [f"--port={db_port}", "--bind-address=127.0.0.1",
            "--mysqlx=OFF", f"--init-file={init.as_posix()}"], stdout=mysql_log,
            stderr=subprocess.STDOUT, creationflags=FLAGS)
        wait_ready(lambda: query("SELECT 1").returncode == 0, mysql_process, "MySQL")
        init.unlink()
        app_process = start_app()
        client = Client(base)
        assert b"account.js" in client.request("/")
        for asset in ("app.js", "account.js", "styles.css", "account.css"):
            client.request("/" + asset)
        client.request("/api/accounts/me", expected=401)
        registration = {"email": "mysql-validation@example.org", "password": "test-password-123",
                        "role": "CLIENT", "adultConfirmed": True}
        client.request("/api/auth/register", "POST", registration, expected=403, csrf=False)
        account = client.request("/api/auth/register", "POST", registration, expected=201)
        assert account["verificationStatus"] == "PENDING"
        client.request("/api/auth/register", "POST", registration, expected=409)
        client.request("/api/auth/login", "POST", {"email": registration["email"], "password": "wrong"},
                       expected=401, form=True)
        credentials = {key: registration[key] for key in ("email", "password")}
        client.request("/api/auth/login", "POST", credentials, expected=204, form=True)
        assert client.request("/api/accounts/me")["id"] == account["id"]
        client.request("/api/profiles/me", expected=404)
        profile = {"displayName": "João — validação", "bio": "Cultura, café e música 🎵"}
        saved = client.request("/api/profiles/me", "PUT", profile)
        assert saved["displayName"] == profile["displayName"] and not saved["eligibleForMatching"]
        assert client.request("/api/profiles/me")["bio"] == profile["bio"]
        client.request("/api/profiles/me", "PUT", profile)  # unchanged update must remain valid
        client.request("/api/profiles/" + account["id"], expected=403)
        client.request("/api/accounts/" + account["id"], expected=403)
        client.request("/api/auth/logout", "POST", {}, expected=204)
        client.request("/api/accounts/me", expected=401)
        result = query("SELECT version FROM underground_validation.flyway_schema_history WHERE success=1")
        assert result.returncode == 0 and result.stdout.strip() == "1"
        print("PASS: MySQL migration, UTF-8 persistence, registration, login, profile, CSRF and logout", flush=True)
        app_process.terminate()
        app_process.wait(timeout=30)
        app_process = start_app()
        client = Client(base)
        client.request("/api/auth/login", "POST", credentials, expected=204, form=True)
        assert client.request("/api/profiles/me")["bio"] == profile["bio"]
        print("PASS: restart preserves profile and revalidates Flyway migration", flush=True)
        if args.browser:
            npm = shutil.which("npm.cmd" if os.name == "nt" else "npm")
            if not npm:
                raise RuntimeError("npm is required for --browser")
            subprocess.run([npm, "run", "test:e2e"], cwd=FRONTEND,
                           env={**env, "E2E_BASE_URL": base}, check=True, creationflags=FLAGS)
        print("VALIDATION SUCCESS", flush=True)
    finally:
        if app_process and app_process.poll() is None:
            app_process.terminate()
            app_process.wait(timeout=30)
        if mysql_process and mysql_process.poll() is None:
            subprocess.run([str(mysql), "--no-defaults", "--protocol=TCP", "--host=127.0.0.1",
                            f"--port={db_port}", "--user=root", "--execute=SHUTDOWN"],
                           env={**env, "MYSQL_PWD": root_password}, capture_output=True,
                           timeout=10, creationflags=FLAGS)
            try:
                mysql_process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                mysql_process.terminate()
                mysql_process.wait(timeout=10)
        if init.exists():
            init.unlink()
        mysql_log.close()
        app_log.close()
        print(f"Logs and isolated test data: {run}", flush=True)


if __name__ == "__main__":
    main()
