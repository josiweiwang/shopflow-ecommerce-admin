<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-intro">
        <h1>ShopFlow</h1>
        <p class="intro-sub">电商后台管理系统</p>
        <ul class="intro-list">
          <li>商品 / 分类 / 库存 / 订单 一体化管理</li>
          <li>JWT 双令牌 + RBAC 接口级权限控制</li>
          <li>Redis 预扣 + 数据库条件更新防超卖</li>
        </ul>
      </div>

      <el-card class="login-card" shadow="always">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large" @keyup.enter="submitLogin">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="用户名" clearable>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password clearable>
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loading" @click="submitLogin">登 录</el-button>
            </el-form>

            <div class="demo-accounts">
              <span class="demo-label">演示账号：</span>
              <el-tag
                v-for="account in demoAccounts"
                :key="account.username"
                class="demo-tag"
                type="info"
                effect="plain"
                @click="fillAccount(account)"
              >
                {{ account.label }}
              </el-tag>
            </div>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="registerForm.username" placeholder="用户名（4-32 位字母数字下划线）" clearable />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="密码（8-32 位，含字母与数字）" show-password />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" show-password />
              </el-form-item>
              <el-form-item prop="nickname">
                <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" clearable />
              </el-form-item>
              <el-form-item prop="email">
                <el-input v-model="registerForm.email" placeholder="邮箱（选填）" clearable />
              </el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loading" @click="submitRegister">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/auth'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'

/**
 * 登录 / 注册页。
 *
 * 登录成功后不直接跳到 /dashboard，而是跳到「当前用户有权访问的第一个页面」：
 * 普通用户没有数据看板权限，硬跳 dashboard 只会被守卫弹回来。
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const activeTab = ref('login')
const loading = ref(false)

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: ''
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '长度需在 4-32 位之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字与下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '长度需在 8-32 位之间', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '必须同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '管理员 admin', username: 'admin', password: 'Admin@123456' },
  { label: '运营 operator', username: 'operator', password: 'Operator@123456' },
  { label: '普通用户 demo', username: 'demo', password: 'User@123456' }
]

function fillAccount(account: { username: string; password: string }): void {
  loginForm.username = account.username
  loginForm.password = account.password
}

async function submitLogin(): Promise<void> {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await userStore.doLogin({ ...loginForm })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string | undefined
    router.push(redirect && redirect.length > 0 ? redirect : permissionStore.firstAccessiblePath)
  } finally {
    loading.value = false
  }
}

async function submitRegister(): Promise<void> {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || undefined,
      email: registerForm.email || undefined
    })
    ElMessage.success('注册成功，请使用新账号登录')
    loginForm.username = registerForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b5876 50%, #4e4376 100%);
}

.login-panel {
  display: flex;
  align-items: center;
  gap: 56px;
  padding: 24px;
}

.login-intro {
  color: #fff;
  max-width: 360px;
}

.login-intro h1 {
  margin: 0;
  font-size: 44px;
  letter-spacing: 2px;
}

.intro-sub {
  margin: 8px 0 24px;
  font-size: 16px;
  opacity: 0.85;
}

.intro-list {
  padding-left: 18px;
  line-height: 2;
  opacity: 0.85;
}

.login-card {
  width: 380px;
  border-radius: 10px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.demo-accounts {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.demo-label {
  font-size: 12px;
  color: #909399;
}

.demo-tag {
  cursor: pointer;
}
</style>