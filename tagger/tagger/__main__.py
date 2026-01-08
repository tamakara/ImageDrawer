import uvicorn
import argparse
import multiprocessing
import os
import sys

def main():
    # PyInstaller 多进程支持
    multiprocessing.freeze_support()

    parser = argparse.ArgumentParser(description="Tagger 服务启动器")
    parser.add_argument("--data_dir", type=str, required=True, help="数据目录路径")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="主机地址")
    parser.add_argument("--port", type=int, default=8000, help="端口")

    # 解析参数
    args, _ = parser.parse_known_args()

    print(f"启动 Tagger 服务: {args.host}:{args.port}")

    # 设置环境变量，供 tagger.main 模块使用，避免该模块重复解析参数
    os.environ["TAGGER_DATA_DIR"] = args.data_dir

    # 延迟导入 app 对象。
    # 这样可以确保环境变量已经设置，且符合包的导入逻辑。
    try:
        from .main import app
    except ImportError:
        # 如果直接运行此脚本而非通过包运行 (python tagger/__main__.py)
        # 则尝试把父目录加入路径 (但不建议这样做，建议使用 run_app.py 或 python -m tagger)
        sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        from tagger.main import app

    # 禁用 reload，因为在 exe 环境或生产环境中不需要
    uvicorn.run(app, host=args.host, port=args.port, log_level="info")

if __name__ == "__main__":
    main()

