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
