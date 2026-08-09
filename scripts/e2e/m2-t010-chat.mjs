import { execFileSync } from 'node:child_process'
import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'

const requireFromAdmin = createRequire(
  new URL('../../pxczxn-admin/package.json', import.meta.url)
)
const JSEncrypt = requireFromAdmin('jsencrypt')

const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8849'
const database = process.env.PXCZXN_DB_NAME || 'pxczxn_community'
const databaseUser = process.env.PXCZXN_DB_USERNAME || 'root'
const databasePassword = process.env.PXCZXN_DB_PASSWORD || 'root'
const mysqlPath = process.env.PXCZXN_MYSQL_PATH
  || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'

const stamp = new Date()
  .toISOString()
  .replace(/\D/g, '')
  .slice(4, 17)
const usernameA = `m2ca_${stamp}`
const usernameB = `m2cb_${stamp}`
const usernameC = `m2cc_${stamp}`
const usernames = [usernameA, usernameB, usernameC]
const groupName = `M2 chat ${stamp}`

let cryptoConfig
let userAId
let userBId
let userCId
let groupId
let failure
const probes = []

function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(
      `${label}: expected [${expected}] but was [${actual}]`
    )
  }
}

function assertTrue(condition, label) {
  if (!condition) {
    throw new Error(label)
  }
}

function sqlString(value) {
  return `'${String(value).replaceAll("'", "''")}'`
}

function mysql(sql) {
  return execFileSync(
    mysqlPath,
    [
      `--user=${databaseUser}`,
      `--database=${database}`,
      '--default-character-set=utf8mb4',
      '--batch',
      '--skip-column-names',
      `--execute=${sql}`
    ],
    {
      encoding: 'utf8',
      env: {
        ...process.env,
        MYSQL_PWD: databasePassword
      },
      stdio: ['ignore', 'pipe', 'pipe']
    }
  ).trim()
}

async function request(path, {
  method = 'GET',
  body,
  token,
  expectedCodes = [200]
} = {}) {
  const headers = {}
  if (token) {
    headers.Authorization = token
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8'
  }
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const result = await response.json()
  if (!expectedCodes.includes(Number(result.code))) {
    throw new Error(
      `${method} ${path} returned ${JSON.stringify(result)}`
    )
  }
  return result
}

async function decryptAdminData(value) {
  if (
    typeof value !== 'string'
    || !value.includes('.')
    || !cryptoConfig?.aesKey
  ) {
    return value
  }
  const [ivBase64, payloadBase64] = value.split('.')
  const key = await webcrypto.subtle.importKey(
    'raw',
    Buffer.from(cryptoConfig.aesKey, 'base64'),
    { name: 'AES-GCM' },
    false,
    ['decrypt']
  )
  const plaintext = await webcrypto.subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: Buffer.from(ivBase64, 'base64')
    },
    key,
    Buffer.from(payloadBase64, 'base64')
  )
  return JSON.parse(Buffer.from(plaintext).toString('utf8'))
}

async function adminRequest(path, options = {}) {
  const result = await request(path, options)
  return {
    ...result,
    data: await decryptAdminData(result.data)
  }
}

function encryptAdminPassword(plainPassword) {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(cryptoConfig.publicKey)
  const encrypted = encryptor.encrypt(plainPassword)
  if (!encrypted) {
    throw new Error('admin password RSA encryption failed')
  }
  return encrypted
}

async function loginAdmin(username) {
  const result = await request('/api/auth/login', {
    method: 'POST',
    body: {
      username,
      password: encryptAdminPassword(adminPassword),
      rememberMe: false
    },
    expectedCodes: [200, 500]
  })
  if (Number(result.code) !== 200) {
    throw new Error(`admin login failed for ${username}: ${result.message}`)
  }
  const data = await decryptAdminData(result.data)
  return data.token
}

