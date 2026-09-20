/** 统一响应体，与后端 R<T> 一一对应 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  traceId?: string
  timestamp?: number
}

/** 统一分页结果，与后端 PageResult<T> 一一对应 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页查询公共参数 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  sortBy?: string
  order?: string
}

export interface LoginResult {
  tokenType: string
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
}

export interface UserInfo {
  id: string
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  roleCodes: string[]
  permissions: string[]
  lastLoginAt?: string
}

export interface CategoryNode {
  id: string
  parentId: string
  name: string
  level: number
  sort: number
  icon?: string
  status: number
  createTime?: string
  children: CategoryNode[]
}

export interface ProductItem {
  id: string
  categoryId: string
  categoryName?: string
  name: string
  sku: string
  subtitle?: string
  mainImage?: string
  detail?: string
  price: number
  originalPrice?: number
  status: number
  statusDesc?: string
  sales?: number
  sort?: number
  availableStock?: number
  lockedStock?: number
  totalStock?: number
  warnStock?: number
  lowStock?: boolean
  createTime?: string
  updateTime?: string
}

export interface ProductQuery extends PageQuery {
  keyword?: string
  categoryId?: string
  status?: number
  minPrice?: number
  maxPrice?: number
}

export interface InventoryItem {
  id: string
  productId: string
  productName?: string
  sku?: string
  totalStock: number
  availableStock: number
  lockedStock: number
  warnStock: number
  version?: number
  fromCache?: boolean
  lowStock?: boolean
  updateTime?: string
}

export interface InventoryLogItem {
  id: string
  productId: string
  productName?: string
  orderNo?: string
  bizType: number
  bizTypeDesc?: string
  quantity: number
  beforeAvailable: number
  afterAvailable: number
  operatorId?: string
  remark?: string
  createTime?: string
}

export interface OrderItem {
  id: string
  productId: string
  sku: string
  productName: string
  productImage?: string
  price: number
  quantity: number
  subtotal: number
}

export interface OrderStatusLog {
  fromStatus?: number
  fromStatusDesc?: string
  toStatus: number
  toStatusDesc?: string
  operatorType?: number
  operatorTypeDesc?: string
  operatorId?: number
  remark?: string
  createTime?: string
}

export interface OrderRow {
  id: string
  orderNo: string
  userId: string
  username?: string
  totalAmount: number
  freightAmount: number
  payAmount: number
  status: number
  statusDesc?: string
  receiverName: string
  receiverPhone: string
  itemCount?: number
  totalQuantity?: number
  expireTime?: string
  payTime?: string
  createTime?: string
}

export interface OrderDetail extends OrderRow {
  receiverAddress?: string
  remark?: string
  cancelReason?: string
  deliverTime?: string
  finishTime?: string
  cancelTime?: string
  items: OrderItem[]
  statusLogs: OrderStatusLog[]
}

export interface OrderQuery extends PageQuery {
  orderNo?: string
  userId?: string
  status?: number
  startTime?: string
  endTime?: string
}

export interface CreateOrderPayload {
  requestNo: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  remark?: string
  items: Array<{ productId: string; quantity: number }>
}

export interface OrderCreateResult {
  orderNo: string
  payAmount: number
  status: number
  statusDesc?: string
  expireTime?: string
}

export interface DashboardOverview {
  userCount: number
  productCount: number
  orderCount: number
  pendingPaymentCount: number
  todayOrderCount: number
  todayAmount: number
  totalAmount: number
}

export interface TrendPoint {
  date: string
  orderCount: number
  amount: number
}

export interface TopProduct {
  productId: string
  productName: string
  quantity: number
  amount: number
}

export interface UserRow {
  id: string
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  status: number
  roleNames?: string
  lastLoginAt?: string
  createTime?: string
}

export interface UserQuery extends PageQuery {
  keyword?: string
  status?: number
  roleId?: string
}

export interface RoleOption {
  id: string
  roleCode: string
  roleName: string
  description?: string
}

export interface OperationLogRow {
  id: string
  userId: string
  username: string
  module: string
  operation: string
  requestUri: string
  requestMethod: string
  ip: string
  durationMs: number
  success: number
  errorMsg?: string
  createTime: string
}
/** 级联选择器选项（分类树转换而来） */
export interface CategoryOption {
  value: string
  label: string
  children?: CategoryOption[]
}

/** 商品表单提交体 */
export interface ProductFormPayload {
  categoryId: string
  name: string
  sku: string
  subtitle?: string
  mainImage?: string
  detail?: string
  price: number
  originalPrice?: number
  status?: number
  sort?: number
  initStock?: number
  warnStock?: number
}