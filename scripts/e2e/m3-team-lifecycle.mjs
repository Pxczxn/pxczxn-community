import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'

const requireFromAdmin = createRequire(new URL('../../pxczxn-admin/package.json', import.meta.url))
const JSEncrypt = requireFromAdmin('jsencrypt')
const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8849'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'
const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 17)
const users = [`m3own_${stamp}`, `m3mem_${stamp}`, `m3out_${stamp}`]
const emails = users.map((name) => `${name}@example.test`)
const password = 'M3-e2e-password-1'
let teamId
let cryptoConfig

function assert(condition, message) { if (!condition) throw new Error(message) }
async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body ? { 'Content-Type': 'application/json' } : {}
  if (token) headers[token.name] = token.value
  const response = await fetch(`${baseUrl}${path}`, { method, headers, body: body ? JSON.stringify(body) : undefined })
  const payload = await response.json()
  if (!codes.includes(Number(payload.code))) throw new Error(`${method} ${path}: ${JSON.stringify(payload)}`)
  return payload
}
async function adminToken() {
  const config = (await api('/api/crypto/config')).data; cryptoConfig = config
  const encryptor = new JSEncrypt(); encryptor.setPublicKey(config.publicKey)
  const encrypted = encryptor.encrypt(adminPassword); assert(encrypted, 'admin password encryption failed')
  const result = await api('/api/auth/login', { method: 'POST', body: { username: 'admin', password: encrypted, rememberMe: false } })
  const data = await decryptAdminData(result.data, config.aesKey)
  return { name: 'Authorization', value: data.token }
}
async function adminApi(path, options) {
  const result = await api(path, options)
  return { ...result, data: await decryptAdminData(result.data, cryptoConfig.aesKey) }
}
async function decryptAdminData(value, aesKey) {
  if (typeof value !== 'string' || !value.includes('.')) return value
  const [iv, payload] = value.split('.')
  const key = await webcrypto.subtle.importKey('raw', Buffer.from(aesKey, 'base64'), { name: 'AES-GCM' }, false, ['decrypt'])
  const plain = await webcrypto.subtle.decrypt({ name: 'AES-GCM', iv: Buffer.from(iv, 'base64') }, key, Buffer.from(payload, 'base64'))
  return JSON.parse(Buffer.from(plain).toString('utf8'))
}
async function register(index) {
  const created = await api('/api/v1/auth/register', { method: 'POST', body: { username: users[index], email: emails[index], password, displayName: users[index] } })
  const session = await api('/api/v1/auth/login', { method: 'POST', body: { email: emails[index], password } })
  return { id: created.data.userId, name: session.data.tokenName, value: session.data.tokenValue }
}
try {
  assert((await api('/api/v1/health')).data.status === 'UP', 'backend health')
  const [owner, member, outsider] = await Promise.all([register(0), register(1), register(2)])
  const key = `m3-team-${stamp}`
  const first = await api('/api/v1/team-applications', { method: 'POST', token: owner, body: { teamName: `M3 Team ${stamp}`, teamSlug: `m3-team-${stamp}`, description: 'M3 E2E', idempotencyKey: key } })
  const replay = await api('/api/v1/team-applications', { method: 'POST', token: owner, body: { teamName: `M3 Team ${stamp}`, teamSlug: `m3-team-${stamp}`, description: 'M3 E2E', idempotencyKey: key } })
  assert(first.data.id === replay.data.id, 'team application idempotency')
  const admin = await adminToken()
  const approved = await adminApi(`/admin-api/community/team-applications/${first.data.id}/approve`, { method: 'POST', token: admin, body: { reviewComment: 'M3 E2E approval' } })
  teamId = approved.data.teamId
  const invitation = await api(`/api/v1/teams/${teamId}/invitations`, { method: 'POST', token: owner, body: { userId: member.id, roleCode: 'EDITOR', idempotencyKey: `m3-invite-${stamp}` } })
  const invitationReplay = await api(`/api/v1/teams/${teamId}/invitations`, { method: 'POST', token: owner, body: { userId: member.id, roleCode: 'EDITOR', idempotencyKey: `m3-invite-${stamp}` } })
  assert(invitation.data.id === invitationReplay.data.id, 'team invitation idempotency')
  const forbidden = await api(`/api/v1/teams/${teamId}/members`, { token: outsider, codes: [403] })
  assert(forbidden.code === 403, 'outsider team access denied')
  await api(`/api/v1/teams/invitations/${invitation.data.id}/accept`, { method: 'POST', token: member })
  await api(`/api/v1/teams/${teamId}/owner`, { method: 'POST', token: owner, body: { userId: member.id } })
  const members = await api(`/api/v1/teams/${teamId}/members`, { token: member })
  assert(members.data.some((value) => String(value.userId) === String(member.id) && value.roleCode === 'OWNER'), `owner transfer: ${JSON.stringify(members.data)}`)
  console.log(`M3 team lifecycle E2E passed: team=${teamId}`)
} finally {
  // Team audit events are intentionally immutable. Run this script only against
  // a disposable E2E database and remove that database after the process exits.
}
