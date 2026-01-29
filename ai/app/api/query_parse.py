from fastapi import APIRouter
from schema.query_parse import QueryParseRequest
from services.query_parse import query_parse

router = APIRouter()


@router.post("/query_parse", response_model=str)
def parse_query(request: QueryParseRequest):
    return query_parse(
        query=request.query,
        llm_url=request.llm_url,
        llm_model=request.llm_model,
        llm_api_key=request.llm_api_key,
    )
