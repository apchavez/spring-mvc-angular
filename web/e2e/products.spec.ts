import { test, expect, Route } from '@playwright/test';

interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  category: string;
  price: number;
  stock: number;
  active: boolean;
}

const PRODUCT: Product = {
  id: 1,
  sku: 'SKU-001',
  name: 'Wireless Mouse',
  description: 'Mouse inalámbrico ergonómico',
  category: 'Electronics',
  price: 29.99,
  stock: 150,
  active: true,
};

function mockProducts(products: Product[]) {
  return async (route: Route) => {
    const method = route.request().method();
    const url = route.request().url();

    if (method === 'GET' && (url.includes('/active') || url.includes('/inactive'))) {
      const filtered = url.includes('/inactive')
        ? products.filter(p => !p.active)
        : products.filter(p => p.active);
      await route.fulfill({
        json: {
          content: filtered,
          page: 0,
          size: 20,
          totalElements: filtered.length,
          totalPages: 1,
          last: true,
        },
      });
    } else if (method === 'GET') {
      const id = Number(url.split('/').pop());
      const found = products.find(p => p.id === id);
      await route.fulfill({ json: found ?? PRODUCT });
    } else if (method === 'POST') {
      const body = await route.request().postDataJSON();
      const created = { id: products.length + 1, ...body };
      products.push(created);
      await route.fulfill({ status: 201, json: created });
    } else if (method === 'PUT') {
      const id = Number(url.split('/').pop());
      const body = await route.request().postDataJSON();
      const idx = products.findIndex(p => p.id === id);
      if (idx >= 0) products[idx] = { ...products[idx], ...body };
      await route.fulfill({ json: products[idx >= 0 ? idx : 0] });
    } else if (method === 'DELETE') {
      const id = Number(url.split('/').pop());
      const idx = products.findIndex(p => p.id === id);
      if (idx >= 0) products.splice(idx, 1);
      await route.fulfill({ status: 204, body: '' });
    } else {
      await route.continue();
    }
  };
}

test.describe('Products', () => {
  test.beforeEach(async ({ page }) => {
    // The product routes are guarded (authGuard) since the login flow was added — seed a fake
    // session before navigation so these tests keep exercising product CRUD, not auth. The API
    // calls are already fully mocked per-test below; a real login round trip isn't needed here.
    await page.addInitScript(() => {
      localStorage.setItem('auth_token', 'fake-e2e-token');
      localStorage.setItem('auth_username', 'admin');
      localStorage.setItem('auth_role', 'ADMIN');
    });
  });

  test('shows empty state when no products exist', async ({ page }) => {
    await page.route('**/api/v1/products**', mockProducts([]));

    await page.goto('/products');

    await expect(page.getByText('No active products found.')).toBeVisible();
  });

  test('shows products in the table', async ({ page }) => {
    await page.route('**/api/v1/products**', mockProducts([PRODUCT]));

    await page.goto('/products');

    await expect(page.getByText('SKU-001')).toBeVisible();
    await expect(page.getByText('Wireless Mouse')).toBeVisible();
    await expect(page.getByText('Electronics')).toBeVisible();
  });

  test('creates a product and shows it in the table', async ({ page }) => {
    const products: Product[] = [];
    await page.route('**/api/v1/products**', mockProducts(products));

    await page.goto('/products');
    await expect(page.getByText('No active products found.')).toBeVisible();

    await page.getByRole('link', { name: /New Product/i }).click();
    await expect(page).toHaveURL(/\/products\/new/);

    await page.getByLabel('SKU').fill('SKU-001');
    await page.getByLabel('Name').fill('Wireless Mouse');
    await page.getByLabel('Price').fill('29.99');
    await page.getByLabel('Stock').fill('150');

    await page.getByRole('button', { name: 'Create' }).click();

    await expect(page).toHaveURL(/\/products$/);
    await expect(page.getByText('SKU-001')).toBeVisible();
    await expect(page.getByText('Wireless Mouse')).toBeVisible();
  });

  test('edits a product and shows updated data', async ({ page }) => {
    const products: Product[] = [{ ...PRODUCT }];
    await page.route('**/api/v1/products**', mockProducts(products));

    await page.goto('/products');
    await expect(page.getByText('Wireless Mouse')).toBeVisible();

    await page.getByLabel('Edit product').first().click();
    await expect(page).toHaveURL(/\/products\/1\/edit/);

    await page.getByLabel('Name').clear();
    await page.getByLabel('Name').fill('Wireless Mouse Pro');
    await page.getByRole('button', { name: 'Update' }).click();

    await expect(page).toHaveURL(/\/products$/);
    await expect(page.getByText('Wireless Mouse Pro')).toBeVisible();
  });

  test('deletes a product and shows empty state', async ({ page }) => {
    const products: Product[] = [{ ...PRODUCT }];
    await page.route('**/api/v1/products**', mockProducts(products));

    await page.goto('/products');
    await expect(page.getByText('Wireless Mouse')).toBeVisible();

    await page.getByLabel('Delete product').first().click();

    await expect(page.getByText('No active products found.')).toBeVisible();
  });

  test('shows validation error for empty required SKU field', async ({ page }) => {
    await page.route('**/api/v1/products**', mockProducts([]));

    await page.goto('/products/new');

    await page.getByLabel('SKU').focus();
    await page.getByLabel('SKU').blur();

    await expect(page.getByText('SKU is required')).toBeVisible();
  });

  test('shows validation error for empty required Name field', async ({ page }) => {
    await page.route('**/api/v1/products**', mockProducts([]));

    await page.goto('/products/new');

    await page.getByLabel('Name').focus();
    await page.getByLabel('Name').blur();

    await expect(page.getByText('Name is required')).toBeVisible();
  });
});
