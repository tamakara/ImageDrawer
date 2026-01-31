from typing import Dict, List

from pydantic import Field, BaseModel


class ParsedTags(BaseModel):
    positive: Dict[str, List[str]]
    negative: Dict[str, List[str]]


class QueryParseRequest(BaseModel):
    query: str = Field(...)
    llm_url: str = Field(...)
    llm_model: str = Field(...)
    llm_api_key: str = Field(...)
