import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

async function source(path) {
  return readFile(new URL(`../${path}`, import.meta.url), 'utf8')
}

test('development ports target the accepted local topology', async () => {
  const viteConfig = await source('vite.config.ts')

  assert.match(viteConfig, /port:\s*8848/)
  assert.match(viteConfig, /strictPort:\s*true/)
  assert.match(viteConfig, /http:\/\/localhost:8849/)
  assert.match(viteConfig, /['"]\/admin-api['"]/)
})

test('community governance routes and APIs remain registered', async () => {
  const [router, api] = await Promise.all([
    source('src/router/index.ts'),
    source('src/api/community.ts')
  ])

  for (const area of ['comments', 'moments', 'interactions', 'reviews']) {
    assert.match(router, new RegExp(`community/${area}`))
    assert.match(api, new RegExp(`/admin-api/community/${area}`))
  }
})

test('disabled SMS capability does not expose admin calls or automatic polling', async () => {
  const [configApi, configPage] = await Promise.all([
    source('src/api/org.ts'),
    source('src/views/system/config/index.vue')
  ])

  assert.doesNotMatch(configApi, /\/sys\/config-group\/test-sms/)
  assert.doesNotMatch(configApi, /\/sys\/config-group\/sms-logs/)
  assert.doesNotMatch(configPage, /loadRecentSmsLogs|handleTestSms|handleShowAllSmsLogs/)
  assert.match(configPage, /当前社区版本未启用短信服务/)
})

test('team governance and collaboration queries use real admin APIs', async () => {
  const [router, api, teams] = await Promise.all([
    source('src/router/index.ts'),
    source('src/api/community.ts'),
    source('src/views/community/teams/index.vue')
  ])

  assert.match(router, /views\/community\/teams\/index\.vue/)
  assert.match(api, /\/admin-api\/community\/teams/)
  assert.match(api, /\/admin-api\/community\/collaborations/)
  assert.match(teams, /communityApi\.teams/)
  assert.match(teams, /communityApi\.collaborations/)
})

test('the three supported visual themes remain available', async () => {
  const themeStore = await source('src/stores/theme.ts')

  for (const theme of ['light-theme', 'dark-theme', 'starry-theme']) {
    assert.match(themeStore, new RegExp(theme))
  }
})

test('community pagination matches the backend contract and normalizes numeric strings', async () => {
  const api = await source('src/api/community.ts')

  assert.match(api, /pageNum:\s*number/)
  assert.doesNotMatch(api, /\n\s*page:\s*number/)
  assert.match(api, /total:\s*Number\(result\.total\)/)
  assert.match(api, /pageNum:\s*Number\(result\.pageNum\)/)
  assert.match(api, /pageSize:\s*Number\(result\.pageSize\)/)
})

test('wide community tables stay inside the content card', async () => {
  const styles = await source('src/styles/index.scss')
  const layoutShrinkRule = styles.match(
    /\.n-layout,\s*\n\.n-layout-content,[\s\S]*?\n\}/
  )?.[0]

  assert.match(styles, /grid-template-columns:\s*minmax\(0,\s*1fr\)/)
  assert.match(styles, /\.n-data-table-wrapper\s*\{[\s\S]*?max-width:\s*100%/)
  assert.ok(layoutShrinkRule)
  assert.match(layoutShrinkRule, /min-width:\s*0\s*!important/)
  assert.doesNotMatch(layoutShrinkRule, /overflow:\s*visible/)
})

test('community forms and filters use shared validation and sizing rules', async () => {
  const [styles, contentRules] = await Promise.all([
    source('src/styles/index.scss'),
    source('src/views/community/content-rules/index.vue')
  ])

  assert.match(styles, /\.filter-select\s*\{[\s\S]*?width:\s*150px/)
  for (const size of ['sm', 'md', 'lg']) {
    assert.match(styles, new RegExp(`\\.dialog-form-${size}\\s*\\{`))
  }
  assert.match(contentRules, /ref="formRef"/)
  assert.match(contentRules, /:rules="formRules"/)
  assert.match(contentRules, /formRef\.value\?\.validate\(\)/)
})

test('websocket authentication uses one-time tickets instead of session tokens', async () => {
  const [websocket, serverManager, authApi] = await Promise.all([
    source('src/utils/websocket.ts'),
    source('src/views/monitor/server-manager/index.vue'),
    source('src/api/auth.ts')
  ])

  assert.match(authApi, /issueWebSocketTicket/)
  assert.match(websocket, /\?ticket=/)
  assert.doesNotMatch(websocket, /\?token=/)
  assert.match(serverManager, /\?ticket=/)
  assert.doesNotMatch(serverManager, /\?token=/)
})

test('instant chat persists through HTTP and uses WebSocket only for events', async () => {
  const [chatApi, chatPage, websocket] = await Promise.all([
    source('src/api/message.ts'),
    source('src/views/message/chat/index.vue'),
    source('src/utils/websocket.ts')
  ])

  for (const route of [
    '/sys/chat/send',
    '/sys/chat/contacts',
    '/sys/chat/unread-count',
    '/chat/group/create',
    '/chat/group/${groupId}/message',
    '/chat/group/${groupId}/read'
  ]) {
    assert.match(chatApi, new RegExp(
      route.replaceAll('/', '\\/').replaceAll('$', '\\$')
    ))
  }
  assert.match(chatPage, /chatApi\.send/)
  assert.match(chatPage, /groupChatApi\.sendMessage/)
  assert.match(chatPage, /wsManager\.on\('chat'/)
  assert.match(chatPage, /wsManager\.on\('groupChat'/)
  assert.match(chatPage, /wsManager\.off\('chat'/)
  assert.match(chatPage, /wsManager\.off\('groupChat'/)
  assert.doesNotMatch(chatPage, /wsManager\.send\(['"](?:chat|groupChat)/)
  assert.match(websocket, /不支持|send\(type/)
})

test('administrator passwords are random, one-time, and forced to change', async () => {
  const [userPage, passwordModal, userStore] = await Promise.all([
    source('src/views/system/user/index.vue'),
    source('src/components/PasswordModal.vue'),
    source('src/stores/user.ts')
  ])

  assert.doesNotMatch(userPage, /默认123456|重置后密码为123456/)
  assert.match(userPage, /temporaryPassword/)
  assert.match(passwordModal, /props\.required/)
  assert.match(userStore, /mustChangePassword/)
})

test('account enforcement selections submit numeric ids and respect operator permissions', async () => {
  const page = await source('src/views/community/account-enforcements/index.vue')

  assert.match(page, /const numericId = \(value: string\) => value\.trim\(\)\.match\(\/\^\\d\+\//)
  assert.match(page, /reviewAccountEnforcement\(caseId,/)
  assert.match(page, /reviewAccountEnforcementAppeal\(appealId,/)
  assert.match(page, /if \(canApprove\.value\) void loadAppeals\(\)/)
  assert.match(page, /tab\.value = 'records'/)
  assert.match(page, /name="review" tab="申请审批"/)
  assert.match(page, /name="appeals" tab="申诉复核"/)
  assert.match(page, /<n-descriptions-item label="申请 ID">/)
  assert.match(page, /<n-descriptions-item label="申诉 ID">/)
  assert.doesNotMatch(page, /<n-description\s/)
  assert.match(page, /selectedReviewApplication\.value\.status === 'SUBMITTED'/)
  assert.match(page, /selectedReviewApplication\.value\.status === 'UNDER_REVIEW' && isSuperAdmin\.value/)
  assert.match(page, /selectedReviewApplication\.value\?\.status === 'APPROVED'/)
  assert.match(page, /\['LONG_FREEZE', 'DATA_CLEANUP', 'ACCOUNT_DELETE'\]\.includes\(form\.measureType\)/)
  assert.match(page, /\['LONG_FREEZE', '提交长期冻结申请'\]/)
  assert.match(page, /reviewAction\('APPROVE'\)/)
  assert.match(page, /reviewAction\('REJECT'\)/)
  assert.match(page, /accountEnforcementReviews\(item\.id\)/)
  assert.match(page, /title="目标账户基本信息"/)
  assert.match(page, /isOwnReviewApplication/)
  assert.match(page, /hasReviewedSelection/)
})
