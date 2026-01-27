import json
import requests
from pathlib import Path
from huggingface_hub import hf_hub_download

from config.settings import settings

# 常量
MODEL_REPO = "Camais03/camie-tagger-v2"
ONNX_MODEL_FILE = "camie-tagger-v2.onnx"
METADATA_FILE = "camie-tagger-v2-metadata.json"
MODEL_DIR = settings.data_dir / "model"


def download_file_from_hf(filename: str, local_only: bool = False) -> Path | None:
    """
    从 HuggingFace Hub 下载文件，如果本地存在则使用本地缓存。
    """
    try:
        path = hf_hub_download(
            repo_id=MODEL_REPO,
            filename=filename,
            cache_dir=MODEL_DIR,
            local_files_only=local_only
        )
        print(f"找到 {filename}: {path}")
        return Path(path)
    except Exception as e:
        if local_only:
            return None
        print(f"下载 {filename} 失败，原因: {e}")
    return None


def download_onnx_with_fallback() -> Path | None:
    """
    尝试先用 HF Hub 下载 ONNX 模型，失败则直接 HTTP 下载。
    """
    onnx_path = download_file_from_hf(ONNX_MODEL_FILE, local_only=True)
    if onnx_path:
        return onnx_path

    print("本地未找到 ONNX 模型，尝试从 HF Hub 下载...")
    onnx_path = download_file_from_hf(ONNX_MODEL_FILE, local_only=False)
    if onnx_path:
        return onnx_path

    # fallback: 直接 HTTP 下载
    onnx_url = f"https://huggingface.co/{MODEL_REPO}/resolve/main/{ONNX_MODEL_FILE}"
    onnx_path = MODEL_DIR / ONNX_MODEL_FILE
    try:
        print(f"尝试直接 HTTP 下载 ONNX 模型: {onnx_url}")
        response = requests.get(onnx_url, stream=True, timeout=60)
        response.raise_for_status()
        with open(onnx_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
        print(f"直接下载成功: {onnx_path}")
        return onnx_path
    except Exception as e:
        print(f"HTTP 下载 ONNX 模型失败: {e}")
        return None


def download_metadata() -> Path | None:
    """
    下载或获取本地元数据文件
    """
    metadata_path = download_file_from_hf(METADATA_FILE, local_only=True)
    if metadata_path:
        return metadata_path

    print("本地未找到元数据，尝试从 HF Hub 下载...")
    metadata_path = download_file_from_hf(METADATA_FILE, local_only=False)
    return metadata_path


def get_model_files() -> dict | None:
    """
    获取 ONNX 模型路径和元数据路径，如果失败返回 None
    """
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    onnx_path = download_onnx_with_fallback()
    metadata_path = download_metadata()

    if not onnx_path or not metadata_path:
        print("模型文件或元数据不可用")
        return None

    return {"onnx_path": onnx_path, "metadata_path": metadata_path}


def load_model_and_metadata() -> tuple[Path | None, dict]:
    """
    下载/加载模型文件和元数据
    """
    files = get_model_files()
    if not files:
        return None, {}

    metadata = {}
    if files["metadata_path"] and files["metadata_path"].exists():
        try:
            with open(files["metadata_path"], "r", encoding="utf-8") as f:
                metadata = json.load(f)
        except Exception as e:
            print(f"加载元数据出错: {e}")

    return files["onnx_path"], metadata
