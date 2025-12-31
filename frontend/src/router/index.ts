import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/gallery'
    },
    {
      path: '/gallery',
      name: 'gallery',
      component: () => import('../views/gallery/GalleryView.vue')
    },
    {
      path: '/upload',
      name: 'upload',
      component: () => import('../views/upload/UploadView.vue')
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/settings/SettingsView.vue')
    }
  ]
})

export default router

