import argparse
import asyncio
import os
import sys
from contextlib import asynccontextmanager
from pathlib import Path

import uvicorn
from fastapi import FastAPI

from dto import TagData, TaggerResponse, TaggerRequest
from inference import process_single_image
from loader import load_model_and_metadata

# 数据目录
parser = argparse.ArgumentParser()
parser.add_argument("--data_dir", type=str, required=True)
args = parser.parse_args()

DATA_DIR = Path(args.data_dir)
IMAGE_DIR = DATA_DIR / "image"
TEMP_DIR = DATA_DIR / "temp"
MODEL_DIR = DATA_DIR / "model"

global status
global model_path
global metadata


@asynccontextmanager
async def lifespan(app: FastAPI):
    global status
    global model_path
    global metadata

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
        # 限制并发访问
        semaphore = asyncio.Semaphore(1)

        async with semaphore:
            result = process_single_image(
                model_path=model_path,
                metadata=metadata,
                image_path=IMAGE_DIR / request.image_hash,
                threshold=request.threshold,
                category_thresholds=request.category_thresholds,
                min_confidence=request.min_confidence,
            )

        if not result['success']:
            print(f"处理图像时出错: {result.get('error')}")

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

        for category in data.keys():
            data[category] = [pair[0] for pair in tags_by_category[category]]

        return TaggerResponse.ok(data)
    except Exception as e:
        return TaggerResponse.fail(str(e))


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
