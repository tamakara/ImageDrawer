import argparse
import os
import shutil
import sys
import tempfile
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Form, File, UploadFile

from app.dto import TaggerConfig, TagData, TaggerResponse
from app.inference import process_single_image
from app.loader import load_model_and_metadata
from fastapi import Depends
import json

global status
global model_path
global metadata

global BASE_DIR
global CACHE_DIR
global IMAGE_CACHE_DIR
global MODEL_CACHE_DIR


def parse_config(config: str = Form("{}")) -> TaggerConfig:
    return TaggerConfig(**json.loads(config))


@asynccontextmanager
async def lifespan(app: FastAPI):
    global status
    global model_path
    global metadata

    global BASE_DIR
    global CACHE_DIR
    global IMAGE_CACHE_DIR
    global MODEL_CACHE_DIR

    status = "unavailable"

    BASE_DIR = Path(__file__).resolve().parent.parent
    CACHE_DIR = BASE_DIR / "cache"
    IMAGE_CACHE_DIR = BASE_DIR / "cache" / "image"
    MODEL_CACHE_DIR = BASE_DIR / "cache" / "model"
    os.makedirs(CACHE_DIR, exist_ok=True)
    os.makedirs(IMAGE_CACHE_DIR, exist_ok=True)
    os.makedirs(MODEL_CACHE_DIR, exist_ok=True)

    print("正在加载模型...")
    model_path, metadata = load_model_and_metadata()

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
async def tag_image(
        image: UploadFile = File(...),
        config: TaggerConfig = Depends(parse_config)
):
    try:
        # 创建临时文件
        try:
            with tempfile.NamedTemporaryFile(
                    dir=IMAGE_CACHE_DIR,
                    suffix=".jpg",
                    delete=False
            ) as tmp:
                shutil.copyfileobj(image.file, tmp)
                image_path = tmp.name

            result = process_single_image(
                image_path=image_path,
                model_path=model_path,
                metadata=metadata,
                threshold=config.threshold,
                category_thresholds=config.category_thresholds,
                min_confidence=config.min_confidence,
            )

        finally:
            if image_path and os.path.exists(image_path):
                os.remove(image_path)

        if not result['success']:
            print(f"处理图像时出错: {result.get('error')}")

        if 'inference_time' in result:
            print(f"推理耗时: {result['inference_time']:.4f} 秒")

        tags_by_category = result.get('tags', {})

        data = TagData()
        data.artist = [pair[0] for pair in tags_by_category['artist']]
        data.character = [pair[0] for pair in tags_by_category['character']]
        data.copyright = [pair[0] for pair in tags_by_category['copyright']]
        data.general = [pair[0] for pair in tags_by_category['general']]
        data.meta = [pair[0] for pair in tags_by_category['meta']]
        data.rating = tags_by_category['rating'][0][0]

        return TaggerResponse.ok(data)
    except Exception as e:
        return TaggerResponse.fail(str(e))
