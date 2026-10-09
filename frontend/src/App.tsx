import {
  createContext, useCallback, useContext, useEffect, useMemo, useState,
  type FormEvent, type ReactNode,
} from 'react'
import {
  ArrowRight, Bike, Check, ChefHat, ChevronDown, Clock3, Heart, Leaf, LogOut,
  MapPin, Menu as MenuIcon, Minus, PackageCheck, Plus, Search, ShieldCheck,
  ShoppingBag, ShoppingCart, Sparkles, UserRound, Utensils, X,
} from 'lucide-react'
import type { ChangeEvent } from 'react'
import {
  api, ApiError, saveToken, type Cart, type Food, type FoodPage,
  type Order, type OrderStatus, type User,
} from './api'
import {
  BrowserRouter, Link, NavLink, Navigate, Outlet, Route, Routes,
  useLocation, useNavigate, useParams,
} from 'react-router-dom'
import './App.css'

type AuthValue = {
  user: User | null
  ready: boolean
  signIn: (user: User, token: string) => void
  signOut: () => void
  notify: (message: string, kind?: 'success' | 'error') => void
}

const AuthContext = createContext<AuthValue | null>(null)

function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside AuthProvider')
  return value
}

function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('foodorder-user')
    return saved ? JSON.parse(saved) as User : null
  })
  const [ready, setReady] = useState(false)
  const [toast, setToast] = useState<{ message: string; kind: 'success' | 'error' } | null>(null)

  useEffect(() => {
    if (!localStorage.getItem('foodorder-token')) {
      setUser(null)
      localStorage.removeItem('foodorder-user')
      setReady(true)
      return
    }
    api.me()
      .then((current) => {
        setUser(current)
        localStorage.setItem('foodorder-user', JSON.stringify(current))
      })
      .catch(() => {
        saveToken(null)
        localStorage.removeItem('foodorder-user')
        setUser(null)
      })
      .finally(() => setReady(true))
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), 3200)
    return () => window.clearTimeout(timer)
  }, [toast])

  useEffect(() => {
    const handleExpiredSession = () => {
      saveToken(null)
      localStorage.removeItem('foodorder-user')
      setUser(null)
      setToast({ message: 'Your session expired. Please sign in again.', kind: 'error' })
    }
    window.addEventListener('foodorder-session-expired', handleExpiredSession)
    return () => window.removeEventListener('foodorder-session-expired', handleExpiredSession)
  }, [])

  const signIn = useCallback((nextUser: User, token: string) => {
    saveToken(token)
    localStorage.setItem('foodorder-user', JSON.stringify(nextUser))
    setUser(nextUser)
  }, [])
  const signOut = useCallback(() => {
    saveToken(null)
    localStorage.removeItem('foodorder-user')
    setUser(null)
  }, [])
  const notify = useCallback((message: string, kind: 'success' | 'error' = 'success') => {
    setToast({ message, kind })
  }, [])

  const value = useMemo(() => ({ user, ready, signIn, signOut, notify }), [user, ready, signIn, signOut, notify])
  return (
    <AuthContext.Provider value={value}>
      {children}
      {toast && (
        <div className={`toast toast-${toast.kind}`} role="status">
          {toast.kind === 'success' ? <Check size={17} /> : <X size={17} />}
          {toast.message}
        </div>
      )}
    </AuthContext.Provider>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<StoreLayout />}>
            <Route index element={<HomePage />} />
            <Route path="foods" element={<MenuPage />} />
            <Route path="foods/:id" element={<FoodPageView />} />
            <Route path="login" element={<AuthPage mode="login" />} />
            <Route path="register" element={<AuthPage mode="register" />} />
            <Route element={<RequireAuth />}>
              <Route path="cart" element={<CartPage />} />
              <Route path="orders" element={<OrdersPage />} />
              <Route path="orders/success/:id" element={<OrderSuccess />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route element={<RequireAdmin />}>
                <Route path="admin" element={<AdminPage />} />
              </Route>
            </Route>
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

function StoreLayout() {
  const { user, signOut, notify } = useAuth()
  const [cartCount, setCartCount] = useState(0)
  const [mobileOpen, setMobileOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const refreshCartCount = useCallback(() => {
    if (!user) {
      setCartCount(0)
      return
    }
    api.cart().then((cart) => setCartCount(cart.itemCount)).catch(() => setCartCount(0))
  }, [user])

  useEffect(() => {
    refreshCartCount()
  }, [refreshCartCount, location.pathname])

  useEffect(() => {
    window.addEventListener('foodorder-cart-updated', refreshCartCount)
    return () => window.removeEventListener('foodorder-cart-updated', refreshCartCount)
  }, [refreshCartCount])

  const logout = () => {
    signOut()
    notify('You have signed out.')
    navigate('/')
  }

  return (
    <>
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/" aria-label="FoodOrder home">
            <span className="brand-mark"><Utensils size={19} strokeWidth={2.8} /></span>
            <span>Food<span className="brand-accent">Order</span></span>
          </Link>
          <nav className={`main-nav ${mobileOpen ? 'nav-open' : ''}`} aria-label="Main navigation">
            <NavLink to="/" onClick={() => setMobileOpen(false)}>Home</NavLink>
            <NavLink to="/foods" onClick={() => setMobileOpen(false)}>Restaurants &amp; food</NavLink>
            <a href="/#categories" onClick={() => setMobileOpen(false)}>Categories</a>
            {user?.role === 'ADMIN' && <NavLink to="/admin" onClick={() => setMobileOpen(false)}>Manage</NavLink>}
          </nav>
          <div className="header-actions">
            <Link className="icon-button header-search" to="/foods" aria-label="Search food"><Search size={19} /></Link>
            <Link className="icon-button cart-link" to="/cart" aria-label={`Cart, ${cartCount} items`}>
              <ShoppingBag size={19} /><span className="cart-count">{cartCount}</span>
            </Link>
            {user ? (
              <div className="account-menu">
                <Link className="avatar" to="/profile" aria-label="Your account">{user.firstName.slice(0, 1).toUpperCase()}</Link>
                <div className="account-dropdown">
                  <Link to="/profile"><UserRound size={16} /> My profile</Link>
                  <Link to="/orders"><PackageCheck size={16} /> My orders</Link>
                  <button type="button" onClick={logout}><LogOut size={16} /> Sign out</button>
                </div>
              </div>
            ) : (
              <Link className="button button-primary header-login" to="/login">Sign in</Link>
            )}
            <button className="icon-button mobile-menu-button" aria-label="Toggle navigation" onClick={() => setMobileOpen(!mobileOpen)}>
              {mobileOpen ? <X size={21} /> : <MenuIcon size={21} />}
            </button>
          </div>
        </div>
      </header>
      <main><Outlet context={{ refreshCartCount }} /></main>
      <Footer />
    </>
  )
}

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <Link className="brand footer-brand" to="/"><span className="brand-mark"><Utensils size={17} /></span>Food<span className="brand-accent">Order</span></Link>
        <p>Good food, good mood. Delivered with care.</p>
        <span>© {new Date().getFullYear()} FoodOrder</span>
      </div>
    </footer>
  )
}

