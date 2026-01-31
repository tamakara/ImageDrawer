from langchain_core.output_parsers import PydanticOutputParser
from langchain_openai import ChatOpenAI

from config.settings import settings
from schema.query_parse import ParsedTags


def query_parse(query: str, llm_url: str, llm_model: str, llm_api_key: str) -> str:
    # 初始化模型
    model = ChatOpenAI(
        model=llm_model,
        openai_api_base=llm_url,
        openai_api_key=llm_api_key,
        temperature=0
    )

    # 关键词提取
    parser = PydanticOutputParser(pydantic_object=ParsedTags)
    from services.query_parse_prompt import query_parse_prompt as prompt
    chain = prompt | model | parser
    parsed_tags: ParsedTags = chain.invoke({"user_input": query})
    print(parsed_tags)

    # 向量匹配
    result_tags = []
    matcher = settings.matcher

    for word, tags in parsed_tags.positive.items():
        for tag in tags:
            match_tag, similarity = matcher.match(tag, threshold=0.9)
            if match_tag:
                result_tags.append(match_tag)
                break

    for word, tags in parsed_tags.negative.items():
        for tag in tags:
            match_tag, similarity = matcher.match(tag, threshold=0.9)
            if match_tag:
                result_tags.append('-' + match_tag)
                break

    print(result_tags)

    # 拼接结果
    result = " ".join(result_tags)
    return result
