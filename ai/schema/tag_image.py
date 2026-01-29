from typing import Dict, List, Optional

from pydantic import BaseModel, Field

type TagData = Dict[str, List[str]]


class TagImageRequest(BaseModel):
    image_path: str = Field(...)
    threshold: float = Field(0.61, ge=0.0, le=1.0)


class TagImageResponse(BaseModel):
    success: bool
    data: TagData = None
    error: Optional[str] = None

    @classmethod
    def ok(cls, data: TagData) -> "TagImageResponse":
        return cls(
            success=True,
            data=data
        )

    @classmethod
    def fail(cls, error: str) -> "TagImageResponse":
        return cls(
            success=False,
            error=error
        )
