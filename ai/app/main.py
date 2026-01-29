import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api import tag_image, query_parse
from config.settings import settings
from services.tag_matcher import TagMatcher
from services.camie_tagger import CamieTagger


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not settings.data_dir:
        print("错误: 未指定数据目录。请设置 DATA_DIR。")
        sys.exit(1)

    try:
        settings.tagger = CamieTagger(
            device="cpu",
            cache_dir=settings.model_dir
        )

        model_dir = settings.model_dir
        index_dir = settings.index_dir
        official_tags = settings.tagger.tag_to_category.keys()

        settings.matcher = TagMatcher(
            index_dir=index_dir,
            valid_tags=official_tags,
            cache_dir=model_dir
        )
    except Exception as e:
        print(f"模型加载失败: {e}")
        sys.exit(1)

    yield

    print("正在关闭 AI 服务...")

app = FastAPI(
    title="BaKaBooru AI Service",
    lifespan=lifespan
)

app.include_router(tag_image.router, tags=["Image Tagging"])
app.include_router(query_parse.router, tags=["Query Parsing"])


@app.get("/health")
def health():
    return {"status": "ok"}
