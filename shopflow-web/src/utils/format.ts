/** 统一格式化与状态映射工具 */

export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 金额展示：统一两位小数并加千分位 */
export function formatAmount(value?: number | string | null): string {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '0.00'
  }
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 时间展示：后端已按 yyyy-MM-dd HH:mm:ss 输出，这里只处理空值 */
export function formatDateTime(value?: string | null): string {
  return value && value.length > 0 ? value : '-'
}

/** 订单状态 -> 标签颜色 */
export function orderStatusTagType(status: number): TagType {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'primary'
    case 2:
      return 'info'
    case 3:
      return 'success'
    case 4:
      return 'danger'
    default:
      return 'info'
  }
}

export const ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '配送中', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
]

export const PRODUCT_STATUS_OPTIONS = [
  { label: '上架', value: 1 },
  { label: '下架', value: 0 }
]

export const INVENTORY_BIZ_TYPE_OPTIONS = [
  { label: '入库', value: 1 },
  { label: '锁定', value: 2 },
  { label: '扣减', value: 3 },
  { label: '释放', value: 4 },
  { label: '盘点调整', value: 5 }
]

/**
 * 生成下单幂等号。
 *
 * 前端负责生成并保证「同一次提交用同一个值」，
 * 服务端凭它保证网络重试或用户连点只产生一笔订单。
 */
export function generateRequestNo(): string {
  const now = new Date()
  const pad = (n: number, len = 2) => String(n).padStart(len, '0')
  const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  const random = Math.floor(Math.random() * 100000).toString().padStart(5, '0')
  return `web-${stamp}-${random}`
}