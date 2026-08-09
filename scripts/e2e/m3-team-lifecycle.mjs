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
  // 我的团队 / 工作台 / 活动（M3.2 智能入口与工作台接口）
  const myTeams = await api('/api/v1/teams/me', { token: member })
  const myTeam = myTeams.data.find((value) => String(value.teamId) === String(teamId))
  assert(myTeam && myTeam.viewerRole === 'OWNER', `my teams membership: ${JSON.stringify(myTeams.data)}`)
  assert(Number.isInteger(myTeam.memberCount) && Number.isInteger(myTeam.articleCount) && Number.isInteger(myTeam.followerCount), 'my team counters are JSON numbers')
  const dashboard = await api(`/api/v1/teams/${teamId}/dashboard`, { token: member })
  assert(dashboard.data.viewerRole === 'OWNER' && Array.isArray(dashboard.data.capabilities), 'dashboard viewer role')
  assert(dashboard.data.stats && Number.isInteger(dashboard.data.stats.publishedArticleCount), 'dashboard stats present')
  assert(dashboard.data.todos && Number.isInteger(dashboard.data.todos.pendingSubmissionCount), 'dashboard todos present')
  assert(Array.isArray(dashboard.data.recentArticles) && Array.isArray(dashboard.data.recentActivities), 'dashboard lists present')
  const outsiderDashboard = await api(`/api/v1/teams/${teamId}/dashboard`, { token: outsider, codes: [403] })
  assert(outsiderDashboard.code === 403, 'outsider dashboard denied')
  const outsiderActivities = await api(`/api/v1/teams/${teamId}/activities`, { token: outsider, codes: [403] })
  assert(outsiderActivities.code === 403, 'outsider activities denied')
  const activities = await api(`/api/v1/teams/${teamId}/activities?limit=20`, { token: member })
  assert(activities.data.some((value) => value.eventType === 'OWNERSHIP_TRANSFERRED'), `ownership activity recorded: ${JSON.stringify(activities.data)}`)
  await api(`/api/v1/teams/${teamId}/leave`, { method: 'POST', token: owner })
  const formerOwner = await api(`/api/v1/teams/${teamId}/members`, { token: owner, codes: [403] })
  assert(formerOwner.code === 403, 'former owner access revoked after leave')
  const formerOwnerTeams = await api('/api/v1/teams/me', { token: owner })
  assert(!formerOwnerTeams.data.some((value) => String(value.teamId) === String(teamId)), 'left team removed from my teams')
  const remaining = await api(`/api/v1/teams/${teamId}/members`, { token: member })
  assert(remaining.data.length === 1 && String(remaining.data[0].userId) === String(member.id), 'member leave')
  const article = await api('/api/v1/articles', { method: 'POST', token: owner, body: {
    title: `M3 collaboration ${stamp}`, slug: `m3-collab-${stamp}`, summary: 'M3 collaboration E2E',
    contentMode: 'MARKDOWN', markdownContent: 'M3 collaboration acceptance content.', visibility: 'PRIVATE', publishMethod: 'MANUAL'
  } })
  const acceptedInvitation = await api(`/api/v1/articles/${article.data.articleId}/collaborators/invitations`, { method: 'POST', token: owner, body: {
    inviteeUserId: outsider.id, contributionType: 'CO_AUTHOR', canEdit: false, attributionOrder: 1, idempotencyKey: `m3-collab-accept-${stamp}`
  } })
  const accepted = await api(`/api/v1/articles/collaboration-invitations/${acceptedInvitation.data.id}/accept`, { method: 'POST', token: outsider, body: { expectedLockVersion: acceptedInvitation.data.lockVersion } })
  assert(accepted.data.status === 'ACCEPTED' && String(accepted.data.userId) === String(outsider.id), 'collaboration acceptance')
  const collaborators = await api(`/api/v1/articles/${article.data.articleId}/collaborators`, { token: owner })
  assert(collaborators.data.some((value) => String(value.userId) === String(outsider.id)), 'accepted collaborator attribution')
  const rejectedInvitation = await api(`/api/v1/articles/${article.data.articleId}/collaborators/invitations`, { method: 'POST', token: owner, body: {
    inviteeUserId: member.id, contributionType: 'RESEARCH', canEdit: false, attributionOrder: 2, idempotencyKey: `m3-collab-reject-${stamp}`
  } })
  await api(`/api/v1/articles/collaboration-invitations/${rejectedInvitation.data.id}/reject`, { method: 'POST', token: member, body: { expectedLockVersion: rejectedInvitation.data.lockVersion } })
  const pending = await api('/api/v1/articles/collaboration-invitations/me', { token: member })
  assert(!pending.data.some((value) => String(value.id) === String(rejectedInvitation.data.id)), 'collaboration rejection')
  const submission = await api('/api/v1/team-submissions', { method: 'POST', token: owner, body: {
    sourceArticleId: article.data.articleId, targetTeamId: teamId, idempotencyKey: `m3-submission-${stamp}`
  } })
  assert(String(submission.data.fixedSourceVersionId) === String(article.data.currentVersionId), 'fixed submission version')
  const teamReviewed = await api(`/api/v1/team-submissions/${submission.data.id}/team/approve`, { method: 'POST', token: member, body: {
    expectedLockVersion: submission.data.lockVersion, comment: 'M3 team review'
  } })
  assert(teamReviewed.data.status === 'PLATFORM_PENDING', 'team submission approval')
  const platformReviewed = await adminApi(`/admin-api/community/team-submissions/${submission.data.id}/approve`, { method: 'POST', token: admin, body: {
    expectedLockVersion: teamReviewed.data.lockVersion, comment: 'M3 platform review'
  } })
  assert(platformReviewed.data.status === 'PUBLISHED' && platformReviewed.data.publishedTeamArticleId, 'platform submission publication')
  const series = await api(`/api/v1/teams/${teamId}/series`, { method: 'POST', token: member, body: {
    title: `M3 Series ${stamp}`, slug: `m3-series-${stamp}`, summary: 'M3 series E2E', serializationStatus: 'ONGOING'
  } })
  const ordered = await api(`/api/v1/series/${series.data.id}/chapters`, { method: 'POST', token: member, body: {
    articleIds: [platformReviewed.data.publishedTeamArticleId], expectedLockVersion: series.data.lockVersion
  } })
  assert(String(ordered.data.chapters[0].articleId) === String(platformReviewed.data.publishedTeamArticleId), 'series chapter ordering')
  const submittedSeries = await api(`/api/v1/series/${series.data.id}/submit-review`, { method: 'POST', token: member, body: { expectedLockVersion: ordered.data.lockVersion } })
  const approvedSeries = await adminApi(`/admin-api/community/series/${series.data.id}/approve`, { method: 'POST', token: admin, body: {
    expectedLockVersion: submittedSeries.data.lockVersion, comment: 'M3 series review'
  } })
  assert(approvedSeries.data.reviewStatus === 'APPROVED', 'series platform approval')

  // 团队直接创作:成员在团队博客创建文章(blogId),出现在工作台内容列表
  const teamBlogId = dashboard.data.team.blogId
  const teamSlugNow = dashboard.data.team.slug
  const direct = await api('/api/v1/articles', { method: 'POST', token: member, body: {
    blogId: String(teamBlogId), title: `M3 direct ${stamp}`, slug: `m3-direct-${stamp}`,
    summary: 'direct team article', contentMode: 'MARKDOWN', markdownContent: 'Direct team article.',
    visibility: 'PRIVATE', publishMethod: 'MANUAL'
  } })
  assert(String(direct.data.blogId) === String(teamBlogId), 'team direct creation targets team blog')
  const contentList = await api(`/api/v1/teams/${teamId}/articles`, { token: member })
  assert(contentList.data.some((value) => value.slug === `m3-direct-${stamp}`), 'direct article appears in workspace content')

  // 系列归属:已发布团队文章携带所属系列
  const publishedRow = contentList.data.find((value) => String(value.articleId) === String(platformReviewed.data.publishedTeamArticleId))
  assert(publishedRow && String(publishedRow.seriesId) === String(series.data.id) && publishedRow.seriesTitle === `M3 Series ${stamp}`, `series membership on content row: ${JSON.stringify(publishedRow)}`)

  // 投稿重提交:要求修改后重新提交并 supersedes 原投稿
  const sub2 = await api('/api/v1/team-submissions', { method: 'POST', token: owner, body: {
    sourceArticleId: article.data.articleId, targetTeamId: teamId, idempotencyKey: `m3-submission-2-${stamp}`
  } })
  const revised = await api(`/api/v1/team-submissions/${sub2.data.id}/team/revision`, { method: 'POST', token: member, body: {
    expectedLockVersion: sub2.data.lockVersion, comment: 'M3 要求修改'
  } })
  assert(revised.data.status === 'TEAM_REVISION_REQUIRED', 'team revision request')
  const resubmit = await api('/api/v1/team-submissions', { method: 'POST', token: owner, body: {
    sourceArticleId: article.data.articleId, targetTeamId: teamId,
    supersedesSubmissionId: sub2.data.id, idempotencyKey: `m3-submission-3-${stamp}`
  } })
  assert(String(resubmit.data.supersedesSubmissionId) === String(sub2.data.id), 'resubmission supersedes previous')

  // 团队设置字段 + 公开页 settings + 投稿开关
  const portalSettingsUpdate = await api(`/api/v1/teams/${teamId}`, { method: 'PATCH', token: member, body: {
    name: myTeam.name, summary: myTeam.summary, avatarFileId: null, backgroundFileId: null,
    category: '技术社区', contentDirection: 'AI 应用研究', theme: 'default',
    seoTitle: 'M3 SEO 标题', seoDescription: 'M3 SEO 描述',
    publicMembers: false, allowSubmissions: false,
    submissionGuideline: '欢迎投稿，需绑定固定版本。', contactInfo: 'contact@example.test'
  } })
  assert(portalSettingsUpdate.data && portalSettingsUpdate.code === 200, 'portal settings patch')
  const portal2 = await api(`/api/v1/teams/slug/${teamSlugNow}`, {}, false)
  const settings = portal2.data.team ? portal2.data.settings : portal2.data.team?.settings ?? portal2.data
  assert(settings.category === '技术社区' && settings.publicMembers === false && settings.allowSubmissions === false, `public portal settings: ${JSON.stringify(settings)}`)
  const outsiderArticle = await api('/api/v1/articles', { method: 'POST', token: outsider, body: {
    title: `M3 outsider ${stamp}`, slug: `m3-outsider-${stamp}`, summary: 'outsider',
    contentMode: 'MARKDOWN', markdownContent: 'Outsider article.', visibility: 'PRIVATE', publishMethod: 'MANUAL'
  } })
  const closedDenied = await api('/api/v1/team-submissions', { method: 'POST', token: outsider, body: {
    sourceArticleId: outsiderArticle.data.articleId, targetTeamId: teamId, idempotencyKey: `m3-submission-4-${stamp}`
  }, codes: [403] })
  assert(closedDenied.code === 403, 'closed submissions reject outsiders')
  await api(`/api/v1/teams/${teamId}`, { method: 'PATCH', token: member, body: {
    name: myTeam.name, summary: myTeam.summary, avatarFileId: null, backgroundFileId: null,
    category: '技术社区', contentDirection: 'AI 应用研究', theme: 'default',
    seoTitle: 'M3 SEO 标题', seoDescription: 'M3 SEO 描述',
    publicMembers: true, allowSubmissions: true,
    submissionGuideline: '欢迎投稿，需绑定固定版本。', contactInfo: 'contact@example.test'
  } })
  console.log(`M3 team lifecycle E2E passed: team=${teamId}`)
} finally {
  // Team audit events are intentionally immutable. Run this script only against
  // a disposable E2E database and remove that database after the process exits.
}
