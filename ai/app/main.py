import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import image_tag, query_parse
from config.settings import settings
from core.model_loader import load_model_and_metadata


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not settings.data_dir:
        print("错误: 未指定数据目录。请设置 DATA_DIR。")
        sys.exit(1)

    print("正在加载模型...")
    model_path, model_metadata = load_model_and_metadata()

    if not model_path:
        print("错误: 模型不可用。")
        sys.exit(1)

    settings.onnx_model_path = model_path
    settings.onnx_model_metadata = model_metadata

    print("模型加载完成。")
    yield
    print("正在关闭 AI 服务...")


app = FastAPI(
    title="BaKaBooru AI Service",
    lifespan=lifespan
)

app.include_router(image_tag.router, tags=["Image Tagging"])
# app.include_router(query_parse.router,  tags=["Query Parsing"])


@app.get("/health")
def health():
    return {"status": "ok"}
