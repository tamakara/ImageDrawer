# BaKaBooru

**BaKaBooru** 是一个本地优先的现代化图片管理平台。它旨在解决海量本地图片（插画、AI 生图）管理难、检索难的问题。通过集成 AI 视觉模型，自动为图片建立语义标签，让混乱的文件夹变成井井有条的语义数据库。

## ✨ 核心特性

### 🤖 强大的标签系统
*   **AI 自动标注**: 集成深度学习视觉模型（基于 [Camie Tagger V2](https://huggingface.co/spaces/Camais03/camie-tagger-v2-app)），上传即自动生成精准标签 (Tag)。
*   **标签管理**: 支持重新生成标签、添加自定义标签（支持多种类型），具备完善的标签增删改查功能。
*   **多维分类**: 标签自动归类显示（版权、角色、作者、自定义、元数据等），支持颜色区分，直观清晰。
*   **安全编辑**: 需开启编辑模式（铅笔图标）才可删除标签，有效防止误操作。

### 🖼️ 现代化的图库管理
*   **沉浸式浏览**: 响应式瀑布流布局，结合虚拟滚动技术，支持数万张图片流畅预览与滑动。
*   **高效交互**:
    *   **右键菜单**: 定制化的图片右键菜单，提供查看详情、选中、下载、删除等快捷操作。
    *   **选中模式**: 支持长按快速选中、多选、一键全选/取消全选。
    *   **批量操作**: 支持选定图片后的批量打包下载 (ZIP) 和批量删除。
    *   **视觉反馈**: 选中状态下拥有清晰的边框高亮、打钩图标和遮罩效果。
*   **体验优化**: 全局禁用浏览器默认右键菜单和图片拖拽，带来类似原生应用的操作体验。

### 🏗️ 稳健的系统架构

BaKaBooru 采用 **多进程微服务架构**，通过 Launcher 统一调度管理。这种设计既保证了 Java 生态的稳健性，又充分利用了 Python 在 AI 领域的优势。

```mermaid
graph TD
    subgraph Client ["🖥️ 客户端层"]
        UI["Web 前端 (Vue 3)"]
    end

    subgraph Launcher ["🚀 BaKaBooru 启动器"]
        direction TB
        PM["进程管理器"]
        
        subgraph JavaProcess ["☕ 后端服务 (Spring Boot)"]
            API["REST 接口"]
            Core["业务逻辑"]
            JPA["持久层"]
        end

        subgraph PythonProcess ["🐍 AI 服务 (FastAPI)"]
            TagAPI["标签生成接口"]
            Inference["ONNX 推理引擎"]
        end
    end

    subgraph Storage ["💾 本地存储 (data/)"]
        SQLite[("SQLite 数据库")]
        Files["图片文件"]
        Models["AI 模型"]
    end

    %% Interactions
    User((用户)) <-->|浏览器| UI
    PM -.->|启动| JavaProcess
    PM -.->|启动| PythonProcess
    
    UI <-->|HTTP/REST| API
    API <--> Core
    Core <--> JPA
    Core <-->|内部 HTTP| TagAPI
    
    JPA <-->|JDBC| SQLite
    Core <-->|IO| Files
    TagAPI <--> Inference
    Inference <-->|加载| Models
```

*   **Launcher (启动器)**: 基于 Python 编写的轻量级进程管理器，负责一键启动/通过管道停止子服务，并实时聚合日志输出。
*   **Backend (业务核心)**: 
    *   **Spring Boot**: 处理业务逻辑、元数据管理、文件 I/O 和 RESTful API 暴露。
    *   **SQLite**: 选用无需配置的嵌入式数据库，存储标签、关联关系及配置信息。
*   **AI Service (智能引擎)**:
    *   **FastAPI**: 提供轻量级的高性能 HTTP 接口供后端调用。
    *   **ONNX Runtime**: 运行量化后的深度学习模型，实现毫秒级的本地标签推理，无需昂贵的 GPU 显卡（支持 CPU 推理）。
*   **Frontend (交互界面)**:
    *   **Vue 3 + Naive UI**: 构建 SPA 单页应用，提供类 Native App 的流畅交互体验。
    *   **Virtual Scroll**: 针对成千上万张图片的列表渲染进行深度优化。

### 💾 数据自主
*   **本地存储**: 所有图片、数据库、模型权重均存储在本地 `data/` 目录，隐私安全，易于迁移。
*   **智能缓存**: 自动管理的临时文件与缩略图缓存机制，保持系统轻量。

## 🛠️ 技术栈

*   **业务后端**: Java 21, Spring Boot 3, Spring Data JPA, SQLite
*   **AI 服务**: Python 3.10+, FastAPI, ONNX Runtime
*   **前端交互**: Vue 3, TypeScript, Naive UI, Tailwind CSS, TanStack Query
*   **部署构建**: PyInstaller, Launch4j, Maven, Vite

## 🚀 运行说明

### 环境准备
✅ **无需安装任何环境**。
得益于内置的独立运行环境（JRE & Python Runtime），BaKaBooru 实现了完全的 "开箱即用"。你不再需要手动配置 Java 或 Python 环境。

### 启动应用
1.  双击运行项目根目录下的 `bakabooru.exe`。
2.  **数据存储**: 首次运行将在 EXE 同级目录自动生成 `data/` 文件夹，包含数据库、图片存储和模型。
3.  **访问地址**: 启动后可通过浏览器访问 `http://localhost:8080` (端口视具体配置)。
4.  **注意**: 这是一个 "Launcher" 程序，它会启动后台 Java 服务和 Python Tagger 服务，请勿关闭弹出的命令行窗口（若有）。

### 🔧 高级配置 (命令行参数)
`bakabooru.exe` 支持以下命令行参数，用于自定义端口或服务地址（适用于端口冲突或高级部署场景）：

| 参数 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `--web-host` | `0.0.0.0` | 业务后端/Web服务监听地址 |
| `--web-port` | `8080` | 业务后端/Web服务监听端口 |
| `--tagger-host` | `0.0.0.0` | Tagger 服务监听地址 |
| `--tagger-port` | `8081` | Tagger 服务监听端口 |

**使用示例**:
在终端或快捷方式目标中添加参数：
```bash
# 修改 Web 端口为 9090
bakabooru.exe --web-port 9090
```

## 📦 编译构建

### 编译环境
若需从源码全量编译本项目，请确保开发环境已安装以下工具（需添加到 PATH）：
*   **Java**: JDK 21+
*   **Python**: 3.12+
*   **Node.js**: LTS 版本
*   **Maven**: 3.9+

### 一键编译
在项目根目录下执行 Maven 命令，自动完成前端构建、后端打包以及 EXE 生成：

```bash
mvn clean install
```

构建完成后，项目根目录将生成唯一的可执行文件：`bakabooru.exe`。


## 🛠️ 开发指南 (Dev)

若需单独开发调试各模块：

1.  **启动后端 (Backend)**:
    ```bash
    cd backend
    mvn spring-boot:run
    ```

2.  **启动前端 (Frontend)**:
    ```bash
    cd frontend
    pnpm install && pnpm dev
    ```

3.  **启动标签服务 (Tagger)**:
    ```bash
    cd tagger
    pip install -r requirements.txt
    python run_app.py --data_dir ../data
    ```
## 📄 许可证
本项目采用 MIT 许可证。