function FoodCard({ food, compact = false }: { food: Food; compact?: boolean }) {
  const { user, notify } = useAuth()
  const navigate = useNavigate()
  const [adding, setAdding] = useState(false)
  const add = async () => {
    if (!user) {
      notify('Sign in to add items to your cart.', 'error')
      navigate('/login')
      return
    }
    setAdding(true)
    try {
      await api.addToCart(food.id)
      notify(`${food.name} added to your cart.`)
    } catch (error) {
      notify(error instanceof Error ? error.message : 'Could not add this item.', 'error')
    } finally {
      setAdding(false)
    }
  }
  return (
    <article className={`food-card ${compact ? 'food-card-compact' : ''}`}>
      <Link className="food-image-wrap" to={`/foods/${food.id}`}>
        <img className="food-image" src={food.imageUrl} alt={food.name} loading="lazy" />
        {food.vegetarian && <span className="veg-dot" aria-label="Vegetarian"><span /></span>}
        {!food.available && <span className="unavailable-tag">Sold out</span>}
        <button className="favorite-button" type="button" aria-label={`View ${food.name}`} onClick={(event) => {
          event.preventDefault()
          navigate(`/foods/${food.id}`)
        }}><Heart size={17} /></button>
      </Link>
      <div className="food-card-body">
        <div className="food-card-title-row">
          <Link to={`/foods/${food.id}`} className="food-title">{food.name}</Link>
          <span className="dish-category">{food.category}</span>
        </div>
        <p className="food-description">{food.description}</p>
        <div className="food-card-bottom">
          <strong className="price">{money(food.price)}</strong>
          <button className="add-small" type="button" disabled={!food.available || adding} onClick={add}>
            {adding ? '…' : <><Plus size={17} /> Add</>}
          </button>
        </div>
      </div>
    </article>
  )
}

