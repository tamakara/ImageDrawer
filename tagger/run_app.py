# tagger 启动脚本
# 用于 PyInstaller 打包入口或开发环境直接运行

import sys
import os

# 将当前目录加入 path，确保能导入 tagger 包
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from tagger.__main__ import main

if __name__ == '__main__':
    main()

