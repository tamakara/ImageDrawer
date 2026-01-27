import asyncio

from fastapi import APIRouter

from config.settings import settings
from schemas.tag import ImageTagResponse, ImageTagRequest, TagData
from workflows.image_tagging import process_single_image

router = APIRouter()
_semaphore = asyncio.Semaphore(1)  # 限制并发访问


@router.post("/tag", response_model=ImageTagResponse)
async def tag_image(request: ImageTagRequest) -> ImageTagResponse:
    """
    接收前端请求，处理单张图片，返回标签结果
    """
    try:
        async with _semaphore:
            image_file = settings.data_dir / request.image_path
            result = process_single_image(
                model_path=settings.onnx_model_path,
                metadata=settings.onnx_model_metadata,
                image_path=image_file,
                threshold=request.threshold,
            )

        if not result.get('success', False):
            return ImageTagResponse.fail(result.get('error', 'Unknown error'))

        # 组织返回数据
        tags_by_category = result.get('tags', {})
        data: TagData = {
            'artist': [],
            'character': [],
            'copyright': [],
            'general': [],
            'meta': [],
            'rating': [],
        }

        for category, cat_tags in tags_by_category.items():
            clean_tags = [pair[0] for pair in cat_tags]
            data[category] = clean_tags

        return ImageTagResponse.ok(data)

    except Exception as e:
        import traceback
        traceback.print_exc()
        return ImageTagResponse.fail(str(e))
