import os
from pathlib import Path

# Base directory of the project
BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Cache directories
DATA_DIR = BASE_DIR / "data"

IMAGE_DIR = DATA_DIR / "image"
TEMP_DIR = DATA_DIR / "temp"
MODEL_DIR = DATA_DIR / "model"

# Ensure directories exist
os.makedirs(IMAGE_DIR, exist_ok=True)
os.makedirs(MODEL_DIR, exist_ok=True)

