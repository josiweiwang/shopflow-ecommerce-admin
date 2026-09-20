import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import '@/styles/index.css'
import App from './App.vue'
import router from './router'

/**
 * 应用入口。
 *
 * Element Plus 采用全量引入（配置简单、不会漏注册组件）；
 * 图标统一注册为全局组件，模板里可以直接写 <el-icon><Search /></el-icon>。
 */
const app = createApp(App)

Object.entries(ElementPlusIcons).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')