function HomePage() {
  const [foods, setFoods] = useState<Food[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const categories = [
    { name: 'Burgers', icon: '🍔' }, { name: 'Pizza', icon: '🍕' },
    { name: 'South Indian', icon: '🥘' }, { name: 'Asian', icon: '🍜' },
    { name: 'Desserts', icon: '🍰' }, { name: 'Healthy', icon: '🥗' },
    { name: 'Snacks', icon: '🍟' },
  ]

  useEffect(() => {
    api.foods(new URLSearchParams({ size: '8', available: 'true' }))
      .then((page) => setFoods(page.items))
      .catch((cause) => setError(errorText(cause)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="page-shell">
      <section className="hero">
        <div className="hero-content">
          <span className="eyebrow"><Sparkles size={14} /> A little joy, delivered</span>
          <h1>Delicious food<br />delivered to <span>your door.</span></h1>
          <p>Your next favorite meal is just a few clicks away.</p>
          <form className="hero-search" onSubmit={(event) => {
            event.preventDefault()
            const data = new FormData(event.currentTarget)
            navigate(`/foods?search=${encodeURIComponent(String(data.get('search') ?? ''))}`)
          }}>
            <Search size={19} />
            <input name="search" aria-label="Search restaurants and dishes" placeholder="Search dishes, cuisines..." />
            <button className="button button-primary" type="submit">Find food <ArrowRight size={16} /></button>
          </form>
          <div className="hero-promise"><span><Check size={15} /> Fresh picks</span><span><Check size={15} /> Easy checkout</span><span><Check size={15} /> Order tracking</span></div>
        </div>
        <div className="hero-photo" aria-hidden="true">
          <div className="hero-note"><span className="hero-note-icon">✦</span><div><strong>Made with care</strong><small>From our kitchen to yours</small></div></div>
        </div>
        <span className="hero-orbit orbit-one" /><span className="hero-orbit orbit-two" />
      </section>

      <section className="content-section category-section" id="categories">
        <div className="section-heading">
          <div><span className="section-kicker">What are you craving?</span><h2>Browse by category</h2></div>
          <Link className="text-link" to="/foods">See all food <ArrowRight size={16} /></Link>
        </div>
        <div className="category-list">
          {categories.map((category) => (
            <Link to={`/foods?category=${encodeURIComponent(category.name)}`} className="category-tile" key={category.name}>
              <span className="category-emoji">{category.icon}</span><span>{category.name}</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="content-section">
        <div className="section-heading">
          <div><span className="section-kicker">Picked for you</span><h2>Customer favorites</h2><p>Fan-favorite plates, made fresh and ready to go.</p></div>
          <Link className="text-link" to="/foods">Explore the menu <ArrowRight size={16} /></Link>
        </div>
        {error ? <ErrorPanel message={error} onRetry={() => window.location.reload()} /> : loading ? <Loading /> :
          foods.length ? <div className="food-grid">{foods.slice(0, 4).map((food) => <FoodCard key={food.id} food={food} />)}</div> :
            <EmptyState icon={<Utensils />} title="The menu is getting ready" text="Check back soon for delicious new dishes." />}
      </section>

      <section className="benefit-strip">
        <div><span className="benefit-icon"><Leaf size={22} /></span><strong>Fresh ingredients</strong><small>Good food starts with great produce.</small></div>
        <div><span className="benefit-icon"><Bike size={22} /></span><strong>Fast delivery</strong><small>From our kitchen straight to you.</small></div>
        <div><span className="benefit-icon"><ChefHat size={22} /></span><strong>Made with care</strong><small>Every order prepared just for you.</small></div>
        <div><span className="benefit-icon"><ShieldCheck size={22} /></span><strong>Secure checkout</strong><small>Your account stays yours.</small></div>
      </section>
    </div>
  )
}

function MenuPage() {
  const location = useLocation()
  const [items, setItems] = useState<Food[]>([])
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [onlyVeg, setOnlyVeg] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState<FoodPage | null>(null)

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    setSearch(params.get('search') ?? '')
    setCategory(params.get('category') ?? '')
  }, [location.search])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setLoading(true)
      setError('')
      const params = new URLSearchParams({ page: '0', size: '24', available: 'true' })
      if (search.trim()) params.set('search', search.trim())
      if (category) params.set('category', category)
      api.foods(params)
        .then((result) => { setPage(result); setItems(result.items) })
        .catch((cause) => setError(errorText(cause)))
        .finally(() => setLoading(false))
    }, 180)
    return () => window.clearTimeout(timer)
  }, [search, category])

  const categories = ['All dishes', 'Burgers', 'Pizza', 'South Indian', 'Asian', 'Desserts', 'Healthy', 'Snacks']
  const filtered = onlyVeg ? items.filter((item) => item.vegetarian) : items

  return (
    <div className="page-shell inner-page">
      <div className="page-title-row">
        <div><span className="section-kicker">The good stuff</span><h1>Explore our menu</h1><p>Find something delicious for every kind of craving.</p></div>
        <span className="menu-count">{page?.totalItems ?? 0} dishes</span>
      </div>
      <div className="catalog-toolbar">
        <label className="search-field"><Search size={18} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search dishes..." /><kbd>⌘ K</kbd></label>
        <label className="veg-filter"><input type="checkbox" checked={onlyVeg} onChange={(event) => setOnlyVeg(event.target.checked)} /><span className="veg-dot"><span /></span> Vegetarian</label>
      </div>
      <div className="category-pills">
        {categories.map((item) => <button className={`filter-pill ${category === (item === 'All dishes' ? '' : item) ? 'active' : ''}`} key={item} onClick={() => setCategory(item === 'All dishes' ? '' : item)}>{item}</button>)}
      </div>
      {error ? <ErrorPanel message={error} onRetry={() => setSearch(`${search} `)} /> : loading ? <Loading /> :
        filtered.length ? <div className="food-grid">{filtered.map((food) => <FoodCard key={food.id} food={food} />)}</div> :
          <EmptyState icon={<Search />} title="No dishes found" text="Try another search or pick a different category." onAction={() => { setSearch(''); setCategory(''); setOnlyVeg(false) }} actionLabel="Clear filters" />}
    </div>
  )
}

function FoodPageView() {
  const { id = '' } = useParams()
  const { user, notify } = useAuth()
  const navigate = useNavigate()
  const [food, setFood] = useState<Food | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    api.food(id).then(setFood).catch((cause) => notify(errorText(cause), 'error')).finally(() => setLoading(false))
  }, [id, notify])
  if (loading) return <div className="page-shell inner-page"><Loading /></div>
  if (!food) return <div className="page-shell inner-page"><EmptyState title="Dish not found" text="This item may have been removed from the menu." actionLabel="Back to menu" onAction={() => navigate('/foods')} /></div>
  return (
    <div className="page-shell inner-page">
      <Link className="back-link" to="/foods">← Back to menu</Link>
      <section className="detail-layout">
        <div className="detail-image-wrap"><img src={food.imageUrl} alt={food.name} /></div>
        <div className="detail-content">
          <span className="tag tag-orange">{food.category}</span>
          <h1>{food.name}</h1>
          <div className="detail-rating"><span>Freshly prepared</span>{food.vegetarian && <span className="veg-dot"><span /></span>}</div>
          <p className="detail-description">{food.description}</p>
          <div className="detail-perks"><span><Clock3 size={17} /> 25–35 min</span><span><MapPin size={17} /> Delivered to your door</span></div>
          <div className="detail-order"><strong>{money(food.price)}</strong><button className="button button-primary" disabled={!food.available} onClick={async () => {
            if (!user) { notify('Sign in to add items to your cart.', 'error'); navigate('/login'); return }
            try { await api.addToCart(food.id); notify(`${food.name} added to your cart.`) }
            catch (error) { notify(errorText(error), 'error') }
          }}><ShoppingCart size={18} /> {food.available ? 'Add to cart' : 'Sold out'}</button></div>
        </div>
      </section>
    </div>
  )
}

