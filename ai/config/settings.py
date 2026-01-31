from pathlib import Path
from pydantic_settings import BaseSettings
from typing import Optional
from services.camie_tagger import CamieTagger
from services.tag_matcher import TagMatcher


class Settings(BaseSettings):
    data_dir: Path  # 数据目录路径，由环境变量 DATA_DIR 提供
    model_dir: Optional[Path] = None
    index_dir: Optional[Path] = None
    db_dir: Optional[Path] = None
    tagger: Optional[CamieTagger] = None
    matcher: Optional[TagMatcher] = None
    danbooru_tags: Optional[set[str]] = None

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # 确保 data_dir 不为 None
        if not self.data_dir:
            raise ValueError("请设置 DATA_DIR 环境变量")

        # 设置路径
        self.model_dir = self.data_dir / "model"
        self.index_dir = self.data_dir / "index"
        self.db_dir = self.data_dir / "db"

        # 自动创建目录
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.model_dir.mkdir(parents=True, exist_ok=True)
        self.db_dir.mkdir(parents=True, exist_ok=True)
        self.index_dir.mkdir(parents=True, exist_ok=True)


# 根据环境变量加载配置
settings = Settings()
