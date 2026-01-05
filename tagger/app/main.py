import sys
from contextlib import asynccontextmanager
from fastapi import FastAPI, Body

from app.config import IMAGE_DIR
from app.dto import TagData, TaggerResponse, TaggerRequest
from app.inference import process_single_image
from app.loader import load_model_and_metadata

global status
global model_path
global metadata


@asynccontextmanager
async def lifespan(app: FastAPI):
    global status
    global model_path
    global metadata

    status = "unavailable"

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
async def tag_image(request: TaggerRequest) -> TaggerResponse:
    try:
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

        data = TagData()
        data.artist = [pair[0] for pair in tags_by_category['artist']]
        data.character = [pair[0] for pair in tags_by_category['character']]
        data.copyright = [pair[0] for pair in tags_by_category['copyright']]
        data.general = [pair[0] for pair in tags_by_category['general']]
        data.meta = [pair[0] for pair in tags_by_category['meta']]
        data.rating = [tags_by_category['rating'][0][0]]

        return TaggerResponse.ok(data)
    except Exception as e:
        return TaggerResponse.fail(str(e))
