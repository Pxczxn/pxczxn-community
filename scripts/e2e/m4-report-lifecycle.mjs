import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'

const requireFromAdmin = createRequire(new URL('../../pxczxn-admin/package.json', import.meta.url))
const JSEncrypt = requireFromAdmin('jsencrypt')
const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8861'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'
const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 17)
const password = 'M4-e2e-password-1'
let cryptoConfig

function assert(condition, message) { if (!condition) throw new Error(message) }

async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body ? { 'Content-Type': 'application/json' } : {}
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
  const username = `m4rep_${label}_${stamp}`
  const email = `${username}@example.test`
  const created = await api('/api/v1/auth/register', {
    method: 'POST', body: { username, email, password, displayName: username },
  })
  const session = await api('/api/v1/auth/login', {
    method: 'POST', body: { email, password },
  })
  return { id: created.data.userId, name: session.data.tokenName, value: session.data.tokenValue }
}

assert((await api('/api/v1/health')).data.status === 'UP', 'backend health')
const reporter = await register('reporter')
const target = await register('target')
const created = await api('/api/v1/reports', {
  method: 'POST', token: reporter,
  body: {
    targetType: 'USER', targetId: target.id, reasonCode: 'ABUSE',
    description: 'M4 report lifecycle acceptance', evidenceJson: JSON.stringify({ source: 'm4-e2e' }),
  },
})
assert(created.data.status === 'PENDING' && created.data.lockVersion === 0, 'report creation')
const duplicate = await api('/api/v1/reports', {
  method: 'POST', token: reporter,
  body: {
    targetType: 'USER', targetId: target.id, reasonCode: 'ABUSE',
    description: 'M4 duplicate', evidenceJson: JSON.stringify({ source: 'm4-e2e' }),
  }, codes: [409],
})
assert(duplicate.code === 409, 'active report deduplication')

const admin = await adminToken()
const pending = await adminApi('/admin-api/community/reports?status=PENDING', { token: admin })
const queued = pending.data.find((report) => String(report.id) === String(created.data.id))
assert(queued && queued.status === 'PENDING', 'admin pending queue')
const claimed = await adminApi(`/admin-api/community/reports/${created.data.id}/claim`, {
  method: 'POST', token: admin, body: { expectedLockVersion: queued.lockVersion },
})
assert(claimed.data.status === 'ASSIGNED' && claimed.data.lockVersion === 1, 'admin claim')
const staleClaim = await adminApi(`/admin-api/community/reports/${created.data.id}/claim`, {
  method: 'POST', token: admin, body: { expectedLockVersion: queued.lockVersion }, codes: [409],
})
assert(staleClaim.code === 409, 'optimistic locking')
const resolved = await adminApi(`/admin-api/community/reports/${created.data.id}/resolve`, {
  method: 'POST', token: admin,
  body: { expectedLockVersion: claimed.data.lockVersion, resolutionCode: 'ACTION_TAKEN', resolutionNote: 'M4 E2E resolution' },
})
assert(resolved.data.status === 'RESOLVED' && resolved.data.lockVersion === 2, 'admin resolution')
const mine = await api('/api/v1/reports/me', { token: reporter })
const visible = mine.data.find((report) => String(report.id) === String(created.data.id))
assert(visible && visible.status === 'RESOLVED' && visible.resolutionCode === 'ACTION_TAKEN', 'reporter final state')
const remaining = await adminApi('/admin-api/community/reports', { token: admin })
assert(!remaining.data.some((report) => String(report.id) === String(created.data.id)), 'resolved report leaves active queue')

console.log(`M4 report lifecycle E2E passed: report=${created.data.id}`)
