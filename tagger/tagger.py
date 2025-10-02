from flask import Flask, request, jsonify
from PIL import Image
import pandas as pd
import timm
import torch
from huggingface_hub import hf_hub_download
from timm.data import create_transform, resolve_data_config
import torch.nn.functional as F
from datetime import datetime
import os
import signal
import sys

app = Flask(__name__)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
MODEL_REPO = "SmilingWolf/wd-swinv2-tagger-v3"
ALLOWED_EXT = {'png', 'jpg', 'jpeg', 'bmp', 'gif'}

mdl = None
lbls = None
xform = None

def allowed(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXT

def pil_rgb(im):
    if im.mode not in ("RGB", "RGBA"):
        im = im.convert("RGBA") if "transparency" in im.info else im.convert("RGB")
    if im.mode == "RGBA":
        bg = Image.new("RGBA", im.size, (255, 255, 255))
        bg.alpha_composite(im)
        im = bg.convert("RGB")
    return im

def pad_square(im):
    w, h = im.size
    s = max(w, h)
    bg = Image.new("RGB", (s, s), (255, 255, 255))
    bg.paste(im, ((s - w) // 2, (s - h) // 2))
    return bg

def load_labels(repo):
    path = hf_hub_download(repo_id=repo, filename="selected_tags.csv")
    df = pd.read_csv(path, usecols=["name", "category"])
    return {
        "names": df["name"].tolist(),
        "rating": [i for i, v in enumerate(df["category"]) if v == 9],
        "general": [i for i, v in enumerate(df["category"]) if v == 0],
        "character": [i for i, v in enumerate(df["category"]) if v == 4],
    }

def get_tags(probs, labels, thresh):
    probs = list(zip(labels["names"], probs.numpy()))
    rating = max(((labels["names"][i], probs[i][1]) for i in labels["rating"]), key=lambda x: x[1])[0]

    copyright_set = set()
    character_tags = []
    general_tags = []

    for i, (name, p) in enumerate(probs):
        if p > thresh:
            if i in labels["character"]:
                character_tags.append((name, p))
                if "(" in name and ")" in name:
                    c = name[name.find("(")+1:name.find(")")]
                    if c:
                        copyright_set.add(c)
            elif i in labels["general"]:
                general_tags.append((name, p))

    copyright_tags = ["copyright:" + c for c in sorted(copyright_set)]
    character_tags = ["character:" + name for name, _ in sorted(character_tags, key=lambda x: x[1], reverse=True)]
    general_tags = [name for name, _ in sorted(general_tags, key=lambda x: x[1], reverse=True)]

    return {"rating": rating, "tags": copyright_tags + character_tags + general_tags}

def process(im, thresh=0.35):
    im = pil_rgb(im)
    im = pad_square(im)
    inp = xform(im).unsqueeze(0)[:, [2, 1, 0]]
    with torch.inference_mode():
        inp = inp.to(device)
        out = torch.sigmoid(mdl(inp))
        out = out.to("cpu")
    return get_tags(out.squeeze(0), lbls, thresh)

@app.route('/', methods=['POST'])
def tagger():
    start = datetime.now()
    if 'image' not in request.files:
        return jsonify({"error": "未提供图像文件"}), 400
    file = request.files['image']
    if not file or not allowed(file.filename):
        return jsonify({"error": "无效文件类型，仅支持 png, jpg, jpeg, bmp, gif"}), 400
    try:
        thresh = float(request.args.get('threshold', app.config.get('DEFAULT_THRESH', 0.35)))
        if not 0 <= thresh <= 1:
            raise ValueError
    except:
        return jsonify({"error": "无效阈值，必须为 0 到 1 之间的数字"}), 400
    try:
        img = Image.open(file.stream)
        res = process(img, thresh)
    except Exception as e:
        return jsonify({"error": f"处理失败: {str(e)}"}), 500
    elapsed = (datetime.now() - start).total_seconds()
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {file.filename} 处理完成，阈值 {thresh}, 耗时 {elapsed:.2f}s, 评级 {res['rating']}, 标签数 {len(res['tags'])}")
    return jsonify(res)


@app.route('/', methods=['GET'])
def health():
    return "服务运行中"

def get_input():
    while True:
        try:
            t = float(input("请输入默认阈值(0~1，默认0.35): ") or "0.35")
            if 0 <= t <= 1:
                break
        except:
            pass
        print("错误：请输入 0 到 1 之间的数字")
    while True:
        try:
            p = int(input("请输入端口号(1024~65535，默认5000): ") or "5000")
            if 1024 <= p <= 65535:
                break
        except:
            pass
        print("错误：请输入 1024 到 65535 之间的整数")
    return t, p

def ensure_hf_token():
    token = os.environ.get("HF_TOKEN")
    hf_home = os.path.expanduser("~/.huggingface")
    token_file = os.path.join(hf_home, "token")

    if token:
        return

    if os.path.exists(token_file):
        with open(token_file, "r") as f:
            token_from_file = f.read().strip()
            if token_from_file:
                os.environ["HF_TOKEN"] = token_from_file
                print(" 已从 ~/.huggingface/token 自动加载 HuggingFace Token")
                return

    print("\n未检测到 HuggingFace Token！")
    print("如果需要访问私有模型或更快下载速度，你需要登录 HuggingFace。")
    print("访问以下链接获取你的 Token：")
    print("https://huggingface.co/settings/tokens\n")
    user_token = input("请输入你的 HuggingFace Token（留空跳过）: ").strip()
    if user_token:
        os.environ["HF_TOKEN"] = user_token
        try:
            os.makedirs(hf_home, exist_ok=True)
            with open(token_file, "w") as f:
                f.write(user_token)
            print("已写入 ~/.huggingface/token")
        except Exception as e:
            print(f"无法写入本地文件: {e}")

def cleanup(signum, frame):
    print("\n到中断信号，准备退出...")
    sys.exit(0)

signal.signal(signal.SIGINT, cleanup)
signal.signal(signal.SIGTERM, cleanup)

if __name__ == "__main__":
    ensure_hf_token()
    thresh, port = get_input()
    print("加载模型...")
    mdl = timm.create_model("hf-hub:" + MODEL_REPO).eval()
    mdl.load_state_dict(timm.models.load_state_dict_from_hf(MODEL_REPO))
    if torch.cuda.device_count() > 1:
        print(f"检测到 {torch.cuda.device_count()} 张 GPU，启用 DataParallel")
        mdl = torch.nn.DataParallel(mdl)
    mdl.to(device)
    print("加载标签...")
    lbls = load_labels(MODEL_REPO)
    print("创建数据转换...")
    pretrained_cfg = mdl.module.pretrained_cfg if hasattr(mdl, "module") else mdl.pretrained_cfg
    xform = create_transform(**resolve_data_config(pretrained_cfg, model=mdl))
    print(f"推理设备: {device}")
    print(f"启动服务器，阈值: {thresh}，端口: {port}")
    app.config['DEFAULT_THRESH'] = thresh
    app.run(host='0.0.0.0', port=port, debug=False, use_reloader=False)
