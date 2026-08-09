const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8861'
const stamp = new Date().toISOString().replace(/\D/g, '').slice(4, 17)
const password = 'M4-anti-abuse-password-1'

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

async function api(path, { method = 'GET', body, token, codes = [200] } = {}) {
  const headers = body ? { 'Content-Type': 'application/json' } : {}
  if (token) headers['pxczxn-community-token'] = token
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

async function register(label) {
  const username = `m4ab_${label}_${stamp}`
  const email = `${username}@example.test`
  const created = await api('/api/v1/auth/register', {
    method: 'POST', body: { username, email, password, displayName: username },
  })
  return { id: created.data.userId, email }
}

async function publishMoment(token, text, codes = [200]) {
  return api('/api/v1/moments', {
    method: 'POST', token, codes,
    body: {
      momentType: 'TEXT', textContent: text, visibility: 'PUBLIC',
      blogId: null, linkUrl: null, articleId: null, repostMomentId: null,
    },
  })
}

assert((await api('/api/v1/health')).data.status === 'UP', 'backend health')
const reporter = await register('reporter')
const session = await api('/api/v1/auth/login', {
  method: 'POST', body: { email: reporter.email, password },
})
const token = session.data.tokenValue

const duplicateText = `M4 duplicate content ${stamp}`
assert((await publishMoment(token, duplicateText)).data.moment.status === 'PUBLISHED', 'first moment publish')
const duplicate = await publishMoment(token, duplicateText, [429])
assert(duplicate.code === 429, 'duplicate moment must be rejected')

for (let index = 0; index < 6; index += 1) {
  assert((await publishMoment(token, `M4 rate content ${stamp}-${index}`)).data.moment.status === 'PUBLISHED', 'rate-limit setup publish')
}
const momentLimit = await publishMoment(token, `M4 rate content ${stamp}-rejected`, [429])
assert(momentLimit.code === 429, 'moment publication rate must be enforced')

const targets = []
for (let index = 0; index < 7; index += 1) targets.push(await register(`target${index}`))
for (let index = 0; index < 6; index += 1) {
  const report = await api('/api/v1/reports', {
    method: 'POST', token,
    body: { targetType: 'USER', targetId: targets[index].id, reasonCode: 'ABUSE', description: `M4 rate report ${index}` },
  })
  assert(report.data.status === 'PENDING', 'report creation')
}
const reportLimit = await api('/api/v1/reports', {
  method: 'POST', token, codes: [429],
  body: { targetType: 'USER', targetId: targets[6].id, reasonCode: 'ABUSE', description: 'M4 rate report rejected' },
})
assert(reportLimit.code === 429, 'report rate must be enforced')

const missingEmail = `missing_${stamp}@example.test`
for (let index = 0; index < 10; index += 1) {
  const failed = await api('/api/v1/auth/login', {
    method: 'POST', codes: [401], body: { email: missingEmail, password },
  })
  assert(failed.code === 401, 'unknown login remains credential failure before rate limit')
}
const loginLimit = await api('/api/v1/auth/login', {
  method: 'POST', codes: [429], body: { email: missingEmail, password },
})
assert(loginLimit.code === 429, 'login rate must be enforced')

console.log(`M4 anti-abuse E2E passed: reporter=${reporter.id}`)
