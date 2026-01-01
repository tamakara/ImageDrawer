# ImageDrawer (Next Gen) - 项目重构需求文档

> **项目状态**: 规划与重构阶段
> **目标**: 打造一个高性能、用户体验极致的本地/私有云图片管理系统。

## 1. 项目愿景 (Vision)

本项目旨在解决旧版 ImageDrawer 架构粗糙、体验不佳的问题。我们将重新构建一个基于 Web 的现代化图片管理平台，核心理念是**“结构清晰、体验流畅、AI 赋能”**。

*   **跨平台**: 采用 B/S 架构，浏览器即客户端，完美适配 PC 与 移动端。
*   **极简运维**: 依赖少，部署简单，数据备份方便。
*   **AI 驱动**: 深度集成 AI 标签识别能力，自动化整理图片库。

## 2. 详细功能需求 (Functional Requirements)

### 2.1 图片浏览与检索 (Gallery & Search)

提供类似原生相册的流畅体验，支持海量图片的高效展示。

*   **响应式瀑布流 (Masonry Layout)**:
    *   自适应布局，从 4K 大屏到手机竖屏均能完美展示。
    *   **虚拟滚动 (Virtual Scrolling)**: 仅渲染可视区域图片，支持数万张图片流畅滑动，拒绝卡顿。
*   **沉浸式详情页 (Detail View)**:
    *   点击图片弹出模态窗口。
    *   **信息查看**: 显示分辨率、文件大小、格式、创建时间、更新时间、哈希值、原文件名等元数据。
    *   **编辑功能**: 支持修改图片标题（默认去除后缀）、手动添加/删除/修改标签。
    *   **文件管理**: 支持**替换图片文件**（保留标签和ID，仅更新文件内容和哈希），支持下载原图（文件名自动拼接标题和后缀）。
    *   **移动端优化**: 支持双指缩放查看细节。
*   **高级查询 (Advanced Search)**:
    *   **标签搜索**: 支持多标签组合查询。
    *   **逻辑运算**: 支持 `AND` (同时包含)、`OR` (或者)、`NOT` (排除) 逻辑。
    *   **智能补全**: 输入标签时，自动联想数据库中已有的标签。
*   **批量管理 (Batch Management)**:
    *   在浏览视图支持“选择模式” (按住 Shift/Ctrl 或长按)。
    *   **批量删除**: 一键删除多张图片。
    *   **批量下载**: 将选中的多张图片打包为 ZIP 下载。
    *   **批量编辑**: 批量添加或移除标签。

### 2.2 智能上传系统 (Smart Upload System)

上传是用户体验最关键的一环，必须解决“慢、乱、卡”的问题。

*   **多种上传方式**:
    *   **文件选择**: 支持多选文件。
    *   **文件夹扫描**: 利用 `<input type="file" webkitdirectory>` 支持扫描整个文件夹（包括子目录）。
    *   **拖拽上传**: 支持直接拖入文件或文件夹。
*   **智能预处理 (Pre-processing)**:
    *   **后缀名过滤**: 在“设置”页面配置允许的后缀名（如 `jpg, png, webp, gif`），前端自动过滤非图片文件。
    *   **文件大小限制**: 用户可在设置中配置“单文件最大限制”（如 50MB）。前端在上传前校验，超限文件直接标记失败并提示，避免无效传输。
*   **可视化任务队列 (Task Queue UI)**:
    *   提供全局的“上传任务中心”面板（可最小化）。
    *   **状态流转**: 清晰展示每个文件的状态：`等待中` -> `上传中` -> `AI识别中` -> `保存中` -> `完成`。
    *   **异常处理**: 上传失败、AI 识别超时或文件过大导致的错误，需在队列中红字标出，并提供“重试”或“忽略”选项。
*   **大文件优化策略**:
    *   为了防止大图（如 8K+ 分辨率）导致 Tagger 服务崩溃或超时，后端在接收到图片后，会生成一张**临时缩略图**（如长边 1024px）发送给 Tagger 进行识别。
    *   原始图片直接保存，确保画质无损。

### 2.3 AI Tagger 服务集成 (AI Integration)

本项目不内置 AI 模型，而是作为客户端调用外部 Tagger 服务，实现解耦。

