<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1>CatchIt</h1>
      <p class="auth-subtitle">Access your account</p>

      <form @submit.prevent="handleLogin" class="auth-form">
        <!-- Email Field -->
        <div class="form-group">
          <label class="form-icon"><Mail class="icon-sm" /></label>
          <input
            v-model="form.email"
            type="email"
            placeholder="Email Address"
            class="form-input"
            required
          />
        </div>

        <!-- Password Field -->
        <div class="form-group">
          <label class="form-icon"><Lock class="icon-sm" /></label>
          <input
            v-model="form.password"
            type="password"
            placeholder="Password"
            class="form-input"
            required
          />
        </div>

        <!-- Error Message -->
        <div v-if="error" class="error-message">{{ error }}</div>

        <!-- Login Button -->
        <button type="submit" class="btn-primary" :disabled="isLoading">
          {{ isLoading ? 'Logging in...' : 'Log In' }}
        </button>

        <!-- Forgot Password -->
        <a href="#" class="forgot-password">Forgot your password?</a>
      </form>

      <!-- Divider -->
      <div class="divider">or</div>

      <!-- Social Login -->
      <button class="btn-social btn-google">
        <Chrome class="icon-sm" /> Continue with Google
      </button>
      <button class="btn-social btn-apple">
        <Apple class="icon-sm" /> Continue with Apple
      </button>

      <!-- Terms -->
      <p class="terms">
        By clicking continue, you agree to our
        <a href="#" class="link">Terms of Service</a>
        and
        <a href="#" class="link">Privacy Policy</a>
      </p>

      <!-- Signup Link -->
      <p class="auth-link">
        Don't have an account?
        <router-link to="/signup" class="link">Create Account</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Apple, Chrome, Lock, Mail } from 'lucide-vue-next'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthViewModel } from '../viewmodels'

const router = useRouter()
const { login, isLoading, error } = useAuthViewModel()

const form = ref({
  email: 'maria@example.com', // Pre-filled for testing
  password: 'password', // Pre-filled for testing
})

const handleLogin = async () => {
  const success = await login(form.value.email, form.value.password)
  if (success) {
    void router.push('/home')
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 1rem;
}

.auth-card {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

h1 {
  font-size: 2rem;
  text-align: center;
  margin-bottom: 0.5rem;
  color: #333;
}

.auth-subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-group {
  position: relative;
}

.form-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
}

.form-input {
  width: 100%;
  padding: 12px 12px 12px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.error-message {
  color: #f44336;
  font-size: 0.875rem;
  padding: 0.75rem;
  background: #ffebee;
  border-radius: 4px;
}

.btn-primary {
  background: #000;
  color: white;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover:not(:disabled) {
  background: #333;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.forgot-password {
  text-align: center;
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
}

.forgot-password:hover {
  text-decoration: underline;
}

.divider {
  text-align: center;
  color: #999;
  margin: 1.5rem 0;
  position: relative;
}

.divider::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: #e0e0e0;
  z-index: -1;
}

.divider {
  display: flex;
  align-items: center;
  justify-content: center;
}

.divider::after {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 100%;
  height: 1px;
  background: #e0e0e0;
}

.btn-social {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-sm {
  width: 1rem;
  height: 1rem;
}

.btn-social:hover {
  background: #f5f5f5;
}

.btn-google {
  color: #1f2937;
}

.btn-apple {
  color: #000;
}

.terms {
  font-size: 0.75rem;
  text-align: center;
  color: #999;
  margin: 1rem 0;
}

.link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
}

.link:hover {
  text-decoration: underline;
}

.auth-link {
  text-align: center;
  font-size: 0.95rem;
  color: #666;
}
</style>
