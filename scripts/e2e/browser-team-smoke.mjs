/**
 * 团队模块浏览器冒烟 E2E(Playwright + 真实前端/后端)
 * 覆盖:无团队默认发现、有团队默认我的团队、接受邀请即时出现、多团队切换、
 *       工作台全导航、头像上传保存、OWNER/EDITOR/AUTHOR 权限差异。
 * 前置:后端 http://127.0.0.1:8849(admin 验证码已临时关闭)、前端 http://localhost:8847
 * 运行:node scripts/e2e/browser-team-smoke.mjs
 */
import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'
import { mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'

const requireWeb = createRequire(new URL('../../pxczxn-web/package.json', import.meta.url))
const { chromium } = requireWeb('playwright')
const requireAdmin = createRequire(new URL('../../pxczxn-admin/package.json', import.meta.url))
const JSEncrypt = requireAdmin('jsencrypt')

const API = 'http://127.0.0.1:8849'
const WEB = 'http://localhost:8847'
const SHOTS = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'outputs', 'e2e-screenshots')
mkdirSync(SHOTS, { recursive: true })

// 本地测试环境:清除注册/登录限流窗口,保证可重复运行(无 mysql CLI 时静默跳过)
try {
  execFileSync('C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe',
    ['--user=root', '--database=pxczxn_community', '--execute=DELETE FROM community_abuse_window WHERE action_type IN ("REGISTER","LOGIN");'],
    { env: { ...process.env, MYSQL_PWD: 'root' }, stdio: 'ignore' })
  console.log('已清除注册/登录限流窗口')
} catch {}

const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 15)
const users = {}
const FAILURES = []

