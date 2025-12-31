import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'

export interface TaggerServer {
  id: string
  name: string
  url: string
  active: boolean
}

export const useSettingsStore = defineStore('settings', () => {
  // Persist settings to localStorage
  const taggerServers = useStorage<TaggerServer[]>('settings-tagger-servers', [])
  const uploadExtensions = useStorage<string[]>('settings-upload-extensions', ['jpg', 'jpeg', 'png', 'webp', 'gif'])
  const maxUploadSize = useStorage<number>('settings-max-upload-size', 50) // MB

  function addTaggerServer(server: Omit<TaggerServer, 'id'>) {
    taggerServers.value.push({
      ...server,
      id: crypto.randomUUID()
    })
  }

  function removeTaggerServer(id: string) {
    const index = taggerServers.value.findIndex(s => s.id === id)
    if (index !== -1) {
      taggerServers.value.splice(index, 1)
    }
  }

  return {
    taggerServers,
    uploadExtensions,
    maxUploadSize,
    addTaggerServer,
    removeTaggerServer
  }
})

