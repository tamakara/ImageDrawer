import subprocess
import sys
from pathlib import Path
import threading
import time
import os
import argparse

# ---------------------------------
# 参数解析
# ---------------------------------
parser = argparse.ArgumentParser(description="BaKaBooru Launcher")
parser.add_argument("--web-host", default="0.0.0.0", help="Web service host")
parser.add_argument("--web-port", default="8080", help="Web service port")
parser.add_argument("--tagger-host", default="0.0.0.0", help="Tagger service host")
parser.add_argument("--tagger-port", default="8081", help="Tagger service port")
args = parser.parse_args()

# ---------------------------------
# 基础路径
# ---------------------------------
BASE_DIR = Path(sys._MEIPASS) if getattr(sys, 'frozen', False) else Path(sys.executable).parent.resolve()
DATA_DIR = Path(sys.executable).parent / "data"
LOG_DIR = Path(sys.executable).parent / "logs"

DATA_DIR.mkdir(exist_ok=True)
LOG_DIR.mkdir(exist_ok=True)

# -------------------------
# 捕获并打印日志
# -------------------------
def stream_logs(prefix, proc, log_file):
    """把子进程 stdout/stderr 输出到控制台和日志文件"""
    with open(log_file, "a", encoding="utf-8") as f:
        for line in proc.stdout:
            line = line.rstrip()
            print(f"[{prefix}] {line}")
            print(line, file=f)


# ---------------------------------
# 启动 Tagger 服务器
# ---------------------------------
tagger_exe = BASE_DIR / "tagger" / "tagger.exe"
if not tagger_exe.exists():
    raise FileNotFoundError(f"找不到 tagger.exe: {tagger_exe}")

tagger_proc = subprocess.Popen(
    [
        str(tagger_exe),
        f"--data_dir={str(DATA_DIR)}",
        f"--host={args.tagger_host}",
        f"--port={args.tagger_port}",
    ],
    cwd=str(tagger_exe.parent),
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
)

threading.Thread(
    target=stream_logs,
    args=("TAGGER", tagger_proc, LOG_DIR / "tagger.log"),
    daemon=True
).start()

print("✅ Tagger 服务器运行中...")

# ---------------------------------
# 启动 Web 服务器
# ---------------------------------
web_exe = BASE_DIR / "web" / "web.exe"
if not web_exe.exists():
    raise FileNotFoundError(f"找不到 web.exe: {web_exe}")

web_proc = subprocess.Popen(
    [
        str(web_exe),
        f'--server.address={args.web_host}',
        f'--server.port={args.web_port}',
        f'--app.data-dir={str(DATA_DIR)}',
        f'--app.tagger.url=http://{args.tagger_host}:{args.tagger_port}/tag',
    ],
    cwd=str(web_exe.parent),
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
)

threading.Thread(
    target=stream_logs,
    args=("WEB", web_proc, LOG_DIR / "web.log"),
    daemon=True
).start()

print("✅ Web 服务器运行中...")

# ---------------------------------
# 守护
# ---------------------------------
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("🛑 正在关闭服务...")
    tagger_proc.terminate()
    web_proc.terminate()