function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [address, setAddress] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const { notify } = useAuth()
  const navigate = useNavigate()

  const load = useCallback(() => api.cart().then(setCart).catch((cause) => setError(errorText(cause))).finally(() => setLoading(false)), [])
  useEffect(() => { load() }, [load])

  const update = async (id: number, quantity: number) => {
    if (!cart) return
    const previous = cart
    setUpdating(true)
    setCart({
      ...cart,
      items: quantity === 0 ? cart.items.filter((line) => line.foodId !== id) :
        cart.items.map((line) => line.foodId === id ? { ...line, quantity, lineTotal: line.unitPrice * quantity } : line),
      itemCount: cart.itemCount + quantity - (cart.items.find((line) => line.foodId === id)?.quantity ?? 0),
      subtotal: cart.subtotal + (quantity - (cart.items.find((line) => line.foodId === id)?.quantity ?? 0)) *
        (cart.items.find((line) => line.foodId === id)?.unitPrice ?? 0),
    })
    try { setCart(await api.setCartQuantity(id, quantity)) }
    catch (cause) { setCart(previous); notify(errorText(cause), 'error') }
    finally { setUpdating(false) }
  }

  const checkout = async (event: FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    try {
      const order = await api.checkout(address)
      notify('Your order is on its way to the kitchen!')
      navigate(`/orders/success/${order.id}`)
    } catch (cause) { notify(errorText(cause), 'error') }
    finally { setSubmitting(false) }
  }

  if (loading) return <div className="page-shell inner-page"><Loading /></div>
  if (error) return <div className="page-shell inner-page"><ErrorPanel message={error} onRetry={load} /></div>
  if (!cart?.items.length) return <div className="page-shell inner-page"><EmptyState icon={<ShoppingBag />} title="Your cart is taking a break" text="Find a dish you love and it will show up here." actionLabel="Explore the menu" onAction={() => navigate('/foods')} /></div>

  return (
    <div className="page-shell inner-page">
      <div className="page-title-row"><div><span className="section-kicker">Almost there</span><h1>Your cart</h1><p>{cart.itemCount} delicious {cart.itemCount === 1 ? 'item' : 'items'} in your bag</p></div><Link className="text-link" to="/foods">Continue browsing <ArrowRight size={16} /></Link></div>
      <div className="cart-layout">
        <section className="cart-list">
          {cart.items.map((item) => (
            <article className="cart-item" key={item.foodId}>
              <img src={item.imageUrl} alt="" />
              <div className="cart-item-info"><strong>{item.name}</strong><span>{money(item.unitPrice)} each</span><button className="remove-link" onClick={() => update(item.foodId, 0)}>Remove</button></div>
              <div className="quantity-control"><button aria-label={`Remove one ${item.name}`} disabled={updating} onClick={() => update(item.foodId, item.quantity - 1)}><Minus size={14} /></button><span>{item.quantity}</span><button aria-label={`Add one ${item.name}`} disabled={updating || item.quantity >= 20} onClick={() => update(item.foodId, item.quantity + 1)}><Plus size={14} /></button></div>
              <strong className="cart-line-total">{money(item.lineTotal)}</strong>
            </article>
          ))}
          <Link className="text-link cart-continue" to="/foods">← Add more good stuff</Link>
        </section>
        <aside className="checkout-card">
          <h2>Order summary</h2>
          <div className="summary-row"><span>Subtotal</span><span>{money(cart.subtotal)}</span></div>
          <div className="summary-row"><span>Delivery</span><span className="free-delivery">Free</span></div>
          <div className="summary-total"><strong>Total</strong><strong>{money(cart.subtotal)}</strong></div>
          <form onSubmit={checkout}>
            <label className="field-label" htmlFor="delivery-address">Delivery address</label>
            <textarea id="delivery-address" required maxLength={300} value={address} onChange={(event) => setAddress(event.target.value)} placeholder="Street, area, city and postal code" rows={3} />
            <button className="button button-primary checkout-button" disabled={submitting || cart.items.some((item) => !item.available)}>
              {submitting ? 'Placing order…' : <>Place order <ArrowRight size={17} /></>}
            </button>
          </form>
          <p className="checkout-secure"><ShieldCheck size={15} /> Secure checkout · No payment collected</p>
        </aside>
      </div>
    </div>
  )
}

