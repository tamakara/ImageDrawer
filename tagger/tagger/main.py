import argparse
import asyncio
import os
import sys
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI

from .dto import TagData, TaggerResponse, TaggerRequest
from .inference import process_single_image
from .loader import load_model_and_metadata

# 全局变量
status = "unavailable"
model_path = None
metadata = {}

DATA_DIR =  Path(os.environ.get('TAGGER_DATA_DIR'))
TEMP_DIR = DATA_DIR / "temp"
MODEL_DIR = DATA_DIR / "model"
PENDING_DIR = TEMP_DIR / "pending"

@asynccontextmanager
async def lifespan(app: FastAPI):
    global status
    global model_path
    global metadata

    if not DATA_DIR:
        print("错误: 未指定数据目录。请设置 --data_dir 参数。")
        sys.exit(1)

    # 确保目录存在
    for d in [DATA_DIR, TEMP_DIR, MODEL_DIR, PENDING_DIR]:
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
            image_file = PENDING_DIR / request.task_id
            result = process_single_image(
                model_path=model_path,
                metadata=metadata,
                image_path=image_file,
                threshold=request.threshold,
                category_thresholds=request.category_thresholds,
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

