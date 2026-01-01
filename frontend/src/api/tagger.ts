import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
})

export interface TaggerServerConfig {
  id: number
  name: string
  url: string
  active: boolean
}

export const taggerApi = {
  listServers: async () => {
    const response = await apiClient.get<TaggerServerConfig[]>('/tagger/servers')
    return response.data
  },

  addServer: async (config: Omit<TaggerServerConfig, 'id'>) => {
    const response = await apiClient.post<TaggerServerConfig>('/tagger/servers', config)
    return response.data
  },

  deleteServer: async (id: number) => {
    await apiClient.delete(`/tagger/servers/${id}`)
  }
}

