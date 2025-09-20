# 前端项目结构说明

## 项目重构后的目录结构

```
src/
├── api/                    # API服务层
│   ├── http.ts            # HTTP客户端配置
│   ├── index.ts           # API统一导出
│   └── services/          # 服务模块
│       ├── imageService.ts    # 图片相关API
│       ├── tagService.ts      # 标签相关API
│       ├── queryService.ts    # 查询相关API
│       ├── fileService.ts     # 文件下载相关API
│       └── index.ts           # 服务统一导出
├── components/            # 组件库
│   ├── forms/            # 表单相关组件
│   │   └── index.ts      # 表单组件导出
│   ├── image/            # 图片相关组件
│   │   └── index.ts      # 图片组件导出
│   ├── *.vue             # 各种Vue组件
│   └── index.ts          # 组件统一导出
├── config/               # 配置管理
│   ├── env.ts            # 环境变量配置
│   └── index.ts          # 配置统一导出
├── constants/            # 常量定义
│   └── index.ts          # 应用常量
├── stores/               # 状态管理
│   ├── image.ts          # 图片状态管理
│   └── index.ts          # Store统一导出
├── types/                # 类型定义
│   └── index.ts          # TypeScript类型定义
├── utils/                # 工具函数
│   └── index.ts          # 通用工具函数
├── DesktopApp.vue        # 桌面端应用入口
├── MobileApp.vue         # 移动端应用入口
└── main.ts               # 应用主入口
```

## 重构优势

### 1. 模块化架构
- **API服务分离**: 将不同功能的API分别封装为服务类，提高代码可维护性
- **组件分类**: 按功能将组件分组管理，便于查找和复用
- **类型集中管理**: 统一的类型定义，避免重复定义

### 2. 清晰的分层结构
- **API层**: 负责与后端通信的所有逻辑
- **Service层**: 业务逻辑封装
- **Store层**: 状态管理
- **Component层**: UI组件
- **Utils层**: 工具函数

### 3. 配置管理
- **环境配置**: 统一的环境变量管理
- **常量管理**: 应用级常量集中定义
- **路径别名**: 使用@符号简化导入路径

### 4. 开发体验优化
- **TypeScript支持**: 完整的类型检查和智能提示
- **模块热替换**: Vite配置优化，提升开发效率
- **代码分割**: 优化打包配置，提升加载性能

## 使用指南

### 导入方式
```typescript
// 使用路径别名
import { ImageService } from '@/api/services/imageService'
import { useImageStore } from '@/stores'
import { formatFileSize } from '@/utils'
import { IMAGE_RATINGS } from '@/constants'
import type { Image } from '@/types'

// 或使用统一导出
import { ImageService, useImageStore } from '@/api'
```

### 添加新功能
1. **新API服务**: 在`api/services/`下创建新的服务文件
2. **新组件**: 按功能分类放入对应的组件文件夹
3. **新类型**: 在`types/index.ts`中添加类型定义
4. **新常量**: 在`constants/index.ts`中添加常量定义

### 命名规范
- **文件名**: 使用camelCase命名
- **组件名**: 使用PascalCase命名
- **服务类**: 使用PascalCase + Service后缀
- **类型名**: 使用PascalCase命名
- **常量名**: 使用UPPER_SNAKE_CASE命名

## 技术栈

- **Vue 3**: 前端框架
- **TypeScript**: 类型支持
- **Pinia**: 状态管理
- **Element Plus**: UI组件库
- **Vite**: 构建工具
- **Axios**: HTTP客户端

## 注意事项

1. 所有新增的服务类都应该是静态方法，保持无状态
2. 组件之间的通信优先使用props和emit，复杂状态使用Pinia
3. 工具函数应该是纯函数，避免副作用
4. 类型定义应该尽可能复用，避免重复定义
