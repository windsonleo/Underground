const {test, expect} = require('@playwright/test');

test('real account lifecycle, persistence, pending state and logout', async ({page}) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/#register');
  await page.locator('#email').fill('browser-' + Date.now() + '@example.org');
  const email = await page.locator('#email').inputValue();
  await page.locator('#password').fill('test-password-123');
  await page.locator('[name=adultConfirmed]').check();
  await page.locator('#register-form button').click();
  await expect(page).toHaveURL(/#login$/);
  await expect(page.locator('#account-feedback')).toContainText('Conta criada');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('wrong-password');
  await page.locator('#login-form button').click();
  await expect(page.locator('#account-feedback')).toContainText('inválidos');
  await page.locator('#password').fill('test-password-123');
  await page.locator('#login-form button').click();
  await expect(page).toHaveURL(/#profile$/);
  await expect(page.locator('.account-summary')).toContainText('Identidade pendente');
  // Stored markup must be displayed as text, including after page reload.
  const name = '<img src=x onerror=alert(1)>';
  await page.locator('#displayName').fill(name);
  await page.locator('#bio').fill('Café, música 🎵 e cultura.');
  await page.locator('#profile-form button').click();
  await expect(page.locator('#account-feedback')).toContainText('Perfil salvo');
  await page.reload();
  await expect(page.locator('#displayName')).toHaveValue(name);
  await expect(page.locator('#bio')).toHaveValue('Café, música 🎵 e cultura.');
  await expect(page.locator('#user-chip img')).toHaveCount(0);
  await expect(page.locator('#user-chip')).toContainText(name);
  await page.locator('#locale').selectOption('pt-PT');
  await expect(page.locator('#profile-form button')).toHaveText('Guardar perfil');
  await page.locator('#logout').click();
  await expect(page).toHaveURL(/#login$/);
  await page.reload();
  await expect(page.locator('#user-chip')).toHaveText('Entrar');
  const privateResponse = await page.request.get('/api/accounts/me');
  expect(privateResponse.status()).toBe(401);
  await page.goto('/#profile');
  await expect(page.locator('#profile-form')).toHaveCount(0);
  expect(errors).toEqual([]);
});

test('registration validation, duplicate email and no implicit approval', async ({page}) => {
  await page.goto('/#register');
  await page.locator('#email').fill('duplicate-' + Date.now() + '@example.org');
  const email = await page.locator('#email').inputValue();
  await page.locator('#password').fill('test-password-123');
  await page.locator('#role').selectOption('HOST');
  await page.locator('#register-form button').click();
  await expect(page).toHaveURL(/#register$/); // mandatory adult confirmation
  await page.locator('[name=adultConfirmed]').check();
  await page.locator('#register-form button').click();
  await expect(page).toHaveURL(/#login$/);
  await page.goto('/#register');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('test-password-123');
  await page.locator('[name=adultConfirmed]').check();
  await page.locator('#register-form button').click();
  await expect(page.locator('#account-feedback')).toContainText('Não foi possível cadastrar');
});

test('network error is actionable and form can be retried', async ({page}) => {
  await page.goto('/#login');
  await page.route('**/api/auth/csrf', route => route.abort());
  await page.locator('#email').fill('network@example.org');
  await page.locator('#password').fill('test-password-123');
  await page.locator('#login-form button').click();
  await expect(page.locator('#account-feedback')).toContainText('Não foi possível conectar');
  await expect(page.locator('#login-form button')).toBeEnabled();
});

test('expired session clears private profile and asks for login', async ({page, context}) => {
  const email = 'expired-' + Date.now() + '@example.org';
  await page.goto('/#register');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('test-password-123');
  await page.locator('[name=adultConfirmed]').check();
  await page.locator('#register-form button').click();
  await expect(page).toHaveURL(/#login$/);
  await page.locator('#email').fill(email);
  await page.locator('#password').fill('test-password-123');
  await page.locator('#login-form button').click();
  await expect(page).toHaveURL(/#profile$/);
  await page.locator('#displayName').fill('Session');
  await context.clearCookies();
  await page.locator('#profile-form button').click();
  await expect(page).toHaveURL(/#login$/);
  await expect(page.locator('#account-feedback')).toContainText('sessão terminou');
  await expect(page.locator('#user-chip')).toHaveText('Entrar');
});

test('mobile forms and demo labeling remain usable', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/#register');
  await expect(page.locator('#register-form button')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.goto('/#discover');
  await expect(page.locator('.demo-banner')).toContainText('dados fictícios');
});
