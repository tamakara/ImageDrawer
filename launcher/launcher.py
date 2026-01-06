import subprocess
import sys
from pathlib import Path
import threading
import time
import os

# ---------------------------------
# 基础路径
# ---------------------------------
BASE_DIR = Path(sys.executable).parent.resolve()
DATA_DIR = Path(sys.executable).parent / "data"
LOG_DIR = Path(sys.executable).parent / "logs"
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
    [str(tagger_exe), "--data_dir", str(DATA_DIR)],
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
web_jar = BASE_DIR / "web" / "web.jar"
if not web_jar.exists():
    raise FileNotFoundError(f"找不到 web.jar: {web_jar}")

web_proc = subprocess.Popen(
    [
        "java",
        f"-Dapp.data-dir={DATA_DIR}",
        "-jar",
        str(web_jar)
    ],
    cwd=str(web_jar.parent),
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