function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { api.orders().then(setOrders).catch((cause) => setError(errorText(cause))).finally(() => setLoading(false)) }, [])
  if (loading) return <div className="page-shell inner-page"><Loading /></div>
  return (
    <div className="page-shell inner-page">
      <div className="page-title-row"><div><span className="section-kicker">Every bite, accounted for</span><h1>Your orders</h1><p>Keep an eye on the good things coming your way.</p></div></div>
      {error ? <ErrorPanel message={error} onRetry={() => window.location.reload()} /> : orders.length ? (
        <div className="orders-list">{orders.map((order) => (
          <article className="order-card" key={order.id}>
            <div className="order-card-top"><div><span className="order-number">Order #{String(order.id).padStart(5, '0')}</span><span className="order-date">{formatDate(order.createdAt)}</span></div><StatusBadge status={order.status} /></div>
            <div className="order-card-items">{order.items.map((item, index) => <span key={`${item.name}-${index}`}>{item.quantity} × {item.name}</span>)}</div>
            <div className="order-card-bottom"><span><MapPin size={15} /> {order.deliveryAddress}</span><strong>{money(order.totalAmount)}</strong></div>
          </article>
        ))}</div>
      ) : <EmptyState icon={<PackageCheck />} title="No orders just yet" text="Your delicious food adventures will show up here." actionLabel="Explore the menu" onAction={() => { window.location.assign('/foods') }} />}
    </div>
  )
}

function OrderSuccess() {
  const { id } = useParams()
  return (
    <div className="page-shell success-shell">
      <div className="success-card"><span className="success-icon"><Check size={35} /></span><span className="section-kicker">That was easy</span><h1>Your order is in!</h1><p>Order <strong>#{String(id).padStart(5, '0')}</strong> has been sent to the kitchen. We’ll keep you posted as it’s prepared.</p><div className="success-actions"><Link className="button button-primary" to="/orders">Track your order</Link><Link className="button button-light" to="/foods">Order something else</Link></div></div>
      <div className="success-illustration">🥗 <span>🍅</span> 🍋</div>
    </div>
  )
}

function StatusBadge({ status }: { status: OrderStatus }) {
  const labels: Record<OrderStatus, string> = {
    PLACED: 'Placed', CONFIRMED: 'Confirmed', PREPARING: 'Preparing',
    OUT_FOR_DELIVERY: 'On the way', DELIVERED: 'Delivered', CANCELLED: 'Cancelled',
  }
  return <span className={`status-badge status-${status.toLowerCase()}`}><span />{labels[status]}</span>
}

