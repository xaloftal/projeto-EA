import { test, expect } from '@playwright/test';

test.describe('Check-in Flow', () => {

  test('should click check-in button, complete check-in and check-out', async ({ page }) => {
    const email = 'a@a';
    const password = '1';
    
    // Login
    await page.goto('/login');
    await page.getByPlaceholder('Email Address').fill(email);
    await page.getByPlaceholder('Password').first().fill(password);
    await page.getByRole('button', { name: 'Log in' }).click();
    await expect(page).toHaveURL('/home');
    await page.waitForTimeout(2000);
    
    // Ir aos tickets
    await page.goto('/home');
    await page.getByText('My Tickets').click();
    await page.waitForTimeout(2000);
    
    // Expandir primeiro grupo
    const firstGroup = page.locator('.ticket-group-header').first();
    await firstGroup.waitFor({ state: 'visible', timeout: 5000 });
    await firstGroup.click();
    await page.waitForTimeout(1000);
    
    // Clicar Check In
    const checkinButton = page.locator('.ticket-group-content .btn-checkin-small, .ticket-group-content a:has-text("Check In")').first();
    await checkinButton.waitFor({ state: 'visible', timeout: 5000 });
    await checkinButton.click();
    
    // Verificar página de check-in
    await expect(page).toHaveURL(/\/checkin\/.+/, { timeout: 20000 });
    
    // Selecionar primeira trip
    await page.waitForTimeout(2000);
    const firstTrip = page.locator('.option-item').first();
    await firstTrip.waitFor({ state: 'visible', timeout: 10000 });
    await firstTrip.click();
    await page.waitForTimeout(1000);
    
    // Confirmar check-in
    await page.getByRole('button', { name: 'Check In' }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: 'Confirm Check-in' }).click();
    await page.waitForTimeout(5000);
    
    // Verificar sucesso
    await expect(page.getByText('You are now on a trip!')).toBeVisible({ timeout: 10000 });
    
    // ==================== CHECK-OUT ====================
    await page.getByRole('button', { name: 'Check Out' }).click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: 'Confirm Check-out' }).click();
    await page.waitForTimeout(5000);
    
    console.log('Check-in and check-out completed successfully!');
  });
});