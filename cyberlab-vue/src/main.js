import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/global-fixes.css'
import '@/styles/apple-theme.css'
import { useAuth } from '@/composables/useAuth'
import { initPageSettings } from '@/utils/pageTitle'
import { startAutoRefresh } from '@/utils/tokenRefresh'

// 初始化认证状态
const { initializeAuth } = useAuth()
initializeAuth()

// 启动自动令牌刷新
startAutoRefresh()

// 初始化页面设置（标题和图标）
initPageSettings()

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.mount('#app')