function ProfilePage() {
  const { user, notify, signIn } = useAuth()
  const [firstName, setFirstName] = useState(user?.firstName ?? '')
  const [lastName, setLastName] = useState(user?.lastName ?? '')
  const [mobile, setMobile] = useState(user?.mobile ?? '')
  const [saving, setSaving] = useState(false)
  const save = async (event: FormEvent) => {
    event.preventDefault()
    setSaving(true)
    try {
      const updated = await api.updateProfile({ firstName, lastName, mobile })
      const token = localStorage.getItem('foodorder-token') ?? ''
      signIn(updated, token)
      notify('Your profile has been updated.')
    } catch (cause) { notify(errorText(cause), 'error') }
    finally { setSaving(false) }
  }
  return (
    <div className="page-shell inner-page profile-page">
      <div className="page-title-row"><div><span className="section-kicker">Your account</span><h1>My profile</h1><p>A few details to make every order feel like yours.</p></div></div>
      <div className="profile-layout">
        <aside className="profile-sidebar"><span className="profile-avatar">{user?.firstName.slice(0, 1).toUpperCase()}</span><h2>{user?.firstName} {user?.lastName}</h2><span>{user?.email}</span><nav><NavLink to="/profile"><UserRound size={17} /> Personal details</NavLink><NavLink to="/orders"><PackageCheck size={17} /> My orders</NavLink></nav></aside>
        <form className="profile-form" onSubmit={save}><div className="form-heading"><div><h2>Personal details</h2><p>Update your name or phone number.</p></div><span className="profile-role"><ShieldCheck size={15} /> {user?.role === 'ADMIN' ? 'Administrator' : 'Customer'}</span></div>
          <div className="form-two-columns"><label className="field-label">First name<input required maxLength={60} value={firstName} onChange={(event) => setFirstName(event.target.value)} /></label><label className="field-label">Last name<input required maxLength={60} value={lastName} onChange={(event) => setLastName(event.target.value)} /></label></div>
          <label className="field-label">Email address<input value={user?.email ?? ''} disabled /><small>Email is used for signing in and cannot be changed here.</small></label>
          <label className="field-label">Mobile number<input required pattern="[+]?[0-9 ()\x2d]{7,20}" value={mobile} onChange={(event) => setMobile(event.target.value)} /></label>
          <button className="button button-primary profile-save" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
        </form>
      </div>
    </div>
  )
}

function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const isRegister = mode === 'register'
  const { user, signIn, notify } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [values, setValues] = useState({ firstName: '', lastName: '', mobile: '', email: '', password: '', confirmPassword: '' })

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />
  const update = (key: keyof typeof values) => (event: ChangeEvent<HTMLInputElement>) =>
    setValues((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (isRegister && values.password !== values.confirmPassword) {
      setError('Your passwords do not match.')
      return
    }
    setLoading(true)
    try {
      const result = isRegister
        ? await api.register({
          firstName: values.firstName, lastName: values.lastName, mobile: values.mobile,
          email: values.email, password: values.password,
        })
        : await api.login(values.email, values.password)
      signIn(result.user, result.token)
      notify(isRegister ? 'Welcome to FoodOrder!' : `Welcome back, ${result.user.firstName}!`)
      navigate(result.user.role === 'ADMIN' ? '/admin' : '/')
    } catch (cause) { setError(errorText(cause)) }
    finally { setLoading(false) }
  }

  return (
    <div className="auth-shell">
      <section className="auth-panel">
        <Link className="brand auth-brand" to="/"><span className="brand-mark"><Utensils size={18} /></span>Food<span className="brand-accent">Order</span></Link>
        <div className="auth-title"><span className="section-kicker">{isRegister ? 'A seat at our table' : 'Good to see you again'}</span><h1>{isRegister ? 'Create your account' : 'Welcome back'}</h1><p>{isRegister ? 'Join us and make your next meal a little more special.' : 'Sign in to pick up right where you left off.'}</p></div>
        <form className="auth-form" onSubmit={submit}>
          {isRegister && <div className="form-two-columns"><label className="field-label">First name<input autoComplete="given-name" required maxLength={60} value={values.firstName} onChange={update('firstName')} placeholder="Aarav" /></label><label className="field-label">Last name<input autoComplete="family-name" required maxLength={60} value={values.lastName} onChange={update('lastName')} placeholder="Shah" /></label></div>}
          {isRegister && <label className="field-label">Mobile number<input type="tel" autoComplete="tel" required pattern="[+]?[0-9 \(\)\-]{7,20}" value={values.mobile} onChange={update('mobile')} placeholder="+91 98765 43210" /></label>}
          <label className="field-label">Email address<input type="email" autoComplete="email" required value={values.email} onChange={update('email')} placeholder="you@example.com" /></label>
          <label className="field-label">Password<input type="password" autoComplete={isRegister ? 'new-password' : 'current-password'} required minLength={isRegister ? 8 : 1} maxLength={72} value={values.password} onChange={update('password')} placeholder={isRegister ? 'At least 8 characters' : 'Enter your password'} />{isRegister && <small>Use 8+ characters with an uppercase letter, lowercase letter and number.</small>}</label>
          {isRegister && <label className="field-label">Confirm password<input type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={values.confirmPassword} onChange={update('confirmPassword')} placeholder="Type your password again" /></label>}
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-primary auth-submit" disabled={loading}>{loading ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'} <ArrowRight size={17} /></button>
        </form>
        <p className="auth-switch">{isRegister ? 'Already have an account?' : 'New to FoodOrder?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Sign in' : 'Create an account'}</Link></p>
        <div className="auth-note"><ShieldCheck size={16} /> Your details are encrypted and kept private.</div>
      </section>
      <aside className="auth-art"><div className="auth-art-copy"><span>YOUR NEXT FAVORITE</span><h2>Something<br />delicious is<br />on its way.</h2><p>Good food brings us together. Let’s make it a great meal.</p></div><div className="auth-art-badge"><span>✦</span> Freshly made<br />just for you</div></aside>
    </div>
  )
}

