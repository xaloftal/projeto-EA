import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, currentUser } from '../viewmodels'

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
      meta: { keepAlive: true, requiresAuth: true },
    },
    {
      path: '/plantrip',
      name: 'plantrip',
      component: () => import('../views/PlanTripView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== SCHEDULE ROUTE ==========
    {
      path: '/schedule',
      name: 'schedule',
      component: () => import('../views/ScheduleView.vue'),
      meta: { requiresAuth: true },
    },

    // ========== CHECKIN ROUTE ==========

    {
      path: '/checkin/:titleId',
      name: 'checkin',
      component: () => import('../views/TransportCheckInView.vue'),
      meta: { requiresAuth: true },
    },

    {
      path: '/itinerary',
      name: 'itinerary',
      component: () => import('../views/ItineraryView.vue'),
      meta: { requiresAuth: true },
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
        if (!isAuthenticated.value) return '/login'
        return currentUser.value?.isAdmin ? '/admin/reports' : '/home'
      },
    },

    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },

    {
      path: '/history',
      name: 'history',
      component: () => import('../views/HistoryView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/reports',
      name: 'admin-reports',
      component: () => import('../views/AdminReportsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    }
  ],
})

// ========== NAVIGATION GUARDS ==========
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin)

  if (requiresAuth && !isAuthenticated.value) {
    next('/login')
  } else if (isAuthenticated.value && currentUser.value?.isAdmin && !requiresAdmin) {
    next('/admin/reports')
  } else if (requiresAdmin && (!currentUser.value || !currentUser.value.isAdmin)) {
    next('/home')
  } else if ((to.path === '/login' || to.path === '/signup') && isAuthenticated.value) {
    next(currentUser.value?.isAdmin ? '/admin/reports' : '/home')
  } else {
    next()
  }
})




export default router
