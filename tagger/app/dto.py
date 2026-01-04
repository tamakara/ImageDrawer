from typing import List, Optional, Dict, Any

from pydantic import BaseModel, Field, confloat


class TaggerConfig(BaseModel):
    threshold: float = Field(default=0.61, ge=0.0, le=1.0)
    category_thresholds: Dict[str, confloat(ge=0.0, le=1.0)] = Field(default_factory=dict)
    min_confidence: float = Field(default=0.01, ge=0.0, le=1.0)


class TagData(BaseModel):
    artist: List[str] = Field(default_factory=list)
    character: List[str] = Field(default_factory=list)
    copyright: List[str] = Field(default_factory=list)
    general: List[str] = Field(default_factory=list)
    meta: List[str] = Field(default_factory=list)
    rating: Optional[str] = None


class TaggerResponse(BaseModel):
    success: bool
    data: Any = None
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