async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body ? { 'Content-Type': 'application/json' } : {}
  if (token) headers[token.name] = token.value
  const r = await fetch(`${API}${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined })
  const p = await r.json()
  if (!codes.includes(Number(p.code))) throw new Error(`${method} ${path}: ${JSON.stringify(p)}`)
  return p
}
function assert(c, m) { if (!c) throw new Error(m) }
async function check(page, label, fn) {
  try { await fn(); console.log(`  ✓ ${label}`) }
  catch (e) {
    FAILURES.push(`${label}: ${e.message}`)
    console.log(`  ✗ ${label}: ${e.message}`)
    try {
      const diag = {
        url: page.url(),
        activeTab: await page.locator('.teams-hub__tabs button.active').textContent().catch(() => null),
        rows: await page.locator('.request-row').count(),
        body: (await page.locator('body').textContent()).slice(0, 260),
      }
      console.log('  [页面诊断]', JSON.stringify(diag))
      await page.screenshot({ path: join(SHOTS, `debug-${label.replace(/[^\w\u4e00-\u9fa5]/g, '_')}.png`) })
    } catch {}
  }
}
async function newUser(role) {
  const name = `bw_${role}_${stamp}`, email = `${name}@example.test`, password = 'Browser-pass-1'
  const created = await api('/api/v1/auth/register', { method: 'POST', body: { username: name, email, password, displayName: `${role}-${stamp}` } })
  const s = await api('/api/v1/auth/login', { method: 'POST', body: { email, password } })
  return { id: created.data.userId, name, email, password, token: { name: s.data.tokenName, value: s.data.tokenValue } }
}
async function adminLogin() {
  const enc = (await api('/api/crypto/config')).data
  const e = new JSEncrypt(); e.setPublicKey(enc.publicKey)
  const login = await api('/api/auth/login', { method: 'POST', body: { username: 'admin', password: e.encrypt('admin123'), rememberMe: false } })
  const decrypt = async (v, k) => {
    if (typeof v !== 'string' || !v.includes('.')) return v
    const [iv, pl] = v.split('.')
    const key = await webcrypto.subtle.importKey('raw', Buffer.from(k, 'base64'), { name: 'AES-GCM' }, false, ['decrypt'])
    const plain = await webcrypto.subtle.decrypt({ name: 'AES-GCM', iv: Buffer.from(iv, 'base64') }, key, Buffer.from(pl, 'base64'))
    return JSON.parse(Buffer.from(plain).toString('utf8'))
  }
  const data = await decrypt(login.data, enc.aesKey)
  return { token: { name: 'Authorization', value: data.token }, decrypt, enc }
}
async function createTeam(owner, slug) {
  const app = await api('/api/v1/team-applications', { method: 'POST', token: owner.token, body: {
    teamName: `Browser Team ${slug}`, teamSlug: slug, description: 'browser smoke', idempotencyKey: slug,
  } })
  const admin = await adminLogin()
  const approved = await api(`/admin-api/community/team-applications/${app.data.id}/approve`, { method: 'POST', token: admin.token, body: { reviewComment: 'ok' } })
  const data = await admin.decrypt(approved.data, admin.enc.aesKey)
  return String(data.teamId)
}
async function invite(owner, target, teamId, roleCode) {
  return api(`/api/v1/teams/${teamId}/invitations`, { method: 'POST', token: owner.token, body: {
    userId: target.id, roleCode, idempotencyKey: `bw-invite-${teamId}-${target.id}`,
  } })
}
async function acceptInvite(target, inviteId) {
  await api(`/api/v1/teams/invitations/${inviteId}/accept`, { method: 'POST', token: target.token })
}

async function loginAndGo(page, user, path, { uiLogin = false } = {}) {
  if (uiLogin) {
    // 浏览器表单登录冒烟(仅在首个用例执行,验证登录 UI 链路)
    await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
    // 等待客户端 React 接管页面(SSR 引导脚本被移除),避免在水合完成前提交表单导致 onSubmit 不触发
    await page.waitForFunction(() => !document.body.textContent.includes('vite-rsc/entry-browser'), null, { timeout: 120000 }).catch(() => {})
    await page.waitForTimeout(800)
    await page.locator('.auth-form button[type="submit"]').filter({ hasText: '登录' }).waitFor()
    await page.getByPlaceholder('请输入邮箱地址').fill(user.email)
    await page.getByPlaceholder('请输入密码').fill(user.password)
    await page.locator('.auth-form button[type="submit"]').filter({ hasText: '登录' }).click()
    await page.waitForFunction(() => {
      const raw = localStorage.getItem('pxczxn-community-session')
      return raw && raw.length > 40
    }, null, { timeout: 45000 })
  } else {
    // API 登录注入 session:addInitScript 在任何页面加载前写入,避免与登录页跳转竞争
    const s = await api('/api/v1/auth/login', { method: 'POST', body: { email: user.email, password: user.password } })
    await page.addInitScript(({ name, value, username }) => {
      localStorage.setItem('pxczxn-community-session', JSON.stringify({
        tokenName: name, tokenValue: value, expiresIn: 7200, username,
      }))
    }, { name: s.data.tokenName, value: s.data.tokenValue, username: user.name })
  }
  await page.goto(`${WEB}${path}`, { waitUntil: 'domcontentloaded' })
  // 等客户端 JS 接管页面(SSR 引导代码被替换),避免 dev 慢编译下的误判
  await page.waitForFunction(() => !document.body.textContent.includes('vite-rsc/entry-browser'), null, { timeout: 120000 }).catch(() => {})
  await page.waitForTimeout(1200)
}
function waitText(page, selector, text, timeout = 45000) {
  return page.waitForFunction(([sel, t]) => {
    const el = document.querySelector(sel)
    return el && el.textContent.includes(t)
  }, [selector, text], { timeout })
}
function waitActiveTab(page, text, timeout = 45000) {
  return page.waitForFunction((t) => {
    const el = document.querySelector('.teams-hub__tabs button.active')
    return el && el.textContent.includes(t)
  }, text, { timeout })
}

;(async () => {
  console.log('准备数据(API)…')
  users.visitor = await newUser('visitor')
  users.owner = await newUser('owner')
  users.editor = await newUser('editor')
  users.author = await newUser('author')
  const team1Id = await createTeam(users.owner, `bw-team1-${stamp}`)
  const team2Id = await createTeam(users.owner, `bw-team2-${stamp}`)
  await invite(users.owner, users.editor, team1Id, 'EDITOR')   // 留给浏览器流程接受
  await invite(users.owner, users.author, team1Id, 'AUTHOR')
  const authorPending = (await api('/api/v1/teams/invitations/me', { token: users.author.token })).data
  await acceptInvite(users.author, authorPending.find(i => String(i.teamId) === String(team1Id)).id)
  const t1 = `bw-team1-${stamp}`, t2 = `bw-team2-${stamp}`
  console.log(`  team1=${t1}(${team1Id}) team2=${t2}(${team2Id})`)

  const browser = await chromium.launch()
  let page, ctx
  async function newPage() {
    const c = await browser.newContext({ viewport: { width: 1440, height: 900 } })
    const p = await c.newPage()
    p.setDefaultTimeout(45000)
    return { ctx: c, page: p }
  }

  // 预热:让 dev server 完成各页面首编译(只保留 3 个关键页面,避免未登录错误页干扰)
  console.log('预热页面…')
  {
    const warm = await newPage()
    for (const p of ['/teams', `/teams/${t1}/workspace`, `/teams/${t1}/workspace/settings`]) {
      await warm.page.goto(`${WEB}${p}`, { waitUntil: 'domcontentloaded' }).catch(() => {})
      await warm.page.waitForTimeout(600)
    }
    await warm.ctx.close().catch(() => {})
  }

  // 1) 无团队 → 默认发现团队(浏览器表单登录冒烟)
  ;({ ctx, page } = await newPage())
  await check(page, '无团队默认展示发现团队', async () => {
    await loginAndGo(page, users.visitor, '/teams', { uiLogin: true })
    await waitActiveTab(page, '发现团队')
    await page.screenshot({ path: join(SHOTS, '01-visitor-discover.png') })
  })
  await ctx.close().catch(() => {})

  // 2) 有团队 → 默认我的团队 + 全部卡片
  ;({ ctx, page } = await newPage())
  await check(page, '有团队默认展示我的团队且卡片齐全', async () => {
    await loginAndGo(page, users.owner, '/teams')
    await waitActiveTab(page, '我的团队')
    await page.getByText('Browser Team bw-team1').first().waitFor()
    await page.getByText('Browser Team bw-team2').first().waitFor()
    await page.screenshot({ path: join(SHOTS, '02-owner-mine.png') })
  })
  await ctx.close().catch(() => {})

  // 3) 接受邀请后:留在邀请页显示“已加入”反馈,提供进入工作台/查看我的团队操作(不自动跳转)
  ;({ ctx, page } = await newPage())
  await check(page, '接受邀请后新团队立即出现', async () => {
    await loginAndGo(page, users.editor, '/teams')
    await page.locator('.teams-hub__tabs button').filter({ hasText: '邀请与申请' }).waitFor({ timeout: 90000 })
    await page.locator('.teams-hub__tabs button').filter({ hasText: '邀请与申请' }).click()
    const acceptBtn = page.locator('.request-row .primary-button').filter({ hasText: '接受' }).first()
    await acceptBtn.waitFor({ timeout: 90000 })
    await acceptBtn.click()
    // 产品行为:接受后不自动跳转,留在邀请页显示“已加入团队”反馈,并提供“进入团队工作台”/“查看我的团队”操作(无剩余邀请时)
    const feedback = page.locator('.requests-panel__accepted')
    await feedback.waitFor({ timeout: 90000 })
    await feedback.getByText('已加入团队').waitFor()
    await feedback.getByText('进入团队工作台').waitFor()
    await feedback.getByText('查看我的团队').waitFor()
    // 主动点“查看我的团队”切 tab,验证新团队已在“我的团队”列表(数据已刷新),非自动跳转
    await feedback.getByText('查看我的团队').click()
    await page.getByText('Browser Team bw-team1').first().waitFor({ timeout: 90000 })
    await page.screenshot({ path: join(SHOTS, '03-invite-accepted.png') })
  })
  await ctx.close().catch(() => {})

  // 4) 工作台多团队切换
  ;({ ctx, page } = await newPage())
  await check(page, '工作台切换器导航到目标团队', async () => {
    await loginAndGo(page, users.owner, `/teams/${t1}/workspace`)
    await waitText(page, '.workspace-header h1', 'Browser Team bw-team1')
    await page.locator('.team-switcher__current').click()
    await page.locator('.team-switcher__option', { hasText: 'Browser Team bw-team2' }).click()
    await page.waitForURL(`**/teams/${t2}/workspace`, { timeout: 30000 })
    await waitText(page, '.workspace-header h1', 'Browser Team bw-team2')
    await page.screenshot({ path: join(SHOTS, '04-switched-team2.png') })
  })
  await ctx.close().catch(() => {})

  // 5) 工作台全导航(OWNER)
  ;({ ctx, page } = await newPage())
  await check(page, '工作台六页导航(OWNER)', async () => {
    await loginAndGo(page, users.owner, `/teams/${t2}/workspace`)
    const nav = [
      ['概览', '', '.workspace-stats'],
      ['内容', '/content', '.workspace-content-page__header'],
      ['系列', '/series', '.workspace-series-create'],
      ['投稿', '/submissions', '.workspace-submit-form'],
      ['成员', '/members', '.workspace-invite-form'],
      ['设置', '/settings', '.workspace-settings-form'],
    ]
    for (const [label, path, sel] of nav) {
      await page.goto(`${WEB}/teams/${t2}/workspace${path}`, { waitUntil: 'domcontentloaded' })
      await page.locator(sel).first().waitFor()
    }
    await page.screenshot({ path: join(SHOTS, '05-owner-nav-settings.png') })
  })
  await ctx.close().catch(() => {})

  // 6) 头像上传保存(设置页)
  ;({ ctx, page } = await newPage())
  await check(page, '头像上传/预览/保存', async () => {
    await loginAndGo(page, users.owner, `/teams/${t1}/workspace/settings`)
    await page.locator('.workspace-settings-form__media-actions .secondary-button').filter({ hasText: '上传头像' }).waitFor()
    const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64')
    await page.locator('.workspace-settings-form__avatar input[type="file"]').setInputFiles({ name: 'avatar.png', mimeType: 'image/png', buffer: png })
    await page.getByText('头像已上传').waitFor()
    await page.locator('.workspace-settings-form .primary-button', { hasText: '保存资料' }).click()
    await page.getByText('团队资料已保存').waitFor()
    await page.screenshot({ path: join(SHOTS, '06-avatar-saved.png') })
  })
  await ctx.close().catch(() => {})

  // 7) 权限差异
  ;({ ctx, page } = await newPage())
  await check(page, 'EDITOR 权限裁剪', async () => {
    await loginAndGo(page, users.editor, `/teams/${t1}/workspace`)
    await page.locator('.workspace-nav').waitFor()
    const navText = await page.locator('.workspace-nav').textContent()
    assert(navText?.includes('系列') && navText?.includes('投稿'), 'EDITOR 应有系列/投稿')
    assert(!navText?.includes('成员') && !navText?.includes('设置'), `EDITOR 不应有成员/设置:${navText}`)
    await page.screenshot({ path: join(SHOTS, '07-editor-nav.png') })
  })
  await ctx.close().catch(() => {})
  ;({ ctx, page } = await newPage())
  await check(page, 'AUTHOR 权限裁剪', async () => {
    await loginAndGo(page, users.author, `/teams/${t1}/workspace`)
    await page.locator('.workspace-nav').waitFor()
    const navText = await page.locator('.workspace-nav').textContent()
    assert(navText?.includes('概览') && navText?.includes('投稿'), 'AUTHOR 应有概览/投稿')
    assert(!navText?.includes('系列') && !navText?.includes('成员') && !navText?.includes('设置'), `AUTHOR 权限过宽:${navText}`)
    await page.screenshot({ path: join(SHOTS, '08-author-nav.png') })
  })
  await ctx.close().catch(() => {})

  await browser.close()
  if (FAILURES.length) {
    console.error(`\n浏览器 E2E 完成,${FAILURES.length} 项失败:`)
    for (const f of FAILURES) console.error(`  ✗ ${f}`)
    process.exit(1)
  }
  console.log('\n浏览器 E2E 全部通过,截图见 outputs/e2e-screenshots/')
})().catch((err) => { console.error('浏览器 E2E 异常:', err.message); process.exit(1) })
