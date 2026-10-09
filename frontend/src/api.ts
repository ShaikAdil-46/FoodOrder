export type Role = 'CUSTOMER' | 'ADMIN'

export interface User {
  id: number
  firstName: string
  lastName: string
  mobile: string
  email: string
  role: Role
}

export interface Food {
  id: number
  name: string
  description: string
  category: string
  price: number
  imageUrl: string
  available: boolean
  vegetarian: boolean
}

export interface FoodPage {
  items: Food[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface CartLine {
  foodId: number
  name: string
  imageUrl: string
  unitPrice: number
  quantity: number
  lineTotal: number
  available: boolean
}

export interface Cart {
  items: CartLine[]
  subtotal: number
  itemCount: number
}

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'

export interface Order {
  id: number
  customerName: string
  customerEmail: string
  status: OrderStatus
  totalAmount: number
  deliveryAddress: string
  createdAt: string
  items: { name: string; unitPrice: number; quantity: number }[]
}

interface LoginResponse {
  token: string
  user: User
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const API = '/api'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isFood(value: unknown): value is Food {
  return isRecord(value) && typeof value.id === 'number' && typeof value.name === 'string'
    && typeof value.description === 'string' && typeof value.category === 'string'
    && typeof value.price === 'number' && typeof value.imageUrl === 'string'
    && typeof value.available === 'boolean' && typeof value.vegetarian === 'boolean'
}

function isFoodPage(value: unknown): value is FoodPage {
  return isRecord(value) && Array.isArray(value.items) && value.items.every(isFood)
    && typeof value.page === 'number' && typeof value.size === 'number'
    && typeof value.totalItems === 'number' && typeof value.totalPages === 'number'
}

export function saveToken(token: string | null) {
  if (token) localStorage.setItem('foodorder-token', token)
  else localStorage.removeItem('foodorder-token')
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('foodorder-token')
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (token && path !== '/auth/login' && path !== '/auth/register') {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API}${path}`, { ...options, headers })
  } catch {
    throw new ApiError('Cannot reach FoodOrder right now. Check that the backend is running.', 0)
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { message?: string; fieldErrors?: Record<string, string> }
      | null
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/register') {
      saveToken(null)
      window.dispatchEvent(new Event('foodorder-session-expired'))
    }
    throw new ApiError(
      body?.message ?? 'Something went wrong. Please try again.',
      response.status,
      body?.fieldErrors,
    )
  }

  if (response.status === 204) return undefined as T
  const result = (await response.json()) as T
  if (path.startsWith('/cart/') && options.method && options.method !== 'GET') {
    window.dispatchEvent(new Event('foodorder-cart-updated'))
  }
  return result
}

export const api = {
  foods: async (params: URLSearchParams = new URLSearchParams()) => {
    const query = params.toString()
    const result = await request<unknown>(`/foods${query ? `?${query}` : ''}`)
    if (!isFoodPage(result)) {
      throw new ApiError('The connected API is not returning FoodOrder menu data. Check VITE_API_PROXY_TARGET.', 502)
    }
    return result
  },
  food: (id: string) => request<Food>(`/foods/${id}`),
  login: (email: string, password: string) =>
    request<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (data: {
    firstName: string
    lastName: string
    mobile: string
    email: string
    password: string
  }) => request<LoginResponse>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  me: () => request<User>('/auth/me'),
  updateProfile: (data: Pick<User, 'firstName' | 'lastName' | 'mobile'>) =>
    request<User>('/profile', { method: 'PUT', body: JSON.stringify(data) }),
  cart: () => request<Cart>('/cart'),
  addToCart: (foodId: number, quantity = 1) =>
    request<Cart>('/cart/items', { method: 'POST', body: JSON.stringify({ foodId, quantity }) }),
  setCartQuantity: (foodId: number, quantity: number) =>
    request<Cart>(`/cart/items/${foodId}?quantity=${quantity}`, { method: 'PUT' }),
  removeFromCart: (foodId: number) =>
    request<Cart>(`/cart/items/${foodId}`, { method: 'DELETE' }),
  orders: () => request<Order[]>('/orders'),
  adminOrders: () => request<Order[]>('/orders/admin'),
  checkout: (deliveryAddress: string) =>
    request<Order>('/orders', { method: 'POST', body: JSON.stringify({ deliveryAddress }) }),
  updateOrderStatus: (id: number, status: OrderStatus) =>
    request<Order>(`/orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  createFood: (food: Omit<Food, 'id'>) =>
    request<Food>('/foods', { method: 'POST', body: JSON.stringify(food) }),
  updateFood: (food: Food) =>
    request<Food>(`/foods/${food.id}`, { method: 'PUT', body: JSON.stringify(food) }),
}
