import { test, expect } from '@playwright/test';

const LOGIN_RESPONSE = {
  token: 'fake-e2e-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  username: 'admin',
  role: 'ADMIN',
};

test.describe('Login', () => {
  test('redirects to /login when there is no session', async ({ page }) => {
    await page.goto('/products');

    await expect(page).toHaveURL(/\/login/);
  });

  test('logs in successfully and loads the product list', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({ json: LOGIN_RESPONSE });
    });
    await page.route('**/api/v1/products/active**', async route => {
      await route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: true } });
    });

    await page.goto('/login');
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('admin123');
    await page.getByRole('button', { name: 'Ingresar' }).click();

    await expect(page).toHaveURL(/\/products$/);
    await expect(page.getByText('admin')).toBeVisible();
  });

  test('shows an error message on invalid credentials', async ({ page }) => {
    await page.route('**/api/v1/auth/login', async route => {
      await route.fulfill({ status: 401, json: { status: 401, error: 'Unauthorized', mensaje: 'Usuario o contraseña incorrectos' } });
    });

    await page.goto('/login');
    await page.getByLabel('Usuario').fill('admin');
    await page.getByLabel('Contraseña').fill('wrong');
    await page.getByRole('button', { name: 'Ingresar' }).click();

    await expect(page.getByText('Usuario o contraseña incorrectos')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('logs out and redirects to /login', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-e2e-token');
      localStorage.setItem('auth_username', 'admin');
      localStorage.setItem('auth_role', 'ADMIN');
    });
    await page.route('**/api/v1/products/active**', async route => {
      await route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: true } });
    });

    await page.goto('/products');
    await page.getByLabel('Cerrar sesión').click();

    await expect(page).toHaveURL(/\/login/);
  });
});