function AdminPage() {
  const [tab, setTab] = useState<'overview' | 'menu' | 'orders'>('overview')
  const [foods, setFoods] = useState<Food[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const { notify } = useAuth()
  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [foodData, orderData] = await Promise.all([api.foods(new URLSearchParams({ size: '50' })), api.adminOrders()])
      setFoods(foodData.items)
      setOrders(orderData)
    } catch (cause) { setError(errorText(cause)) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])
  const toggleAvailability = async (food: Food) => {
    try { await api.updateFood({ ...food, available: !food.available }); await load(); notify(`${food.name} is ${food.available ? 'unavailable' : 'available'} now.`) }
    catch (cause) { notify(errorText(cause), 'error') }
  }
  const saveFood = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    try {
      await api.createFood({
        name: String(data.get('name')), description: String(data.get('description')),
        category: String(data.get('category')), price: Number(data.get('price')),
        imageUrl: String(data.get('imageUrl')), available: true, vegetarian: data.get('vegetarian') === 'on',
      })
      setShowForm(false)
      await load()
      notify('The new dish is on the menu.')
    } catch (cause) { notify(errorText(cause), 'error') }
  }
  const setStatus = async (order: Order, status: OrderStatus) => {
    try { await api.updateOrderStatus(order.id, status); await load(); notify(`Order #${order.id} updated.`) }
    catch (cause) { notify(errorText(cause), 'error') }
  }
  const revenue = orders.reduce((total, order) => total + (order.status !== 'CANCELLED' ? order.totalAmount : 0), 0)

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar"><Link to="/" className="brand"><span className="brand-mark"><Utensils size={17} /></span>Food<span className="brand-accent">Order</span></Link><span className="admin-label">STORE MANAGEMENT</span><nav>
        <button className={tab === 'overview' ? 'selected' : ''} onClick={() => setTab('overview')}><span className="admin-nav-icon">◫</span> Dashboard</button>
        <button className={tab === 'menu' ? 'selected' : ''} onClick={() => setTab('menu')}><Utensils size={17} /> Food items</button>
        <button className={tab === 'orders' ? 'selected' : ''} onClick={() => setTab('orders')}><ShoppingBag size={17} /> Orders <span className="nav-count">{orders.filter((order) => !['DELIVERED', 'CANCELLED'].includes(order.status)).length}</span></button>
      </nav><div className="admin-sidebar-bottom"><ShieldCheck size={16} /> Administrator access</div></aside>
      <section className="admin-main">
        <header className="admin-topbar"><div><span>Store</span><ChevronDown size={15} /><span className="admin-topbar-sub">/ {tab === 'overview' ? 'Dashboard' : tab === 'menu' ? 'Food items' : 'Orders'}</span></div><Link className="button button-light" to="/">View storefront <ArrowRight size={15} /></Link></header>
        <div className="admin-content">
          {error && <ErrorPanel message={error} onRetry={load} />}
          {loading ? <Loading /> : tab === 'overview' ? (
            <>
              <div className="admin-page-heading"><div><span className="section-kicker">Your store at a glance</span><h1>Dashboard</h1><p>Welcome back. Here’s what’s happening today.</p></div><span className="admin-date">{new Date().toLocaleDateString('en', { weekday: 'long', month: 'long', day: 'numeric' })}</span></div>
              <div className="stats-grid"><StatCard icon={<ShoppingBag />} label="Total orders" value={orders.length.toString()} tone="orange" /><StatCard icon={<Utensils />} label="Menu items" value={foods.length.toString()} tone="green" /><StatCard icon={<Clock3 />} label="In progress" value={orders.filter((order) => !['DELIVERED', 'CANCELLED'].includes(order.status)).length.toString()} tone="purple" /><StatCard icon={<span className="rupee-icon">₹</span>} label="Order value" value={money(revenue)} tone="blue" /></div>
              <div className="admin-section-heading"><div><h2>Recent orders</h2><p>Your latest customer orders.</p></div><button className="text-link" onClick={() => setTab('orders')}>View all <ArrowRight size={15} /></button></div>
              <OrdersTable orders={orders.slice(0, 5)} onStatus={setStatus} />
            </>
          ) : tab === 'menu' ? (
            <>
              <div className="admin-page-heading"><div><span className="section-kicker">The dishes you serve</span><h1>Food items</h1><p>Add new dishes or manage what’s currently available.</p></div><button className="button button-primary" onClick={() => setShowForm(!showForm)}><Plus size={17} /> Add food item</button></div>
              {showForm && <form className="admin-add-form" onSubmit={saveFood}><div className="form-heading"><div><h2>Add a dish</h2><p>It will be visible in the customer menu after saving.</p></div><button type="button" className="icon-button" aria-label="Close form" onClick={() => setShowForm(false)}><X size={18} /></button></div><div className="form-two-columns"><label className="field-label">Name<input name="name" required maxLength={100} /></label><label className="field-label">Category<input name="category" required maxLength={40} placeholder="e.g. Pizza" /></label></div><label className="field-label">Description<input name="description" required maxLength={500} /></label><div className="form-two-columns"><label className="field-label">Price (₹)<input name="price" type="number" min="0.01" step="0.01" required /></label><label className="field-label">Photo URL<input name="imageUrl" type="url" required maxLength={500} placeholder="https://…" /></label></div><label className="checkbox-field"><input name="vegetarian" type="checkbox" /> Vegetarian</label><button className="button button-primary">Save food item</button></form>}
              <div className="admin-food-table"><table><thead><tr><th>ITEM</th><th>CATEGORY</th><th>PRICE</th><th>STATUS</th><th>ACTION</th></tr></thead><tbody>{foods.map((food) => <tr key={food.id}><td><div className="admin-food-name"><img src={food.imageUrl} alt="" /><span><strong>{food.name}</strong><small>{food.vegetarian ? 'Vegetarian' : 'Non-vegetarian'}</small></span></div></td><td>{food.category}</td><td>{money(food.price)}</td><td><span className={`availability ${food.available ? 'is-available' : 'is-unavailable'}`}>{food.available ? 'Available' : 'Hidden'}</span></td><td><button className="table-action" onClick={() => toggleAvailability(food)}>{food.available ? 'Hide' : 'Show'}</button></td></tr>)}</tbody></table>{!foods.length && <EmptyState title="No menu items yet" text="Add a dish to start building your menu." />}</div>
            </>
          ) : (
            <>
              <div className="admin-page-heading"><div><span className="section-kicker">Customer activity</span><h1>Orders</h1><p>Keep each order moving from kitchen to customer.</p></div></div>
              <OrdersTable orders={orders} onStatus={setStatus} />
            </>
          )}
        </div>
      </section>
    </div>
  )
}

