import time
from pathlib import Path
from typing import List, Optional, Tuple

from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document

# 常量配置
MODEL_REPO = "sentence-transformers/all-MiniLM-L6-v2"
FAISS_INDEX_FILE_NAME = "index.faiss"


class TagMatcher:
    def __init__(
            self,
            index_dir: Path,
            device: str = "cpu",
            cache_dir: Optional[Path] = None,
            valid_tags: Optional[List[str]] = None,
            model_name: str = MODEL_REPO
    ):
        self.index_dir = Path(index_dir)

        # 1. 初始化嵌入模型 (始终需要)
        print(f"正在加载嵌入模型: {model_name}...")
        self.embeddings = HuggingFaceEmbeddings(
            model_name=model_name,
            model_kwargs={'device': device},
            encode_kwargs={'normalize_embeddings': True},
            cache_folder=str(cache_dir) if cache_dir else None,
        )

        # 2. 索引逻辑：优先加载，失败则尝试根据传入的 tags 构建
        self.vector_store = self._init_vector_store(valid_tags)

    def _init_vector_store(self, valid_tags: Optional[List[str]]) -> FAISS:
        """初始化逻辑：文件优先"""
        # 路径检查：如果 index.faiss 存在，直接加载
        if (self.index_dir / FAISS_INDEX_FILE_NAME).exists():
            print(f"发现现有索引，正在从 '{self.index_dir}' 加载...")
            return FAISS.load_local(
                str(self.index_dir),
                self.embeddings,
                allow_dangerous_deserialization=True
            )

        # 如果没有文件但有标签列表，则构建
        if valid_tags:
            print("本地无索引，正在根据提供的标签列表构建...")
            return self.rebuild_index(valid_tags)

        raise ValueError(f"错误：'{self.index_dir}' 下无索引文件且未提供初始标签列表！")

    def rebuild_index(self, valid_tags: List[str]) -> FAISS:
        """
        生成/重建索引文件。调用此方法会强制覆盖本地旧索引。
        """
        print(f"正在构建向量索引 (共 {len(valid_tags)} 个标签)...")
        start_time = time.time()

        # 预处理：将下划线替换为空格以获得更好的语义向量
        documents = [
            Document(
                page_content=tag.replace("_", " ").replace("(", "").replace(")", ""),
                metadata={"original_tag": tag}
            ) for tag in valid_tags
        ]

        # 1. 计算向量并创建 FAISS 实例
        vector_store = FAISS.from_documents(documents, self.embeddings)

        # 2. 保存到本地
        self.index_dir.mkdir(parents=True, exist_ok=True)
        vector_store.save_local(str(self.index_dir))

        # 3. 更新当前实例引用的 vector_store
        self.vector_store = vector_store

        print(f"索引已保存至 '{self.index_dir}'，耗时: {time.time() - start_time:.2f}s")
        return vector_store

    def match(self, query: str, threshold: float = 0.6) -> Optional[Tuple[str, float]]:
        clean_q = query.replace("_", " ").replace("(", "").replace(")", "").lower()
        search_res = self.vector_store.similarity_search_with_score(clean_q, k=1)

        if search_res:
            doc, score = search_res[0]
            # 计算余弦相似度
            similarity = 1 - (score ** 2) / 2
            if similarity >= threshold:
                return doc.metadata["original_tag"], float(similarity)
        return None
