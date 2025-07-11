import axios, {type AxiosInstance} from 'axios'
import qs from 'qs'

// ======================
// 类型定义
// ======================

/** 图片实体 */
export interface Image {
    filename: string
    mimetype: string
    hash: string
    size: number
    width: number
    height: number
    rating: string
    tags: string[]
}

/** 分页信息 */
export interface Page<T> {
    records: T[]
    totalElements: number
    totalPages: number
    page: number
    pageSize: number
}

/** 图片查询条件 */
export interface ImageQueryParams {
    tags: string[]
    sort: string
    rating: string
    page: number
    pageSize: number
    seed: string
}

// ======================
// Axios 实例
// ======================

const api: AxiosInstance = axios.create({
    baseURL:
        import.meta.env.MODE === 'production'
            ? '/'
            : 'http://localhost:8080',
})

// 响应拦截器
api.interceptors.response.use(
    res => res,
    error => {
        console.error('API 请求错误:', error);
        return Promise.reject(error);
    }
);

// ======================
// Image API
// ======================

/**
 * 添加图片
 * @param file 图片文件
 */
export async function addImage(file: File): Promise<Image> {
    const formData = new FormData()
    formData.append('file', file)

    const res = await api.post<Image>('/image', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    })
    return res.data
}

/**
 * 获取图片信息
 * @param hash 图片hash
 */
export async function getImage(hash: string): Promise<Image> {
    const res = await api.get<Image>(`/image/${hash}`)
    return res.data
}

/**
 * 删除图片
 * @param hash 图片hash
 */
export async function deleteImage(hash: string): Promise<void> {
    await api.delete(`/image/${hash}`)
}

// ======================
// Tag API
// ======================

/**
 * 设置tagger服务器地址
 * @param url 服务器地址
 */
export async function setTaggerUrl(url: string): Promise<void> {
    await api.put('/tag/tagger', url, {
        headers: {
            'Content-Type': 'text/plain'
        }
    });
}

/**
 * 查询匹配标签
 * @param keyword 关键词
 */
export async function queryTags(keyword: string): Promise<string[]> {
    const res = await api.get<string[]>('/tag/query', {params: {keyword}});
    return res.data;
}

// ======================
// Query API
// ======================


/**
 * 查询图片
 * @param params 查询条件
 */
export async function queryImages(params: ImageQueryParams): Promise<Page<Image>> {
    const res = await api.get<Page<Image>>('/query', {
        params,
        paramsSerializer: params => qs.stringify(params, {arrayFormat: 'repeat'})
    })
    return res.data
}

// ======================
// File API
// ======================

/**
 * 下载单张图片
 * @param hash 图片哈希值
 * @returns blob 和后端生成的文件名
 */
export async function getImageFile(hash: string): Promise<{ blob: Blob, filename: string }> {
    const response = await api.get(
        '/file',
        {
            params: {hash},
            responseType: 'blob'
        }
    )

    const filename = response.headers['x-filename'] || hash + '.jpg';
    const blob = new Blob([response.data]);
    return {blob, filename}
}

/**
 * 打包批量下载图片
 * @param hashes 图片哈希数组
 * @returns blob 和后端生成的文件名
 */
export async function getImageFileZip(hashes: string[]): Promise<{ blob: Blob; filename: string }> {
    const response = await api.post(
        '/file/zip',
        hashes,
        {
            responseType: 'blob'
        }
    );

    const filename = response.headers['x-filename'] || 'unknown.zip';
    const blob = new Blob([response.data], {type: 'application/zip'});

    return {blob, filename};
}