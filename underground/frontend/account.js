/* Account data stays in memory. Authentication belongs to the HttpOnly session cookie. */
(() => {
  const br = {
    account: 'Minha conta', login: 'Entrar', register: 'Criar conta', profile: 'Meu perfil', logout: 'Sair',
    appLabel: 'CONTA E PERFIL', guest: 'Visitante', loading: 'Carregando…',
    trustTitle: 'Sua conta é real', trustBody: 'Descoberta, convites e pagamentos ainda são demonstrações.',
    demo: 'Demonstração com dados fictícios. Nenhum convite ou pagamento será realizado.',
    email: 'E-mail', password: 'Senha', passwordHelp: 'Use de 12 a 64 caracteres: letras sem acentos, números, espaços ou símbolos.',
    role: 'Tipo de perfil', CLIENT: 'Cliente', HOST: 'Anfitrião', ADMIN: 'Administrador',
    adult: 'Confirmo que tenho 18 anos ou mais.',
    registerLead: 'Crie sua conta para preparar seu perfil. A verificação de identidade ainda não está disponível.',
    loginLead: 'Acesse sua conta e continue preenchendo seu perfil.',
    registered: 'Conta criada. Entre para completar seu perfil.',
    noAccount: 'Ainda não tem conta?', hasAccount: 'Já tem conta?',
    displayName: 'Nome de exibição', bio: 'Sobre você', save: 'Salvar perfil',
    publicHelp: 'Nome e apresentação serão visíveis a outras contas verificadas. Não inclua endereço, telefone ou documentos.',
    PENDING: 'Identidade pendente', VERIFIED: 'Identidade verificada', REJECTED: 'Identidade não aprovada',
    pendingHelp: 'Você pode editar seu perfil. A descoberta fica indisponível enquanto sua identidade estiver pendente. Ainda não recebemos documentos nem aprovamos identidades.',
    rejectedHelp: 'Sua identidade não foi aprovada. A descoberta permanece indisponível.',
    verifiedHelp: 'Sua identidade está verificada. As telas de descoberta ainda exibem somente demonstrações.',
    saved: 'Perfil salvo.', signedOut: 'Você saiu da conta.', sessionExpired: 'Sua sessão terminou. Entre novamente.',
    network: 'Não foi possível conectar. Verifique sua conexão e tente novamente.',
    invalid: 'Confira os dados informados.', conflict: 'Não foi possível cadastrar este e-mail. Use outro ou entre na sua conta.',
    credentials: 'E-mail ou senha inválidos.', forbidden: 'A solicitação expirou ou não foi autorizada. Tente novamente.',
    unexpected: 'Não foi possível concluir. Tente novamente.', profileError: 'Não foi possível carregar seu perfil.',
    retry: 'Tentar novamente', loginRequired: 'Entre para acessar seu perfil.', page: 'Página'
  };
  const pt = {...br, account: 'A minha conta', profile: 'O meu perfil', password: 'Palavra-passe',
    loading: 'A carregar…', save: 'Guardar perfil', saved: 'Perfil guardado.', signedOut: 'Saiu da conta.',
    displayName: 'Nome de apresentação', bio: 'Sobre si', email: 'Email',
    passwordHelp: 'Use entre 12 e 64 caracteres: letras sem acentos, números, espaços ou símbolos.',
    registerLead: 'Crie a sua conta para preparar o seu perfil. A verificação de identidade ainda não está disponível.',
    loginLead: 'Aceda à sua conta e continue a preencher o seu perfil.',
    pendingHelp: 'Pode editar o seu perfil. A descoberta fica indisponível enquanto a sua identidade estiver pendente. Ainda não recebemos documentos nem aprovamos identidades.',
    credentials: 'Email ou palavra-passe inválidos.', sessionExpired: 'A sua sessão terminou. Entre novamente.',
    network: 'Não foi possível estabelecer ligação. Verifique a ligação e tente novamente.',
    adult: 'Confirmo que tenho 18 anos ou mais.'};
  const catalogs = {'pt-BR': br, 'pt-PT': pt};
  const state = {account: null, profile: null, initializing: true, profileError: false};
  let redraw = () => {};
  let notice = '';
  const t = key => (catalogs[document.documentElement.lang] || br)[key] || key;
  const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  class ApiError extends Error { constructor(status) { super(String(status)); this.status = status; } }
  async function request(path, options = {}) {
    const response = await fetch('/api' + path, {credentials: 'same-origin', cache: 'no-store',
      signal: AbortSignal.timeout(12000), ...options});
    if (!response.ok) throw new ApiError(response.status);
    return response.status === 204 ? null : response.json();
  }
  async function mutate(path, method, data, form = false) {
    const csrf = await request('/auth/csrf');
    return request(path, {method, headers: {
      [csrf.headerName]: csrf.token,
      'Content-Type': form ? 'application/x-www-form-urlencoded' : 'application/json'
    }, body: form ? new URLSearchParams(data) : JSON.stringify(data)});
  }
  function errorText(error, login = false) {
    if (!(error instanceof ApiError)) return t('network');
    return t(({400: 'invalid', 401: login ? 'credentials' : 'sessionExpired',
      403: 'forbidden', 409: 'conflict'})[error.status] || 'unexpected');
  }
  function announce(message) { document.querySelector('#announcer').textContent = message; }
  function feedback(message, isError = false) {
    const element = document.querySelector('#account-feedback');
    if (element) { element.textContent = message; element.classList.toggle('error', isError); }
    announce(message);
  }
  function shell(title, lead, content) {
    return `<section class="page account-page"><span class="eyebrow">${t('account')}</span>
      <h1 class="page-title">${t(title)}</h1><p class="lead">${lead}</p>
      <p id="account-feedback" class="form-feedback" role="status">${escape(notice)}</p>${content}</section>`;
  }
  function passwordField(create) {
    return `<label for="password">${t('password')}</label>
      <input id="password" name="password" type="password" required ${create ? 'minlength="12" maxlength="64" pattern="[&#x20;-&#x7E;]+" aria-describedby="password-help"' : ''}
        autocomplete="${create ? 'new-password' : 'current-password'}">
      ${create ? `<p id="password-help" class="field-help">${t('passwordHelp')}</p>` : ''}`;
  }
  function loginPage() {
    return shell('login', t('loginLead'), `<form id="login-form" class="card account-form"><fieldset>
      <label for="email">${t('email')}</label><input id="email" name="email" type="email" maxlength="254" autocomplete="username" required>
      ${passwordField(false)}<button class="btn" type="submit">${t('login')}</button>
      </fieldset></form><p>${t('noAccount')} <a href="#register">${t('register')}</a></p>`);
  }
  function registerPage() {
    return shell('register', t('registerLead'), `<form id="register-form" class="card account-form"><fieldset>
      <label for="email">${t('email')}</label><input id="email" name="email" type="email" maxlength="254" autocomplete="username" required>
      ${passwordField(true)}<label for="role">${t('role')}</label>
      <select id="role" name="role"><option value="CLIENT">${t('CLIENT')}</option><option value="HOST">${t('HOST')}</option></select>
      <label class="checkbox-field"><input name="adultConfirmed" type="checkbox" required> <span>${t('adult')}</span></label>
      <button class="btn" type="submit">${t('register')}</button></fieldset></form>
      <p>${t('hasAccount')} <a href="#login">${t('login')}</a></p>`);
  }
  function profilePage() {
    if (state.initializing) return shell('profile', t('loading'), '');
    if (!state.account) return shell('profile', t('loginRequired'), `<a class="btn" href="#login">${t('login')}</a>`);
    if (state.profileError) return shell('profile', t('profileError'), `<button class="btn" id="retry-profile">${t('retry')}</button>`);
    const a = state.account, p = state.profile || {};
    const status = ['PENDING', 'VERIFIED', 'REJECTED'].includes(a.verificationStatus) ? a.verificationStatus : 'PENDING';
    return shell('profile', '', `<div class="card account-summary"><p>${escape(a.email)} · ${t(a.role)}</p>
      <span class="status ${status === 'VERIFIED' ? '' : 'pending'}">${t(status)}</span>
      <p>${t({PENDING:'pendingHelp', VERIFIED:'verifiedHelp', REJECTED:'rejectedHelp'}[status])}</p></div>
      <form id="profile-form" class="card account-form"><fieldset>
      <label for="displayName">${t('displayName')}</label><input id="displayName" name="displayName" maxlength="80" required value="${escape(p.displayName)}" autocomplete="nickname">
      <label for="bio">${t('bio')}</label><textarea id="bio" name="bio" maxlength="1000" rows="5" aria-describedby="public-help">${escape(p.bio)}</textarea>
      <p id="public-help" class="field-help">${t('publicHelp')}</p><button class="btn" type="submit">${t('save')}</button>
      </fieldset></form><button id="logout" class="btn secondary">${t('logout')}</button>`);
  }
  function header() {
    const chip = document.querySelector('#user-chip');
    chip.replaceChildren();
    const link = document.createElement('a');
    link.href = state.account ? '#profile' : '#login';
    link.textContent = state.account ? (state.profile?.displayName || t('profile')) : t('login');
    chip.append(link);
  }
  async function loadProfile() {
    state.profileError = false;
    try { state.profile = await request('/profiles/me'); }
    catch (error) {
      if (error.status === 404) state.profile = null;
      else if (error.status === 401) { state.account = null; state.profile = null; notice = t('sessionExpired'); }
      else state.profileError = true;
    }
  }
  async function submit(form, action) {
    const data = Object.fromEntries(new FormData(form));
    const route = location.hash;
    const fields = form.querySelector('fieldset');
    fields.disabled = true;
    form.setAttribute('aria-busy', 'true');
    feedback(t('loading'));
    try { await action(data); }
    catch (error) {
      if (error.status === 401 && form.id === 'profile-form') {
        state.account = null; state.profile = null; notice = t('sessionExpired'); location.hash = 'login'; redraw();
      } else if (location.hash === route) feedback(errorText(error, form.id === 'login-form'), true);
    } finally { fields.disabled = false; form.removeAttribute('aria-busy'); }
  }
  function mount() {
    header();
    document.querySelector('#login-form')?.addEventListener('submit', event => {
      event.preventDefault();
      submit(event.currentTarget, async data => {
        await mutate('/auth/login', 'POST', data, true);
        state.account = await request('/accounts/me'); notice = ''; await loadProfile();
        location.hash = 'profile'; redraw();
      });
    });
    document.querySelector('#register-form')?.addEventListener('submit', event => {
      event.preventDefault();
      submit(event.currentTarget, async data => {
        await mutate('/auth/register', 'POST', {...data, adultConfirmed: data.adultConfirmed === 'on'});
        notice = t('registered'); location.hash = 'login'; redraw();
      });
    });
    document.querySelector('#profile-form')?.addEventListener('submit', event => {
      event.preventDefault();
      submit(event.currentTarget, async data => {
        state.profile = await mutate('/profiles/me', 'PUT', data); header(); feedback(t('saved'));
      });
    });
    document.querySelector('#retry-profile')?.addEventListener('click', async event => {
      event.currentTarget.disabled = true; await loadProfile(); redraw();
    });
    document.querySelector('#logout')?.addEventListener('click', async event => {
      const button = event.currentTarget; button.disabled = true;
      try { await mutate('/auth/logout', 'POST', {}); state.account = null; state.profile = null;
        notice = t('signedOut'); location.hash = 'login'; redraw();
      } catch (error) { feedback(errorText(error), true); button.disabled = false; }
    });
  }
  async function init(render) {
    redraw = render;
    try { state.account = await request('/accounts/me'); await loadProfile(); }
    catch (error) { if (error.status !== 401) notice = t('network'); }
    finally { state.initializing = false; if (location.hash === '#profile') redraw(); else { header(); if (notice) feedback(notice); } }
  }
  window.accountUI = {catalogs, loginPage, registerPage, profilePage, mount, init, t,
    clearNotice: () => { notice = ''; }};
})();
