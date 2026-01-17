from typing import List, Optional, Dict

from pydantic import BaseModel, Field


class TaggerRequest(BaseModel):
    task_id: str = Field(...)
    threshold: float = Field(0.61, ge=0.0, le=1.0)
    category_thresholds: dict[str, float] = Field(default_factory=dict)


type TagData = Dict[str, List[str]]


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

