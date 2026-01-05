from typing import List, Optional, Dict, Any

from pydantic import BaseModel, Field, confloat


class TaggerRequest(BaseModel):
    image_hash: str = Field(...)
    threshold: float = Field(0.61, ge=0.0, le=1.0)
    min_confidence: float = Field(0.01, ge=0.0, le=1.0)
    category_thresholds: dict[str, float] = Field(default_factory=dict)


class TagData(BaseModel):
    artist: List[str] = Field(default_factory=list)
    character: List[str] = Field(default_factory=list)
    copyright: List[str] = Field(default_factory=list)
    general: List[str] = Field(default_factory=list)
    meta: List[str] = Field(default_factory=list)
    rating: List[str] = Field(default_factory=list)


class TaggerResponse(BaseModel):
    success: bool
    data: TagData = None
    error: Optional[str] = None

    @classmethod
    def ok(cls, data: TagData) -> "TaggerResponse":
        return cls(
            success=True,
            data=data
        )

    @classmethod
    def fail(cls, error: str) -> "TaggerResponse":
        return cls(
            success=False,
            error=error
        )
