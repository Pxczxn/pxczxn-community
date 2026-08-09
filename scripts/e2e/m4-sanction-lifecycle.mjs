import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'

const requireFromAdmin = createRequire(new URL('../../pxczxn-admin/package.json', import.meta.url))
const JSEncrypt = requireFromAdmin('jsencrypt')
const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8861'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'
const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 17)
const password = 'M4-sanction-e2e-password-1'
let cryptoConfig

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function localDateTime(afterSeconds) {
  const date = new Date(Date.now() + afterSeconds * 1000)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}`
}

async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body === undefined ? {} : { 'Content-Type': 'application/json' }
  if (token) headers[token.name] = token.value
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const payload = await response.json()
  if (!codes.includes(Number(payload.code))) {
    throw new Error(`${method} ${path}: ${JSON.stringify(payload)}`)
  }
  return payload
}

async function decryptAdminData(value, aesKey) {
  if (typeof value !== 'string' || !value.includes('.')) return value
  const [iv, payload] = value.split('.')
  const key = await webcrypto.subtle.importKey(
    'raw', Buffer.from(aesKey, 'base64'), { name: 'AES-GCM' }, false, ['decrypt'],
  )
  const plain = await webcrypto.subtle.decrypt(
    { name: 'AES-GCM', iv: Buffer.from(iv, 'base64') }, key, Buffer.from(payload, 'base64'),
  )
  return JSON.parse(Buffer.from(plain).toString('utf8'))
}

async function adminToken() {
  cryptoConfig = (await api('/api/crypto/config')).data
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(cryptoConfig.publicKey)
  const encrypted = encryptor.encrypt(adminPassword)
  assert(encrypted, 'admin password encryption failed')
  const result = await api('/api/auth/login', {
    method: 'POST', body: { username: 'admin', password: encrypted, rememberMe: false },
  })
  const data = await decryptAdminData(result.data, cryptoConfig.aesKey)
  return { name: 'Authorization', value: data.token }
}

async function adminApi(path, options) {
  const result = await api(path, options)
  return { ...result, data: await decryptAdminData(result.data, cryptoConfig.aesKey) }
}

async function register(label) {
  const username = `m4san_${label}_${stamp}`
  const email = `${username}@example.test`
  const created = await api('/api/v1/auth/register', {
    method: 'POST', body: { username, email, password, displayName: username },
  })
  const session = await login(email)
  const blog = await api('/api/v1/blogs/me', { token: session })
  return { id: created.data.userId, email, blogId: blog.data.blogId, ...session }
}

async function login(email, codes = [200]) {
  const result = await api('/api/v1/auth/login', {
    method: 'POST', body: { email, password }, codes,
  })
  if (codes.includes(200)) {
    return { name: result.data.tokenName, value: result.data.tokenValue }
  }
  return result
}

async function issue(admin, targetUserId, sanctionType, expiresAt = undefined) {
  return adminApi('/admin-api/community/sanctions', {
    method: 'POST', token: admin,
    body: { targetUserId, sanctionType, reasonCode: 'M4_E2E', reasonNote: 'M4 sanction lifecycle acceptance', expiresAt },
  })
}

async function revoke(admin, sanctionId) {
  return adminApi(`/admin-api/community/sanctions/${sanctionId}/revoke`, {
    method: 'POST', token: admin, body: { note: 'M4 E2E revoke' },
  })
}

assert((await api('/api/v1/health')).data.status === 'UP', 'backend health')
const target = await register('target')
const admin = await adminToken()
const initialMoment = await api('/api/v1/moments', {
  method: 'POST', token: target,
  body: { blogId: target.blogId, momentType: 'TEXT', textContent: 'M4 sanction target', visibility: 'PUBLIC' },
})
const momentId = initialMoment.data.moment.momentId

const commentBan = await issue(admin, target.id, 'COMMENT_BAN', localDateTime(120))
assert(commentBan.data.status === 'ACTIVE', 'comment ban issued')
const userHistory = await api('/api/v1/sanctions/me', { token: target })
assert(userHistory.data.some((item) => item.id === commentBan.data.id && item.type === 'COMMENT_BAN'), 'user sees sanction history')
const deniedComment = await api(`/api/v1/interactions/MOMENT/${momentId}/comments`, {
  method: 'POST', token: target, body: { content: 'must be denied' }, codes: [403],
})
assert(deniedComment.code === 403, 'comment ban enforced')
assert((await revoke(admin, commentBan.data.id)).data.status === 'REVOKED', 'comment ban revoked')
await api(`/api/v1/interactions/MOMENT/${momentId}/comments`, {
  method: 'POST', token: target, body: { content: 'allowed after revoke' },
})

const momentBan = await issue(admin, target.id, 'MOMENT_BAN', localDateTime(120))
const deniedMoment = await api('/api/v1/moments', {
  method: 'POST', token: target,
  body: { blogId: target.blogId, momentType: 'TEXT', textContent: 'must be denied', visibility: 'PUBLIC' },
  codes: [403],
})
assert(deniedMoment.code === 403, 'moment ban enforced')
await revoke(admin, momentBan.data.id)

const rateLimit = await issue(admin, target.id, 'RATE_LIMIT', localDateTime(120))
await api('/api/v1/moments', {
  method: 'POST', token: target,
  body: { blogId: target.blogId, momentType: 'TEXT', textContent: 'first rate-limited action', visibility: 'PUBLIC' },
})
const deniedRate = await api('/api/v1/moments', {
  method: 'POST', token: target,
  body: { blogId: target.blogId, momentType: 'TEXT', textContent: 'second rate-limited action', visibility: 'PUBLIC' },
  codes: [429],
})
assert(deniedRate.code === 429, 'rate limit enforced')
await revoke(admin, rateLimit.data.id)

const loginSuspend = await issue(admin, target.id, 'LOGIN_SUSPEND', localDateTime(120))
const deniedLogin = await login(target.email, [403])
assert(deniedLogin.code === 403, 'login suspend enforced')
await revoke(admin, loginSuspend.data.id)
await login(target.email)

const expiring = await issue(admin, target.id, 'COMMENT_BAN', localDateTime(2))
await new Promise((resolve) => setTimeout(resolve, 3000))
const expiredHistory = await api('/api/v1/sanctions/me', { token: target })
assert(expiredHistory.data.some((item) => item.id === expiring.data.id && item.status === 'EXPIRED'), 'expired sanction reconciled')
await api(`/api/v1/interactions/MOMENT/${momentId}/comments`, {
  method: 'POST', token: target, body: { content: 'allowed after expiry' },
})

const adminHistory = await adminApi(`/admin-api/community/sanctions/users/${target.id}`, { token: admin })
assert(adminHistory.data.filter((item) => item.status === 'REVOKED').length >= 3, 'admin sees revoked history')
console.log(`M4 sanction lifecycle E2E passed: user=${target.id} moment=${momentId}`)
