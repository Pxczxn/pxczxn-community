import { constants, publicEncrypt, webcrypto } from 'node:crypto'

const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8861'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'
const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 17)
const password = 'M4-block-e2e-password-1'
let cryptoConfig

function assert(condition, message) { if (!condition) throw new Error(message) }

async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body === undefined ? {} : { 'Content-Type': 'application/json' }
  if (token) headers[token.name] = token.value
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const payload = await response.json()
  if (!codes.includes(Number(payload.code))) throw new Error(`${method} ${path}: ${JSON.stringify(payload)}`)
  return payload
}

async function register(label) {
  const username = `m4blk_${label}_${stamp}`
  const email = `${username}@example.test`
  const created = await api('/api/v1/auth/register', {
    method: 'POST', body: { username, email, password, displayName: username },
  })
  const session = await api('/api/v1/auth/login', {
    method: 'POST', body: { email, password },
  })
  const blog = await api('/api/v1/blogs/me', { token: { name: session.data.tokenName, value: session.data.tokenValue } })
  return {
    id: created.data.userId,
    blogId: blog.data.blogId,
    name: session.data.tokenName,
    value: session.data.tokenValue,
  }
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
  const publicKey = cryptoConfig.publicKey.includes('BEGIN')
    ? cryptoConfig.publicKey
    : `-----BEGIN PUBLIC KEY-----\n${cryptoConfig.publicKey.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
  const encrypted = publicEncrypt(
    { key: publicKey, padding: constants.RSA_PKCS1_PADDING }, Buffer.from(adminPassword),
  ).toString('base64')
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

assert((await api('/api/v1/health')).data.status === 'UP', 'backend health')
const blocker = await register('blocker')
const target = await register('target')
const published = await api('/api/v1/moments', {
  method: 'POST', token: target,
  body: { blogId: target.blogId, momentType: 'TEXT', textContent: 'M4 block lifecycle', visibility: 'PUBLIC' },
})
const momentId = published.data.moment.momentId

await api(`/api/v1/blogs/${target.blogId}/follow`, { method: 'POST', token: blocker })
await api(`/api/v1/blogs/${blocker.blogId}/follow`, { method: 'POST', token: target })
const beforeBlock = await api('/api/v1/moments', { token: blocker })
assert(beforeBlock.data.records.some((item) => String(item.momentId) === String(momentId)), 'visible moment before block')
await api('/api/v1/chat/messages', {
  method: 'POST', token: blocker, body: { recipientUserId: target.id, contentText: 'before block' },
})

const firstBlock = await api('/api/v1/blocks', {
  method: 'POST', token: blocker, body: { targetType: 'USER', targetId: target.id },
})
const replay = await api('/api/v1/blocks', {
  method: 'POST', token: blocker, body: { targetType: 'USER', targetId: target.id },
})
assert(firstBlock.data.id === replay.data.id, 'user block idempotency')
const blocks = await api('/api/v1/blocks', { token: blocker })
assert(blocks.data.some((item) => item.targetType === 'USER' && String(item.targetId) === String(target.id)), 'block list')
const hiddenMoment = await api(`/api/v1/moments/${momentId}`, { token: blocker, codes: [404] })
assert(hiddenMoment.code === 404, 'blocked moment detail')
const hiddenFeed = await api('/api/v1/moments', { token: blocker })
assert(!hiddenFeed.data.records.some((item) => String(item.momentId) === String(momentId)), 'blocked moment feed')
const hiddenNotification = await api('/api/v1/notifications', { token: blocker })
assert(!hiddenNotification.data.records.some((item) => String(item.sender?.userId) === String(target.id)), 'blocked notification sender')
const blockedChat = await api('/api/v1/chat/messages', {
  method: 'POST', token: target, body: { recipientUserId: blocker.id, contentText: 'must be blocked' }, codes: [403],
})
assert(blockedChat.code === 403, 'user block restricts reverse direct chat')

await api(`/api/v1/blocks/USER/${target.id}`, { method: 'DELETE', token: blocker })
await api('/api/v1/chat/messages', {
  method: 'POST', token: target, body: { recipientUserId: blocker.id, contentText: 'after user unblock' },
})
await api('/api/v1/blocks', {
  method: 'POST', token: blocker, body: { targetType: 'BLOG', targetId: target.blogId },
})
const hiddenBlogFeed = await api(`/api/v1/blogs/${target.blogId}/moments`, { token: blocker })
assert(!hiddenBlogFeed.data.records.some((item) => String(item.momentId) === String(momentId)), 'blocked blog moment feed')
await api(`/api/v1/blocks/BLOG/${target.blogId}`, { method: 'DELETE', token: blocker })
const admin = await adminToken()
const createdTag = await adminApi('/admin-api/community/tags', {
  method: 'POST', token: admin,
  body: { name: `M4 Tag ${stamp}`, slug: `m4-tag-${stamp}`, description: 'M4 block list E2E' },
})
const tagId = createdTag.data.tagId
const tags = await api('/api/v1/tags', { token: blocker })
assert(tags.data.some((tag) => String(tag.tagId) === String(tagId)), 'active tag fixture')
await api('/api/v1/blocks', {
  method: 'POST', token: blocker, body: { targetType: 'TAG', targetId: tagId },
})
assert((await api('/api/v1/blocks', { token: blocker })).data.some((item) => item.targetType === 'TAG' && String(item.targetId) === String(tagId)), 'tag block list')
await api(`/api/v1/blocks/TAG/${tagId}`, { method: 'DELETE', token: blocker })
await adminApi(`/admin-api/community/tags/${tagId}`, { method: 'DELETE', token: admin })
await api('/api/v1/blocks', {
  method: 'POST', token: blocker, body: { targetType: 'CHAT', targetId: target.id },
})
const chatBlocked = await api('/api/v1/chat/messages', {
  method: 'POST', token: target, body: { recipientUserId: blocker.id, contentText: 'chat target blocked' }, codes: [403],
})
assert(chatBlocked.code === 403, 'chat block restricts reverse direct chat')
await api(`/api/v1/blocks/CHAT/${target.id}`, { method: 'DELETE', token: blocker })
await api('/api/v1/chat/messages', {
  method: 'POST', token: target, body: { recipientUserId: blocker.id, contentText: 'after chat unblock' },
})

console.log(`M4 block list lifecycle E2E passed: blocker=${blocker.id} target=${target.id} moment=${momentId}`)
