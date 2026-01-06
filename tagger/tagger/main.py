import argparse
import asyncio
import os
import sys
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI

# 使用相对导入，适应包结构
from .dto import TagData, TaggerResponse, TaggerRequest
from .inference import process_single_image
from .loader import load_model_and_metadata

# 全局变量
status = "unavailable"
model_path = None
metadata = {}

# 确定数据目录
# 优先使用环境变量 TAGGER_DATA_DIR，否则尝试解析命令行参数
data_dir_env = os.environ.get("TAGGER_DATA_DIR")
if data_dir_env:
    DATA_DIR = Path(data_dir_env)
else:
    # 使用 parse_known_args 避免与其他工具（如 uvicorn CLI）参数冲突
    parser = argparse.ArgumentParser(description="Tagger 服务")
    parser.add_argument("--data_dir", type=str, help="数据目录路径", default=None)
    args, _ = parser.parse_known_args()

    if args.data_dir:
        DATA_DIR = Path(args.data_dir)
    else:
        # 如果未提供，默认为当前目录下的 data 文件夹，或者抛出警告
        # 为了保持原有逻辑的必需性，如果没有提供则报错，除非只是被导入检查
        pass
        # 注意: 这里如果不设置 DATA_DIR，后续会报错。
        # 暂时设为 None，在 lifespan 中检查
        DATA_DIR = None

IMAGE_DIR = None
TEMP_DIR = None
MODEL_DIR = None

if DATA_DIR:
    IMAGE_DIR = DATA_DIR / "image"
    TEMP_DIR = DATA_DIR / "temp"
    MODEL_DIR = DATA_DIR / "model"


@asynccontextmanager
async def lifespan(app: FastAPI):
    global status
    global model_path
    global metadata

    if DATA_DIR is None:
        print("错误: 未指定数据目录。请设置 TAGGER_DATA_DIR 环境变量或提供 --data_dir 参数。")
        sys.exit(1)

    # 确保目录存在
    for d in [IMAGE_DIR, TEMP_DIR, MODEL_DIR]:
        os.makedirs(d, exist_ok=True)

    status = "unavailable"

    print("正在加载模型...")
    model_path, metadata = load_model_and_metadata(MODEL_DIR)

    if not model_path:
        print("错误: 模型不可用。")
        sys.exit(1)

    print("模型加载完成。")

    status = "available"

    yield

    print("正在关闭...")


app = FastAPI(
    title="tagger",
    version="1.0.0",
    lifespan=lifespan
)


@app.get("/health")
def health():
    return {
        "status": status,
    }


@app.post("/tag")
async def tag_image(request: TaggerRequest) -> TaggerResponse:
    try:
        if status != "available":
            return TaggerResponse.fail("服务未就绪")

        # 限制并发访问
        semaphore = asyncio.Semaphore(1)

        async with semaphore:
            image_file = IMAGE_DIR / request.image_hash
            result = process_single_image(
                model_path=model_path,
                metadata=metadata,
                image_path=image_file,
                threshold=request.threshold,
                category_thresholds=request.category_thresholds,
                min_confidence=request.min_confidence,
            )

        if not result['success']:
            print(f"处理图像时出错: {result.get('error')}")
            return TaggerResponse.fail(result.get('error', 'Unknown error'))

        if 'inference_time' in result:
            print(f"推理耗时: {result['inference_time']:.4f} 秒")

        tags_by_category = result.get('tags', {})

        data: TagData = {
            'artist': [],
            'character': [],
            'copyright': [],
            'general': [],
            'meta': [],
            'rating': [],
        }

        for category in tags_by_category: # Iterate over returned categories
            clean_tags = [pair[0] for pair in tags_by_category[category]]
            if category in data:
                 data[category] = clean_tags
            else:
                # 如果有未预定义的分类，也可以添加
                data[category] = clean_tags

        return TaggerResponse.ok(data)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return TaggerResponse.fail(str(e))


# 移除 __main__ 块，因为现在有专门的 __main__.py

