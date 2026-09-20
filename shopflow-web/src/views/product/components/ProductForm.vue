<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑商品' : '新增商品'"
    width="720px"
    destroy-on-close
    @update:model-value="handleVisibleChange"
    @open="handleOpen"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="商品分类" prop="categoryId">
            <el-cascader
              v-model="form.categoryId"
              class="full-width"
              :options="categoryOptions"
              :props="{ emitPath: false }"
              placeholder="请选择二级分类"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品编码" prop="sku">
            <el-input v-model="form.sku" placeholder="如 SF-PHONE-0007" maxlength="64" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="商品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="128" />
      </el-form-item>

      <el-form-item label="卖点副标题" prop="subtitle">
        <el-input v-model="form.subtitle" placeholder="选填，例如：6.7 英寸 120Hz 高刷屏" maxlength="255" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="销售价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="原价" prop="originalPrice">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" class="full-width" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item v-if="!isEdit" label="初始库存" prop="initStock">
            <el-input-number v-model="form.initStock" :min="0" :step="10" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="库存预警值" prop="warnStock">
            <el-input-number v-model="form.warnStock" :min="0" :step="5" class="full-width" :disabled="isEdit" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" :max="9999" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="主图地址" prop="mainImage">
        <el-input v-model="form.mainImage" placeholder="选填，图片 URL" maxlength="255" />
      </el-form-item>

      <el-form-item label="商品详情" prop="detail">
        <el-input v-model="form.detail" type="textarea" :rows="3" placeholder="选填，商品详情描述" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createProduct, fetchProductDetail, updateProduct, type ProductPayload } from '@/api/product'
import type { CategoryOption } from '@/types'

/**
 * 商品新增/编辑弹窗。
 *
 * 编辑态会先拉一次详情（走的是后端的 Redis 缓存接口），
 * 这样即使列表接口没有返回大字段 detail，编辑页也能拿到完整内容。
 */
const props = defineProps<{
  visible: boolean
  productId: string | null
  categoryOptions: CategoryOption[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const isEdit = computed(() => props.productId !== null)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ProductPayload>({
  categoryId: '',
  name: '',
  sku: '',
  subtitle: '',
  mainImage: '',
  detail: '',
  price: 0,
  originalPrice: 0,
  status: 1,
  sort: 0,
  initStock: 0,
  warnStock: 10
})

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  sku: [
    { required: true, message: '请输入商品编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]+$/, message: '只能包含字母、数字与短横线', trigger: 'blur' }
  ],
  price: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

function resetForm(): void {
  form.categoryId = ''
  form.name = ''
  form.sku = ''
  form.subtitle = ''
  form.mainImage = ''
  form.detail = ''
  form.price = 0
  form.originalPrice = 0
  form.status = 1
  form.sort = 0
  form.initStock = 0
  form.warnStock = 10
}

function handleVisibleChange(value: boolean): void {
  emit('update:visible', value)
}

async function handleOpen(): Promise<void> {
  resetForm()
  if (!props.productId) {
    return
  }
  loading.value = true
  try {
    const { data } = await fetchProductDetail(props.productId)
    form.categoryId = data.categoryId
    form.name = data.name
    form.sku = data.sku
    form.subtitle = data.subtitle ?? ''
    form.mainImage = data.mainImage ?? ''
    form.detail = data.detail ?? ''
    form.price = data.price
    form.originalPrice = data.originalPrice ?? data.price
    form.status = data.status
    form.sort = data.sort ?? 0
    form.warnStock = data.warnStock ?? 10
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    if (props.productId) {
      await updateProduct(props.productId, { ...form })
      ElMessage.success('商品已更新')
    } else {
      await createProduct({ ...form })
      ElMessage.success('商品已创建，库存记录已同步初始化')
    }
    emit('update:visible', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}
</style>