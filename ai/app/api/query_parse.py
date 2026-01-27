# from fastapi import APIRouter
# from schemas.search import QueryParseRequest, QueryParseResponse
# from workflows.query_parsing import query_parsing_workflow
#
# router = APIRouter()
#
#
# @router.post("/parse", response_model=QueryParseResponse)
# def parse_query(request: QueryParseRequest):
#     """
#     Input: user natural language query
#     Output: structured search condition JSON
#     """
#     search_condition = query_parsing_workflow(request.query)
#     return QueryParseResponse(search_condition=search_condition)
