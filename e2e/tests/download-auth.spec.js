import { test, expect } from '@playwright/test';

function mockLocalApiBase(page) {
  return page.route('**/api-config.js*', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/javascript',
      body: "window.VEHICLE_WALLPAPER_CONFIG = Object.assign({ apiBase: '' }, window.VEHICLE_WALLPAPER_CONFIG || {});",
    });
  });
}

async function mockNativeSavePicker(page) {
  await page.addInitScript(() => {
    window.__downloadedBytes = 0;
    window.showSaveFilePicker = async () => ({
      createWritable: async () => new WritableStream({
        write(chunk) {
          const byteLength = chunk && typeof chunk.byteLength === 'number' ? chunk.byteLength : 0;
          window.__downloadedBytes += byteLength;
        },
      }),
    });
  });
}

async function registerAndLogin(page) {
  const email = `download-${Date.now()}@test.wallpaper.local`;

  await page.goto('/main.html');
  await page.waitForSelector('#account');
  await page.locator('[data-auth-tab="register"]').click();
  await page.fill('#registerDisplayName', 'Download E2E');
  await page.fill('#registerEmail', email);
  await page.fill('#registerPassword', 'downloadE2E123');
  await page.locator('#registerForm button[type="submit"]').click();
  await expect(page.locator('#logoutButton')).toBeVisible({ timeout: 15_000 });
}

test.describe('download auth flow', () => {
  test('logged-in download uses authenticated fetch and does not navigate to JSON error page', async ({ page }) => {
    await mockNativeSavePicker(page);
    await mockLocalApiBase(page);
    await registerAndLogin(page);

    await page.locator('#gallery').scrollIntoViewIfNeeded();
    await page.waitForSelector('.image-card', { timeout: 15_000 });
    await page.locator('.image-card').first().click();
    await page.waitForSelector('#myModal.show', { timeout: 10_000 });

    const downloadRequestPromise = page.waitForRequest((request) => {
      return request.url().includes('/download/') && /^Bearer /i.test(request.headers().authorization || '');
    });
    const downloadResponsePromise = page.waitForResponse((response) => {
      return response.url().includes('/download/') && response.status() === 200;
    });

    await page.locator('#downloadBtn').click();

    const downloadRequest = await downloadRequestPromise;
    await downloadResponsePromise;

    expect(downloadRequest.headers().authorization).toMatch(/^Bearer /i);
    await expect.poll(() => page.evaluate(() => window.__downloadedBytes)).toBeGreaterThan(0);
    await expect(page).toHaveURL(/\/main\.html$/);
    await expect(page.locator('#myModal')).toHaveClass(/show/);
    await expect(page.locator('body')).not.toContainText('"status":401');
  });
});
