import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
})

export interface TaggerSettings {
  id?: number
  threshold: number
  minConfidence: number
}

export const taggerApi = {
  getSettings: async () => {
    const response = await apiClient.get<TaggerSettings>('/tagger/settings')
    return response.data
  },

  updateSettings: async (settings: TaggerSettings) => {
    const response = await apiClient.post<TaggerSettings>('/tagger/settings', settings)
    return response.data
  }
}