*   **多节点支持**:
    *   在“设置”页面可以添加多个 Tagger 服务器。
    *   配置项：`服务器名称`、`API 地址` (Endpoint)。
*   **调用策略**:
    *   上传时，用户可选择使用哪个 Tagger 服务器。
    *   后端通过 HTTP 协议将图片（或缩略图）发送给 Tagger 服务，接收返回的标签列表。

### 2.4 数据存储与备份 (Storage & Backup)

采用**文件系统 + SQLite** 的组合，兼顾性能与便携性。

*   **存储策略 (Storage Strategy)**:
    *   **图片文件**: 存储在本地文件系统 (`/data/images/`)。
        *   **哈希存储**: 文件名使用 SHA-256 哈希值（无后缀），实现自动去重。
        *   **缩略图**: 自动生成缩略图存储在 `/data/temp/`，加速列表加载。
    *   **元数据**: 存储在 SQLite 数据库 (`/data/db/data.sqlite`)。
        *   *理由*: 单文件数据库，无需安装 MySQL 等服务，零配置启动，查询速度极快。
*   **备份与还原 (Backup & Restore)**:
    *   **一键备份**: 在设置页面点击“导出备份”。
        *   后端将 `data.sqlite` 和 `images/` 目录打包成一个 ZIP 文件供用户下载。
    *   **一键还原**: 在设置页面上传备份 ZIP 包。
        *   后端解压覆盖现有数据（需弹窗警告数据覆盖风险）。

## 3. 系统设计与架构 (System Design & Architecture)

为了确保项目结构清晰且易于维护，我们采用**模块化**的设计思路，并引入现代化的开发工具。

### 3.1 逻辑模块划分 (Logical Modules)

为了实现高内聚低耦合，我们将系统按业务领域拆分为五个独立模块：

1.  **图库核心模块 (Gallery Core)**
    *   **职责**: 图片资产的“管家”，负责最基础的数据管理。
    *   **功能**: 图片实体的增删改查 (CRUD)、物理文件的读写与哈希校验、缩略图的实时生成与缓存。
2.  **上传与处理模块 (Upload & Processing)**
    *   **职责**: 图片进入系统的“流水线”，处理复杂的导入逻辑。
    *   **功能**: 接收上传请求（支持文件夹递归扫描）、执行文件预检（格式/大小过滤）、维护**上传任务队列**、协调 AI 识别与最终入库的流程。
3.  **搜索与元数据模块 (Search & Metadata)**
    *   **职责**: 图片的“索引库”，提供高效的检索能力。
    *   **功能**: 标签管理 (Tag CRUD)、标签关联逻辑、构建复杂查询条件 (Specification)、支持标签的逻辑组合搜索 (AND/OR/NOT)。
4.  **Tagger 集成模块 (Tagger Integration)**
    *   **职责**: 与外部智能大脑的“连接器”，实现服务解耦。
    *   **功能**: 管理多 Tagger 服务器配置、封装统一的 HTTP 调用接口、处理识别结果的标准化转换。
5.  **系统基础设施模块 (System Infrastructure)**
    *   **职责**: 应用的“后勤保障”。
    *   **功能**: 全局设置管理 (Settings)、数据库与文件的一键备份 (Backup) 与还原 (Restore)。

### 3.2 技术选型与工具 (Tech Stack & Tools)

#### 后端 (Backend)
*   **核心框架**: Spring Boot 3 (Java 21)
*   **数据库**: SQLite + **Flyway** (数据库版本迁移工具，确保 Schema 变更安全)
*   **ORM**: Spring Data JPA
*   **对象映射**: **MapStruct** (高性能 Bean 映射，减少手动 get/set)
*   **API 文档**: **SpringDoc (OpenAPI 3)** (自动生成接口文档)
*   **并发**: Java 21 Virtual Threads (虚拟线程)

#### 前端 (Frontend)
*   **核心框架**: Vue 3 + TypeScript + Vite
*   **UI 组件**: Naive UI + Tailwind CSS
*   **状态管理**: Pinia
*   **数据请求**: **TanStack Query (Vue Query)** (服务端状态管理、缓存、自动重试)
*   **工具库**: **VueUse**

### 3.3 详细目录结构 (Detailed Project Structure)

