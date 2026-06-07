import { test, expect } from '@playwright/test';

const TEST_USER = {
  displayName: 'E2E测试用户',
  email: `e2e-${Date.now()}@test.wallpaper.local`,
  password: 'e2ePass123!',
};

test.describe('用户注册/登录/收藏/下载/登出 完整链路', () => {
  test('完整链路：注册 → 登录态呈现 → 收藏 → 下载 → 登出 → 游客态恢复', async ({ page }) => {
    // ========== 1. 打开首页，确认游客态 ==========
    await page.goto('/main.html');
    await page.waitForSelector('#account');

    // 游客状态：注册/登录表单可见
    const loginForm = page.locator('#loginForm');
    await expect(loginForm).toBeVisible();
    const registerForm = page.locator('#registerForm');
    await expect(registerForm).toBeVisible();

    // 登录身份区域隐藏
    await expect(page.locator('#accountIdentity')).toBeHidden();

    // ========== 2. 注册新用户 ==========
    await page.fill('#registerDisplayName', TEST_USER.displayName);
    await page.fill('#registerEmail', TEST_USER.email);
    await page.fill('#registerPassword', TEST_USER.password);
    await page.locator('#registerForm button[type="submit"]').click();

    // 等待注册完成，表单隐藏
    await expect(registerForm).toBeHidden({ timeout: 10_000 });

    // ========== 3. 验证登录态 ==========
    // 身份区域可见，显示昵称
    const accountIdentity = page.locator('#accountIdentity');
    await expect(accountIdentity).toBeVisible();
    await expect(page.locator('#accountDisplayName')).toHaveText(TEST_USER.displayName);

    // 表单不可见，退出按钮可见
    await expect(page.locator('#accountForms')).toBeHidden();
    await expect(page.locator('#logoutButton')).toBeVisible();

    // 状态文案更新为"已登录"
    await expect(page.locator('#accountStatusCopy')).toContainText('已登录');

    // ========== 4. 收藏一张壁纸 ==========
    // 滚动到画廊区域
    await page.locator('#gallery').scrollIntoViewIfNeeded();
    await page.waitForSelector('.image-card');
    const firstCard = page.locator('.image-card').first();

    // 点击图片打开模态框
    await firstCard.click();
    await page.waitForSelector('#myModal.show');

    // 检查是否有收藏按钮（如果有）
    const favButton = page.locator('[data-action="favorite"]').first();
    if (await favButton.isVisible()) {
      await favButton.click();
    }

    // 关闭模态框
    await page.locator('.close').click();
    await expect(page.locator('#myModal.show')).toBeHidden({ timeout: 3000 });

    // ========== 5. 下载计数 ==========
    // 再次打开同一张图片触发下载记录
    await firstCard.click();
    await page.waitForSelector('#myModal.show');

    const downloadBtn = page.locator('#downloadBtn');
    await expect(downloadBtn).toBeVisible();
    await expect(downloadBtn).toHaveAttribute('href', /\/download\//);

    // 关闭模态框
    await page.locator('.close').click();

    // ========== 6. 登出 ==========
    await page.locator('#account').scrollIntoViewIfNeeded();
    await page.locator('#logoutButton').click();

    // 等待登出完成
    await expect(page.locator('#loginForm')).toBeVisible({ timeout: 10_000 });

    // ========== 7. 验证回到游客态 ==========
    await expect(page.locator('#accountForms')).toBeVisible();
    await expect(page.locator('#accountIdentity')).toBeHidden();
    await expect(page.locator('#logoutButton')).toBeHidden();
    await expect(page.locator('#accountStatusCopy')).toContainText('游客');
  });

  test('登录：输入错误密码应显示错误提示', async ({ page }) => {
    await page.goto('/main.html');
    await page.waitForSelector('#loginForm');

    await page.fill('#loginEmail', TEST_USER.email);
    await page.fill('#loginPassword', 'wrong-password-999');
    await page.locator('#loginForm button[type="submit"]').click();

    // 应显示错误信息
    await expect(page.locator('#loginStatus')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('#loginStatus')).toContainText(/错误|失败|凭据/i);
  });

  test('注册：昵称为空应阻止提交并提示', async ({ page }) => {
    await page.goto('/main.html');
    await page.waitForSelector('#registerForm');

    await page.fill('#registerEmail', TEST_USER.email);
    await page.fill('#registerPassword', TEST_USER.password);
    // 昵称留空
    await page.locator('#registerForm button[type="submit"]').click();

    // 应显示前端验证错误
    await expect(page.locator('#registerStatus')).toBeVisible({ timeout: 3000 });
  });
});
