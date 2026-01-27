import argparse
import multiprocessing
import os
import sys

import uvicorn

# -------------------------
# PyInstaller 多进程支持
# -------------------------
multiprocessing.freeze_support()

# -------------------------
# 确保 ai 目录在 sys.path
# -------------------------
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

# -------------------------
# 解析命令行参数
# -------------------------
parser = argparse.ArgumentParser(description="AI 服务启动器")
parser.add_argument("--data_dir", type=str, required=True, help="数据目录路径")
parser.add_argument("--host", type=str, default="0.0.0.0", help="主机地址")
parser.add_argument("--port", type=int, default=8081, help="端口")
args, _ = parser.parse_known_args()

# -------------------------
# 设置环境变量
# -------------------------
os.environ["DATA_DIR"] = args.data_dir

print(f"启动 AI 服务: {args.host}:{args.port}")
print(f"数据目录: {args.data_dir}")

# -------------------------
# 启动 Uvicorn
# -------------------------
if __name__ == "__main__":
    from app.main import app

    uvicorn.run(app, host=args.host, port=args.port, log_level="info")