function createWebSocketProbe(url) {
  const socket = new WebSocket(url)
  const queued = []
  const waiters = []

  socket.addEventListener('message', (event) => {
    let payload
    try {
      payload = JSON.parse(String(event.data))
    } catch {
      return
    }
    const waiterIndex = waiters.findIndex((item) =>
      item.predicate(payload)
    )
    if (waiterIndex >= 0) {
      const [waiter] = waiters.splice(waiterIndex, 1)
      clearTimeout(waiter.timer)
      waiter.resolve(payload)
      return
    }
    queued.push(payload)
  })

  return {
    socket,
    waitFor(predicate, label, timeoutMs = 5000) {
      const queuedIndex = queued.findIndex(predicate)
      if (queuedIndex >= 0) {
        return Promise.resolve(queued.splice(queuedIndex, 1)[0])
      }
      return new Promise((resolve, reject) => {
        const waiter = { predicate, resolve, reject, timer: null }
        waiter.timer = setTimeout(() => {
          const index = waiters.indexOf(waiter)
          if (index >= 0) {
            waiters.splice(index, 1)
          }
          reject(new Error(`WebSocket timeout: ${label}`))
        }, timeoutMs)
        waiters.push(waiter)
      })
    },
    close() {
      for (const waiter of waiters.splice(0)) {
        clearTimeout(waiter.timer)
        waiter.reject(new Error('WebSocket probe closed'))
      }
      socket.close()
    }
  }
}

async function connectWebSocket(token) {
  const ticketResult = await adminRequest(
    '/api/auth/websocket-ticket',
    { method: 'POST', token }
  )
  assertTrue(
    typeof ticketResult.data.ticket === 'string'
      && ticketResult.data.ticket.length >= 40,
    'one-time WebSocket ticket was not issued'
  )
  assertTrue(
    /^[A-Za-z0-9_-]{43}$/.test(ticketResult.data.ticket),
    'one-time WebSocket ticket was not returned in its opaque wire format'
  )
  assertTrue(
    !Object.hasOwn(ticketResult.data, 'token'),
    'WebSocket ticket response leaked a long-lived token'
  )

  const socketUrl = new URL('/ws/message', baseUrl)
  socketUrl.protocol = socketUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  socketUrl.searchParams.set('ticket', ticketResult.data.ticket)
  const probe = createWebSocketProbe(socketUrl)
  probes.push(probe)
  await probe.waitFor(
    (message) => message.type === 'connected',
    'connection acknowledgement'
  )
  return probe
}

function cleanup() {
  mysql(`
    SET @user_a := (
      SELECT id FROM sys_user
      WHERE username = ${sqlString(usernameA)}
      LIMIT 1
    );
    SET @user_b := (
      SELECT id FROM sys_user
      WHERE username = ${sqlString(usernameB)}
      LIMIT 1
    );
    SET @user_c := (
      SELECT id FROM sys_user
      WHERE username = ${sqlString(usernameC)}
      LIMIT 1
    );

    DELETE FROM sys_chat_group_message
    WHERE group_id IN (
      SELECT id FROM sys_chat_group
      WHERE name = ${sqlString(groupName)}
         OR owner_id IN (@user_a, @user_b, @user_c)
    );
    DELETE FROM sys_chat_group_member
    WHERE group_id IN (
      SELECT id FROM sys_chat_group
      WHERE name = ${sqlString(groupName)}
         OR owner_id IN (@user_a, @user_b, @user_c)
    );
    DELETE FROM sys_chat_group
    WHERE name = ${sqlString(groupName)}
       OR owner_id IN (@user_a, @user_b, @user_c);
    DELETE FROM sys_chat_message
    WHERE sender_id IN (@user_a, @user_b, @user_c)
       OR receiver_id IN (@user_a, @user_b, @user_c);
    DELETE FROM sys_user_blacklist
    WHERE user_id IN (@user_a, @user_b, @user_c)
       OR blocked_user_id IN (@user_a, @user_b, @user_c);
    DELETE FROM sys_user_role
    WHERE user_id IN (@user_a, @user_b, @user_c);
    DELETE FROM sys_login_log
    WHERE username IN (
      ${sqlString(usernameA)},
      ${sqlString(usernameB)},
      ${sqlString(usernameC)}
    );
    DELETE FROM sys_user
    WHERE username IN (
      ${sqlString(usernameA)},
      ${sqlString(usernameB)},
      ${sqlString(usernameC)}
    );
  `)
}