采用 **"按功能分包 (Package by Feature)"** 的策略，使代码结构更具可读性。

```
ImageDrawer/
├── backend/                                # 后端工程
│   ├── src/main/java/com/imagedrawer/
│   │   ├── common/                         # [通用层]：跨模块复用的代码
│   │   │   ├── config/                     # 全局配置 (Async, WebMvc, Swagger)
│   │   │   ├── event/                      # 模块间通信的事件定义 (Spring Events)
│   │   │   ├── exception/                  # 全局异常定义
│   │   │   └── util/                       # 工具类 (FileUtil, HashUtil)
│   │   ├── module/                         # [业务层]：按逻辑模块划分
│   │   │   ├── gallery/                    # 1. 图库核心
│   │   │   │   ├── controller/             # ImageController (浏览/详情)
│   │   │   │   ├── entity/                 # Image
│   │   │   │   ├── repository/
│   │   │   │   └── service/                # ImageService, StorageService
│   │   │   ├── upload/                     # 2. 上传处理
│   │   │   │   ├── controller/             # UploadController
│   │   │   │   ├── model/                  # UploadTask (队列中的任务模型)
│   │   │   │   └── service/                # UploadQueueService, FileScanner
│   │   │   ├── search/                     # 3. 搜索元数据
│   │   │   │   ├── controller/             # TagController, SearchController
│   │   │   │   ├── entity/                 # Tag
│   │   │   │   ├── repository/
│   │   │   │   └── service/                # TagService, SearchService
│   │   │   ├── tagger/                     # 4. Tagger 集成
│   │   │   │   ├── client/                 # TaggerApiClient (HTTP调用)
│   │   │   │   ├── entity/                 # TaggerServerConfig
│   │   │   │   └── service/                # TaggerService
│   │   │   └── system/                     # 5. 系统基础
│   │   │       ├── controller/             # SettingsController, BackupController
│   │   │       └── service/
│   │   └── ImageDrawerApplication.java
│   └── ...
├── frontend/                               # 前端工程
│   ├── src/
│   │   ├── api/                            # API 定义 (按模块: gallery, upload, search, tagger, system)
│   │   ├── components/                     # 组件库
│   │   │   ├── common/                     # 基础UI
│   │   │   ├── business/                   # 业务组件 (ImageCard, UploadQueue)
│   │   │   └── layout/                     # 布局组件
│   │   ├── hooks/                          # 组合式函数
│   │   ├── stores/                         # Pinia 状态
│   │   ├── views/                          # 页面视图
│   │   │   ├── gallery/                    # 图库页
│   │   │   ├── upload/                     # 上传页
│   │   │   ├── detail/                     # 详情页
│   │   │   └── settings/                   # 设置页
│   │   └── App.vue
│   └── ...
└── README.md
```

## 4. 外部接口协议 (Tagger API Protocol)

Tagger 服务需遵循以下简单的 HTTP 协议：

*   **Method**: `POST`
*   **Content-Type**: `multipart/form-data`
*   **Request**:
    *   `file`: 图片文件二进制流
*   **Response (JSON)**:
    ```json
    {
      "success": true,
      "data": {
        "tags": [
          { "name": "1girl", "score": 0.99 },
          { "name": "blue_eyes", "score": 0.85 }
        ]
      },
      "error": null
    }
    ```

## 5. 用户体验细节 (UX Details)

*   **加载状态**: 图片加载过程中显示占位骨架屏 (Skeleton)。
*   **错误反馈**: 网络请求失败或后端报错时，使用 Toast 轻提示，而不是弹窗打断用户。
*   **深色模式**: 自动跟随系统或手动切换深色/浅色主题。

## 6. 如何运行 (How to Run)

### 后端 (Backend)

1.  进入 `backend` 目录。
2.  运行 `mvn spring-boot:run`。
    *   后端服务将在 `http://localhost:8080` 启动。
    *   API 文档地址: `http://localhost:8080/swagger-ui.html`。
    *   数据文件将存储在项目根目录的 `data/` 文件夹下。

### 前端 (Frontend)

1.  进入 `frontend` 目录。
2.  安装依赖: `pnpm install`。
3.  启动开发服务器: `pnpm dev`。
    *   前端页面将在 `http://localhost:5173` (默认) 启动。
