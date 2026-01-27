from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    全局配置 Settings
    - data_dir: 数据目录路径
    """
    data_dir: Path  # 数据目录路径，由环境变量 DATA_DIR 提供
    onnx_model_path: Path | None = None  # ONNX 模型路径
    onnx_model_metadata: dict | None = None  # ONNX 模型元数据

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # 确保 data_dir 不为 None
        if not self.data_dir:
            raise ValueError("请设置 DATA_DIR 环境变量")

        # 自动创建目录
        self.data_dir.mkdir(parents=True, exist_ok=True)


# 实例化 Settings 时，会根据环境变量加载配置
settings = Settings()
