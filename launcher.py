import subprocess
import sys
import os
from pathlib import Path
import time

# -------------------------
# 路径设置
# -------------------------
if getattr(sys, "frozen", False):
    BASE_DIR = Path(sys._MEIPASS)  # exe 内部
else:
    BASE_DIR = Path(__file__).parent

DATA_DIR = Path(sys.executable).parent / "data"  # exe 同级

# -------------------------
# Tagger 服务器
# -------------------------
tagger_dir = BASE_DIR / "tagger"
tagger_cmd = [
    sys.executable,
    str(tagger_dir / "main.py"),
    "--data_dir", str(DATA_DIR),
]
tagger_proc = subprocess.Popen(tagger_cmd, cwd=tagger_dir)
print("Tagger 服务器运行中...")

# -------------------------
# Web 服务器
# -------------------------
web_jar = BASE_DIR / "backend/target/backend-0.0.1-SNAPSHOT.jar"
web_cmd = [
    "java",
    f"-Dapp.data-dir={DATA_DIR}",  # JVM 系统属性
    "-jar",
    str(web_jar)
]
web_proc = subprocess.Popen(web_cmd, cwd=web_jar.parent)
print("Web 服务器运行中...")

try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("结束所有服务器...")
    fastapi_proc.terminate()
    springboot_proc.terminate()
