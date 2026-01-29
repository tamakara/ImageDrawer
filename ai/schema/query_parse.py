from pydantic import Field, BaseModel


class ParsedTags(BaseModel):
    positive: list[str] = Field(description="List of positive tags")
    negative: list[str] = Field(description="List of negative tags")


class QueryParseRequest(BaseModel):
    query: str = Field(...)
    llm_url: str = Field(...)
    llm_model: str = Field(...)
    llm_api_key: str = Field(...)
