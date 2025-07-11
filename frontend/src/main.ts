import {createApp} from 'vue'
import {createPinia} from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)

async function bootstrap() {
    if (isMobile) {
        const {default: MobileApp} = await import('./MobileApp.vue')
        const app = createApp(MobileApp)
        app.use(createPinia())
        app.use(ElementPlus)
        app.mount('#app')
    } else {
        const {default: DesktopApp} = await import('./DesktopApp.vue')
        const app = createApp(DesktopApp)
        app.use(createPinia())
        app.use(ElementPlus)
        app.mount('#app')
    }
}

bootstrap()
