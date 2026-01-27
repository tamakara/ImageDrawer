from typing import Dict, List, Optional

from pydantic import BaseModel, Field

type TagData = Dict[str, List[str]]


class ImageTagRequest(BaseModel):
    image_path: str = Field(...)
    threshold: float = Field(0.61, ge=0.0, le=1.0)


class ImageTagResponse(BaseModel):
    success: bool
    data: TagData = None
    error: Optional[str] = None

    @classmethod
    def ok(cls, data: TagData) -> "ImageTagResponse":
        return cls(
            success=True,
            data=data
        )

    @classmethod
    def fail(cls, error: str) -> "ImageTagResponse":
        return cls(
            success=False,
            error=error
        )
