import asyncio

from PIL import Image
from fastapi import APIRouter

from config.settings import settings
from schema.tag_image import TagImageResponse, TagImageRequest, TagData

router = APIRouter()
_semaphore = asyncio.Semaphore(1)  # 限制并发访问


@router.post("/tag_image", response_model=TagImageResponse)
async def tag_image(request: TagImageRequest) -> TagImageResponse:
    """
    接收前端请求，处理单张图片，返回标签结果
    """
    tagger = settings.tagger

    try:
        async with _semaphore:
            image_file_path = settings.data_dir / request.image_path
            result = tagger.tag(
                image=Image.open(image_file_path),
                threshold=request.threshold,
            )

        # 组织返回数据
        data: TagData = {c: [] for c in result.keys()}
        for cat, cat_tags in result.items():
            for pair in cat_tags:
                data[cat].append(pair["tag"])

        return TagImageResponse.ok(data)

    except Exception as e:
        import traceback
        traceback.print_exc()
        return TagImageResponse.fail(str(e))
