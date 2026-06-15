import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  const generateUniqueEmail = () => `test_${Date.now()}_${Math.random().toString(36).substring(2, 8)}@example.com`;

  // ==================== TESTES DE LOGIN ====================

  test('should display login page', async ({ page }) => {
    await page.goto('/login');
    
    await expect(page.locator('.logo')).toBeVisible();
    await expect(page.getByPlaceholder('Email Address')).toBeVisible();
    await expect(page.getByPlaceholder('Password').first()).toBeVisible();  // .first() para evitar strict mode
    await expect(page.getByRole('button', { name: 'Log In' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Create Account' })).toBeVisible();
  });

  test('should login with valid admin credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email Address').fill('admin@catchit.pt');
    await page.getByPlaceholder('Password').first().fill('admin123');
    await page.getByRole('button', { name: 'Log In' }).click();
    
    await expect(page).toHaveURL(/admin\/reports|home/);
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email Address').fill('invalid@email.com');
    await page.getByPlaceholder('Password').first().fill('wrongpassword');
    await page.getByRole('button', { name: 'Log In' }).click();
    
    await expect(page.locator('.error-message')).toBeVisible();
  });

  test('should navigate to signup page from login', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('link', { name: 'Create Account' }).click();
    await expect(page).toHaveURL('/signup');
    await expect(page.getByText('Create an account')).toBeVisible();
  });

  // ==================== TESTES DE REGISTO ====================

  test('should display signup page', async ({ page }) => {
    await page.goto('/signup');
    
    await expect(page.getByText('Create an account')).toBeVisible();
    await expect(page.getByPlaceholder('Full Name')).toBeVisible();
    await expect(page.getByPlaceholder('Email Address')).toBeVisible();
    await expect(page.getByPlaceholder('Phone Number')).toBeVisible();
    // Usar .first() para o campo Password
    await expect(page.getByPlaceholder('Password').first()).toBeVisible();
    await expect(page.getByPlaceholder('Confirm Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create Account' })).toBeVisible();
  });

  test('should create a new account successfully', async ({ page }) => {
    const uniqueEmail = generateUniqueEmail();
    const userName = `Test User`;
    const password = 'Test123456!';
    
    await page.goto('/signup');
    
    await page.getByPlaceholder('Full Name').fill(userName);
    await page.getByPlaceholder('Email Address').fill(uniqueEmail);
    await page.getByPlaceholder('Phone Number').fill('912345678');
    await page.getByPlaceholder('Password').first().fill(password);
    await page.getByPlaceholder('Confirm Password').fill(password);
    
    await page.getByRole('button', { name: 'Create Account' }).click();
    
    await expect(page).toHaveURL('/home');
  });

  test('should show alert when passwords do not match', async ({ page }) => {
    // Configurar handler para o alerta ANTES de clicar no botão
    page.on('dialog', async (dialog) => {
      expect(dialog.message()).toContain('Passwords do not match');
      await dialog.accept();
    });
    
    await page.goto('/signup');
    
    await page.getByPlaceholder('Full Name').fill('Test User');
    await page.getByPlaceholder('Email Address').fill('test@example.com');
    await page.getByPlaceholder('Phone Number').fill('912345678');
    await page.getByPlaceholder('Password').first().fill('password123');
    await page.getByPlaceholder('Confirm Password').fill('different456');
    
    await page.getByRole('button', { name: 'Create Account' }).click();
  });

  test('should navigate back to login from signup', async ({ page }) => {
    await page.goto('/signup');
    
    await page.getByRole('link', { name: 'Log In' }).click();
    await expect(page).toHaveURL('/login');
    await expect(page.getByText('Access your account')).toBeVisible();
  });

  // ==================== TESTES COM ADMIN ====================

  test('admin should access reports page', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('Email Address').fill('admin@catchit.pt');
    await page.getByPlaceholder('Password').first().fill('admin123');
    await page.getByRole('button', { name: 'Log In' }).click();
    
    await expect(page).toHaveURL('/admin/reports');
    await expect(page.getByText('Admin Reports')).toBeVisible();
  });
});