try {
  const health = await request('/api/v1/health')
  assertEqual(health.data.status, 'UP', 'backend health')
  cryptoConfig = (
    await request('/api/crypto/config')
  ).data

  cleanup()
  mysql(`
    INSERT INTO sys_user
      (username, password, must_change_password, nickname,
       status, user_type, deleted, remark)
    SELECT
      ${sqlString(usernameA)}, password, 0,
      ${sqlString(`聊天用户A ${stamp}`)}, 1, 'admin', 0, 'M2-T010 E2E'
    FROM sys_user
    WHERE username = 'admin'
    LIMIT 1;
    INSERT INTO sys_user
      (username, password, must_change_password, nickname,
       status, user_type, deleted, remark)
    SELECT
      ${sqlString(usernameB)}, password, 0,
      ${sqlString(`聊天用户B ${stamp}`)}, 1, 'admin', 0, 'M2-T010 E2E'
    FROM sys_user
    WHERE username = 'admin'
    LIMIT 1;
    INSERT INTO sys_user
      (username, password, must_change_password, nickname,
       status, user_type, deleted, remark)
    SELECT
      ${sqlString(usernameC)}, password, 0,
      ${sqlString(`聊天用户C ${stamp}`)}, 1, 'admin', 0, 'M2-T010 E2E'
    FROM sys_user
    WHERE username = 'admin'
    LIMIT 1;
    INSERT INTO sys_user_role (user_id, role_id)
    SELECT id, 1 FROM sys_user
    WHERE username IN (
      ${sqlString(usernameA)},
      ${sqlString(usernameB)},
      ${sqlString(usernameC)}
    );
  `)

  const ids = mysql(`
    SELECT id FROM sys_user
    WHERE username IN (
      ${sqlString(usernameA)},
      ${sqlString(usernameB)},
      ${sqlString(usernameC)}
    )
    ORDER BY FIELD(
      username,
      ${sqlString(usernameA)},
      ${sqlString(usernameB)},
      ${sqlString(usernameC)}
    );
  `).split(/\s+/).map(Number)
  ;[userAId, userBId, userCId] = ids
  assertEqual(ids.length, 3, 'temporary chat principal count')

  const [tokenA, tokenB, tokenC] = await Promise.all([
    loginAdmin(usernameA),
    loginAdmin(usernameB),
    loginAdmin(usernameC)
  ])
  const [probeB, probeC] = await Promise.all([
    connectWebSocket(tokenB),
    connectWebSocket(tokenC)
  ])

  const directSocketError = probeB.waitFor(
    (message) => message.type === 'error',
    'direct WebSocket business send rejection'
  )
  probeB.socket.send(JSON.stringify({
    type: 'chat',
    receiverId: userAId,
    content: 'must not be persisted'
  }))
  assertTrue(
    (await directSocketError).content.includes('不支持'),
    'WebSocket accepted a direct business message'
  )
  assertEqual(
    Number(mysql(`
      SELECT COUNT(*) FROM sys_chat_message
      WHERE content = 'must not be persisted';
    `)),
    0,
    'direct WebSocket message persistence'
  )

  const privateContent = `private-${stamp}`
  const privateEventPromise = probeB.waitFor(
    (message) =>
      message.type === 'chat'
      && message.content === privateContent,
    'persisted private message event'
  )
  const privateMessage = (
    await adminRequest('/api/sys/chat/send', {
      method: 'POST',
      token: tokenA,
      body: {
        receiverId: userBId,
        content: privateContent,
        msgType: 1
      }
    })
  ).data
  const privateEvent = await privateEventPromise
  assertEqual(
    String(privateEvent.id),
    String(privateMessage.id),
    'private WebSocket persisted message id'
  )

  const privateUnread = (
    await adminRequest('/api/sys/chat/unread-count', { token: tokenB })
  ).data
  assertEqual(privateUnread, 1, 'private unread count')
  const contacts = (
    await adminRequest('/api/sys/chat/contacts', { token: tokenB })
  ).data
  const contactA = contacts.find((item) =>
    String(item.userId) === String(userAId)
  )
  assertTrue(contactA, 'recent private contact')
  assertEqual(contactA.unreadCount, 1, 'recent contact unread count')
  assertEqual(contactA.online, false, 'offline contact state')

  await adminRequest(`/api/sys/chat/read/${userAId}`, {
    method: 'POST',
    token: tokenB
  })
  assertEqual(
    (
      await adminRequest(
        '/api/sys/chat/unread-count',
        { token: tokenB }
      )
    ).data,
    0,
    'private mark-as-read'
  )

  await adminRequest(`/api/sys/chat/block/${userAId}`, {
    method: 'POST',
    token: tokenB
  })
  const blockedSend = await adminRequest('/api/sys/chat/send', {
    method: 'POST',
    token: tokenA,
    body: {
      receiverId: userBId,
      content: `blocked-${stamp}`,
      msgType: 1
    },
    expectedCodes: [403]
  })
  assertEqual(blockedSend.code, 403, 'blacklist send isolation')
  await adminRequest(`/api/sys/chat/block/${userAId}`, {
    method: 'DELETE',
    token: tokenB
  })

  await adminRequest(`/api/sys/chat/clear/${userAId}`, {
    method: 'DELETE',
    token: tokenB
  })
  const historyB = (
    await adminRequest(
      `/api/sys/chat/history/${userAId}?page=1&pageSize=20`,
      { token: tokenB }
    )
  ).data
  const historyA = (
    await adminRequest(
      `/api/sys/chat/history/${userBId}?page=1&pageSize=20`,
      { token: tokenA }
    )
  ).data
  assertEqual(Number(historyB.total), 0, 'receiver single-party clear')
  assertEqual(Number(historyA.total), 1, 'sender history retained')

  const createdGroup = (
    await adminRequest('/api/chat/group/create', {
      method: 'POST',
      token: tokenA,
      body: {
        name: groupName,
        memberIds: [userBId]
      }
    })
  ).data
  groupId = createdGroup.id
  assertEqual(createdGroup.memberCount, 2, 'initial group member count')

  await adminRequest(`/api/chat/group/${groupId}/read`, {
    method: 'POST',
    token: tokenB
  })
  const outsiderDetail = await adminRequest(
    `/api/chat/group/${groupId}`,
    { token: tokenC, expectedCodes: [403] }
  )
  assertEqual(outsiderDetail.code, 403, 'group outsider detail isolation')
  const memberEscalation = await adminRequest(
    `/api/chat/group/${groupId}/members`,
    {
      method: 'POST',
      token: tokenB,
      body: { userIds: [userCId] },
      expectedCodes: [403]
    }
  )
  assertEqual(
    memberEscalation.code,
    403,
    'ordinary member add-member isolation'
  )

  await adminRequest(`/api/chat/group/${groupId}/members`, {
    method: 'POST',
    token: tokenA,
    body: { userIds: [userCId] }
  })
  const groupDetailC = (
    await adminRequest(`/api/chat/group/${groupId}`, {
      token: tokenC
    })
  ).data
  assertEqual(groupDetailC.members.length, 3, 'owner added group member')
  await Promise.all([
    adminRequest(`/api/chat/group/${groupId}/read`, {
      method: 'POST',
      token: tokenB
    }),
    adminRequest(`/api/chat/group/${groupId}/read`, {
      method: 'POST',
      token: tokenC
    })
  ])

  const groupContent = `group-${stamp}`
  const groupEventBPromise = probeB.waitFor(
    (message) =>
      message.type === 'groupChat'
      && message.content === groupContent,
    'group event for member B'
  )
  const groupEventCPromise = probeC.waitFor(
    (message) =>
      message.type === 'groupChat'
      && message.content === groupContent,
    'group event for member C'
  )
  const groupMessage = (
    await adminRequest(`/api/chat/group/${groupId}/message`, {
      method: 'POST',
      token: tokenA,
      body: { content: groupContent, msgType: 1 }
    })
  ).data
  const [groupEventB, groupEventC] = await Promise.all([
    groupEventBPromise,
    groupEventCPromise
  ])
  assertEqual(
    String(groupEventB.id),
    String(groupMessage.id),
    'group event B persisted message id'
  )
  assertEqual(
    String(groupEventC.id),
    String(groupMessage.id),
    'group event C persisted message id'
  )

  const [groupUnreadB, groupUnreadC] = await Promise.all([
    adminRequest(`/api/chat/group/${groupId}/unread-count`, {
      token: tokenB
    }),
    adminRequest(`/api/chat/group/${groupId}/unread-count`, {
      token: tokenC
    })
  ])
  assertEqual(groupUnreadB.data, 1, 'group unread count B')
  assertEqual(groupUnreadC.data, 1, 'group unread count C')
  await adminRequest(`/api/chat/group/${groupId}/read`, {
    method: 'POST',
    token: tokenB
  })
  assertEqual(
    (
      await adminRequest(
        `/api/chat/group/${groupId}/unread-count`,
        { token: tokenB }
      )
    ).data,
    0,
    'group mark-as-read'
  )

  const invalidPage = await adminRequest(
    `/api/chat/group/${groupId}/messages?page=1&pageSize=101`,
    { token: tokenC, expectedCodes: [400] }
  )
  assertEqual(invalidPage.code, 400, 'group page size boundary')
  const groupHistory = (
    await adminRequest(
      `/api/chat/group/${groupId}/messages?page=1&pageSize=20`,
      { token: tokenC }
    )
  ).data
  assertTrue(
    groupHistory.list.some((item) =>
      String(item.id) === String(groupMessage.id)
    ),
    'group history persisted message'
  )

  await adminRequest(
    `/api/chat/group/${groupId}/mute/${userBId}?muted=true`,
    { method: 'POST', token: tokenA }
  )
  const mutedSend = await adminRequest(
    `/api/chat/group/${groupId}/message`,
    {
      method: 'POST',
      token: tokenB,
      body: { content: `muted-${stamp}`, msgType: 1 },
      expectedCodes: [403]
    }
  )
  assertEqual(mutedSend.code, 403, 'group mute enforcement')
  await adminRequest(
    `/api/chat/group/${groupId}/mute/${userBId}?muted=false`,
    { method: 'POST', token: tokenA }
  )

  await adminRequest(
    `/api/chat/group/${groupId}/admin/${userCId}?isAdmin=true`,
    { method: 'POST', token: tokenA }
  )
  const updatedGroupName = `${groupName} updated`
  await adminRequest('/api/chat/group/update', {
    method: 'PUT',
    token: tokenC,
    body: {
      id: groupId,
      name: updatedGroupName,
      announcement: `announcement-${stamp}`
    }
  })
  assertEqual(
    (
      await adminRequest(`/api/chat/group/${groupId}`, {
        token: tokenB
      })
    ).data.name,
    updatedGroupName,
    'group administrator update'
  )

  await adminRequest(
    `/api/chat/group/${groupId}/transfer/${userBId}`,
    { method: 'POST', token: tokenA }
  )
  assertEqual(
    String((
      await adminRequest(`/api/chat/group/${groupId}`, {
        token: tokenB
      })
    ).data.ownerId),
    String(userBId),
    'group ownership transfer'
  )
  await adminRequest(`/api/chat/group/${groupId}/quit`, {
    method: 'POST',
    token: tokenA
  })
  const postQuitDetail = await adminRequest(
    `/api/chat/group/${groupId}`,
    { token: tokenA, expectedCodes: [403] }
  )
  assertEqual(postQuitDetail.code, 403, 'former member quit isolation')

  await adminRequest(`/api/chat/group/${groupId}`, {
    method: 'DELETE',
    token: tokenB
  })
  const dissolvedDetail = await adminRequest(
    `/api/chat/group/${groupId}`,
    { token: tokenC, expectedCodes: [404] }
  )
  assertEqual(dissolvedDetail.code, 404, 'dissolved group isolation')

  const databaseEvidence = mysql(`
    SELECT CONCAT(
      'private=', (
        SELECT COUNT(*) FROM sys_chat_message
        WHERE sender_id IN (${userAId}, ${userBId}, ${userCId})
           OR receiver_id IN (${userAId}, ${userBId}, ${userCId})
      ),
      ',groups=', (
        SELECT COUNT(*) FROM sys_chat_group
        WHERE id = ${groupId} AND status = 0
      ),
      ',members=', (
        SELECT COUNT(*) FROM sys_chat_group_member
        WHERE group_id = ${groupId}
      ),
      ',groupMessages=', (
        SELECT COUNT(*) FROM sys_chat_group_message
        WHERE group_id = ${groupId}
      )
    );
  `)
  assertTrue(
    databaseEvidence.includes('groups=1,members=0'),
    'dissolved group database state'
  )

  console.log(JSON.stringify({
    stamp,
    backend: 'UP',
    privateChat: {
      persistedPush: true,
      unreadAndRead: true,
      blacklist: true,
      singlePartyClear: true
    },
    groupChat: {
      memberIsolation: true,
      persistentUnreadCursor: true,
      mute: true,
      administrator: true,
      transferQuitDissolve: true
    },
    websocket: {
      oneTimeTicket: true,
      directBusinessMessagesRejected: true,
      persistedEventsDelivered: true
    },
    databaseEvidence
  }, null, 2))
} catch (error) {
  failure = error
  console.error(`CHAT_E2E_FAILED: ${error.message}`)
} finally {
  for (const probe of probes) {
    try {
      probe.close()
    } catch {
      // Best-effort connection cleanup.
    }
  }
  try {
    cleanup()
    const remaining = mysql(`
      SELECT COUNT(*) FROM sys_user
      WHERE username IN (
        ${sqlString(usernameA)},
        ${sqlString(usernameB)},
        ${sqlString(usernameC)}
      );
    `)
    console.log(`CLEANUP: users=${remaining}`)
  } catch (error) {
    console.error(`CHAT_CLEANUP_FAILED: ${error.message}`)
    failure ||= error
  }
}

if (failure) {
  process.exitCode = 1
}
