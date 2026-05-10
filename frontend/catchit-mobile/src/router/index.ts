import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../viewmodels'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ========== AUTH ROUTES ==========
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/signup',
      name: 'signup',
      component: () => import('../views/SignupView.vue'),
    },

    // ========== MAIN APP ROUTES ==========
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== MAP ROUTE ==========
    {
      path: '/map',
      name: 'map',
      component: () => import('../views/MapView.vue'),
      meta: { keepAlive: true },
    },



    // ========== CARD ROUTES ==========
    {
      path: '/cards',
      name: 'cards',
      component: () => import('../views/StoreView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== CHECKOUT ROUTES ==========
    {
      path: '/cart',
      name: 'cart',
      component: () => import('../views/CartView.vue'),
      meta: { requiresAuth: true },
    },

    {
      path: '/checkout',
      name: 'checkout',
      component: () => import('../views/CheckoutView.vue'),
      meta: { requiresAuth: true },
    },

    {
      path: '/checkout-success/:orderId',
      name: 'checkout-success',
      component: () => import('../views/CheckoutSuccessView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== USER ROUTES ==========
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },

    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('../views/NotificationsView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== REDIRECT ROUTES ==========
    {
      path: '/',
      redirect: () => {
        return isAuthenticated.value ? '/home' : '/login'
      },
    },

    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

// ========== NAVIGATION GUARDS ==========
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (requiresAuth && !isAuthenticated.value) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/signup') && isAuthenticated.value) {
    next('/home')
  } else {
    next()
  }
})

export default router