function OrdersTable({ orders, onStatus }: { orders: Order[]; onStatus: (order: Order, status: OrderStatus) => void }) {
  if (!orders.length) return <EmptyState icon={<ShoppingBag />} title="No orders to show" text="New customer orders will appear here." />
  return <div className="admin-order-table"><table><thead><tr><th>ORDER</th><th>CUSTOMER</th><th>ITEMS</th><th>TOTAL</th><th>STATUS</th><th>UPDATE</th></tr></thead><tbody>{orders.map((order) => <tr key={order.id}><td><strong>#{String(order.id).padStart(5, '0')}</strong><small>{formatDate(order.createdAt)}</small></td><td><strong>{order.customerName}</strong><small>{order.customerEmail}</small></td><td>{order.items.reduce((sum, item) => sum + item.quantity, 0)} items</td><td>{money(order.totalAmount)}</td><td><StatusBadge status={order.status} /></td><td><select aria-label={`Update order ${order.id} status`} value={order.status} disabled={['DELIVERED', 'CANCELLED'].includes(order.status)} onChange={(event) => onStatus(order, event.target.value as OrderStatus)}>{['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].map((status) => <option key={status} value={status}>{status.replace(/_/g, ' ')}</option>)}</select></td></tr>)}</tbody></table></div>
}

function StatCard({ icon, label, value, tone }: { icon: ReactNode; label: string; value: string; tone: string }) {
  return <article className="stat-card"><span className={`stat-icon stat-${tone}`}>{icon}</span><div><small>{label}</small><strong>{value}</strong></div></article>
}

function RequireAuth() {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <div className="page-shell inner-page"><Loading /></div>
  return user ? <Outlet /> : <Navigate to="/login" state={{ from: location.pathname }} replace />
}

function RequireAdmin() {
  const { user } = useAuth()
  return user?.role === 'ADMIN' ? <Outlet /> : <Navigate to="/" replace />
}

function EmptyState({ icon, title, text, actionLabel, onAction }: {
  icon?: ReactNode; title: string; text: string; actionLabel?: string; onAction?: () => void
}) {
  return <div className="empty-state"><span className="empty-icon">{icon ?? <Utensils />}</span><h2>{title}</h2><p>{text}</p>{actionLabel && onAction && <button className="button button-light" onClick={onAction}>{actionLabel} <ArrowRight size={16} /></button>}</div>
}

function ErrorPanel({ message, onRetry }: { message: string; onRetry: () => void }) {
  return <div className="error-panel" role="alert"><div><strong>We hit a little snag</strong><p>{message}</p></div><button className="button button-light" onClick={onRetry}>Try again</button></div>
}

function Loading() {
  return <div className="loading-state" role="status"><span className="spinner" /> <span>Getting the good stuff ready…</span></div>
}

function NotFound() {
  const navigate = useNavigate()
  return <div className="page-shell inner-page"><EmptyState icon={<Search />} title="This page wandered off" text="Let's get you back to something delicious." actionLabel="Back to the menu" onAction={() => navigate('/')} /></div>
}

function money(amount: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(amount)
}

function formatDate(date: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date))
}

function errorText(error: unknown) {
  return error instanceof ApiError ? error.message : error instanceof Error ? error.message : 'Something went wrong. Please try again.'
}

export default App
