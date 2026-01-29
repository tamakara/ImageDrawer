from langchain_core.output_parsers import JsonOutputParser
from langchain_openai import ChatOpenAI

from config.settings import settings
from schema.query_parse import ParsedTags
from services.query_parse_prompt import prompt


def query_parse(query: str, llm_url: str, llm_model: str, llm_api_key: str) -> str:
    model = ChatOpenAI(
        model=llm_model,
        openai_api_base=llm_url,
        openai_api_key=llm_api_key,
        temperature=0
    )

    tags_parser = JsonOutputParser(pydantic_object=ParsedTags)
    chain = prompt | model | tags_parser

    tags: ParsedTags = chain.invoke({"user_input": query})
    print(tags)

    matcher = settings.matcher

    pos_input = tags.get("positive", [])
    pos_matches = [
        matcher.match(query=tag, threshold=0.61)
        for tag in pos_input if not None
    ]
    valid_pos_tags = [match for match in pos_matches if match]
    print(valid_pos_tags)

    neg_input = tags.get("negative", [])
    neg_matches = [
        matcher.match(query=tag, threshold=0.61)
        for tag in neg_input
    ]
    valid_neg_tags = [match for match in neg_matches if match]
    print(valid_neg_tags)

    result_tags = set(valid_pos_tags) | set(['-' + tag for tag in valid_neg_tags])

    response = " ".join(result_tags)
    print(response)

    return response
