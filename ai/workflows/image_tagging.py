import os
import time
import traceback

import numpy as np
import onnxruntime as ort
import torchvision.transforms as transforms
from PIL import Image


def preprocess_image(image_path: str, image_size: int = 512) -> np.ndarray:
    """
    预处理图像：
    - 调整大小保持纵横比
    - 填充至 image_size x image_size
    - 转换为 Tensor 并归一化
    """
    if not os.path.exists(image_path):
        raise FileNotFoundError(f"未找到图像文件: {image_path}")

    transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize(
            mean=[0.485, 0.456, 0.406],
            std=[0.229, 0.224, 0.225]
        )
    ])

    with Image.open(image_path) as img:
        if img.mode in ('RGBA', 'P'):
            img = img.convert('RGB')

        width, height = img.size
        aspect_ratio = width / height

        if aspect_ratio > 1:
            new_width = image_size
            new_height = int(new_width / aspect_ratio)
        else:
            new_height = image_size
            new_width = int(new_height * aspect_ratio)

        img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

        # 填充背景
        pad_color = (124, 116, 104)
        new_image = Image.new('RGB', (image_size, image_size), pad_color)
        paste_x = (image_size - new_width) // 2
        paste_y = (image_size - new_height) // 2
        new_image.paste(img, (paste_x, paste_y))

        img_tensor = transform(new_image)
        return img_tensor.numpy()


class ONNXImageTagger:
    """
    ONNX 图像标注器
    - 单例模式可通过 process_single_image 缓存
    """

    def __init__(self, model_path: str, metadata: dict):
        self.model_path = model_path

        providers = []
        available = ort.get_available_providers()
        if "CUDAExecutionProvider" in available:
            providers.append("CUDAExecutionProvider")
        providers.append("CPUExecutionProvider")

        self.session = ort.InferenceSession(model_path, providers=providers)

        self.metadata = metadata
        self.idx_to_tag = {}
        self.tag_to_category = {}
        self.total_tags = 0

        if 'dataset_info' in metadata:
            tag_mapping = metadata['dataset_info']['tag_mapping']
            self.idx_to_tag = tag_mapping['idx_to_tag']
            self.tag_to_category = tag_mapping['tag_to_category']
            self.total_tags = metadata['dataset_info']['total_tags']
        else:
            self.idx_to_tag = metadata.get('idx_to_tag', {})
            self.tag_to_category = metadata.get('tag_to_category', {})
            self.total_tags = metadata.get('total_tags', len(self.idx_to_tag))

        self.input_name = self.session.get_inputs()[0].name
        print(f"ONNX 模型加载完成，输入: {self.input_name}, 总标签: {self.total_tags}")

    def predict_batch(self, image_arrays: list[np.ndarray], threshold: float = 0.61,
                      category_thresholds: dict = None) -> list[dict]:
        """批量推理"""
        batch_input = np.stack(image_arrays)
        start_time = time.time()
        outputs = self.session.run(None, {self.input_name: batch_input})

        main_logits = outputs[1] if len(outputs) > 1 else outputs[0]
        main_probs = 1.0 / (1.0 + np.exp(-main_logits))

        batch_results = []

        for i in range(main_probs.shape[0]):
            probs = main_probs[i]
            all_probs = {}

            for idx, prob in enumerate(probs):
                idx_str = str(idx)
                tag_name = self.idx_to_tag.get(idx_str, f"unknown-{idx}")
                category = self.tag_to_category.get(tag_name, "general")
                all_probs.setdefault(category, []).append((tag_name, float(prob)))

            # 排序并筛选
            tags = {}
            for category, cat_tags in all_probs.items():
                cat_threshold = category_thresholds.get(category, threshold) if category_thresholds else threshold
                tags[category] = [(t, p) for t, p in sorted(cat_tags, key=lambda x: x[1], reverse=True) if
                                  p >= cat_threshold]

            all_tags = [t for cat in tags.values() for t, _ in cat]

            batch_results.append({
                'tags': tags,
                'all_probs': all_probs,
                'all_tags': all_tags,
                'success': True,
                'inference_time': time.time() - start_time
            })

        return batch_results


def process_single_image(model_path, metadata, image_path, threshold=0.61, category_thresholds=None):
    """
    处理单张图像，缓存 ONNXImageTagger
    """
    try:
        if category_thresholds is None:
            category_thresholds = {}

        # 缓存 tagger 实例
        if not hasattr(process_single_image, "tagger"):
            process_single_image.tagger = ONNXImageTagger(model_path, metadata)
        tagger = process_single_image.tagger

        img_array = preprocess_image(image_path)
        results = tagger.predict_batch([img_array], threshold, category_thresholds)

        if results:
            return results[0]
        return {'success': False, 'error': '处理图像失败', 'all_tags': [], 'tags': {}}

    except Exception as e:
        traceback.print_exc()
        return {'success': False, 'error': str(e), 'all_tags': [], 'tags': {}}
