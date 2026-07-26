import { execFileSync } from 'node:child_process'
import { webcrypto } from 'node:crypto'
import { createRequire } from 'node:module'

const requireFromAdmin = createRequire(
  new URL('../../pxczxn-admin/package.json', import.meta.url)
)
const JSEncrypt = requireFromAdmin('jsencrypt')

const baseUrl = process.env.PXCZXN_BASE_URL || 'http://127.0.0.1:8849'
const database = process.env.PXCZXN_DB_NAME || 'mars-system'
const databaseUser = process.env.PXCZXN_DB_USERNAME || 'root'
const databasePassword = process.env.PXCZXN_DB_PASSWORD || 'root'
const mysqlPath = process.env.PXCZXN_MYSQL_PATH
  || 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe'
const adminPassword = process.env.PXCZXN_ADMIN_PASSWORD || 'admin123'
const keepData = process.argv.includes('--keep-data')
const cleanupArgument = process.argv.find((item) =>
  item.startsWith('--cleanup-stamp=')
)
const cleanupStamp = cleanupArgument?.slice('--cleanup-stamp='.length)

const stamp = cleanupStamp || new Date()
  .toISOString()
  .replace(/\D/g, '')
  .slice(4, 17)
const usernameA = `m2ga_${stamp}`
const usernameB = `m2gb_${stamp}`
const emailA = `${usernameA}@example.test`
const emailB = `${usernameB}@example.test`
const password = 'M2Governance!2026'
const reviewKeyword = `governreview${stamp}`
const ruleId = 8_200_000_000_000_000_000n
  + BigInt(Date.now() % 1_000_000_000_000)
const tempAdmin = `m2gov_${stamp}`
const tempRole = `m2gov_role_${stamp}`
const forcedReplacementPassword = 'M2ForcedChange!2026'

let userAId
let userBId
let blogAId
let blogBId
let adminToken = process.env.PXCZXN_ADMIN_TOKEN || ''
let readOnlyToken = ''
let cryptoConfig
let failure

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
  admin = false,
  expectedCodes = [200]
} = {}) {
  const headers = {}
  if (token) {
    headers[admin ? 'Authorization' : 'pxczxn-community-token'] = token
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
  const result = await request(path, {
    ...options,
    token: options.token || adminToken,
    admin: true
  })
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

async function loginAdmin(username, plainPassword) {
  const result = await request('/api/auth/login', {
    method: 'POST',
    body: {
      username,
      password: encryptAdminPassword(plainPassword),
      rememberMe: false
    },
    admin: true,
    expectedCodes: [200, 500]
  })
  if (Number(result.code) !== 200) {
    throw new Error(
      `admin login failed: ${result.message}; `
      + 'pass PXCZXN_ADMIN_TOKEN when image captcha is enabled'
    )
  }
  const data = await decryptAdminData(result.data)
  return data.token
}

async function register(username, email, displayName) {
  return request('/api/v1/auth/register', {
    method: 'POST',
    body: { username, email, password, displayName }
  })
}

async function loginCommunity(email) {
  const result = await request('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password }
  })
  return result.data.tokenValue
}

async function createMoment(token, text) {
  return request('/api/v1/moments', {
    method: 'POST',
    token,
    body: {
      blogId: null,
      momentType: 'TEXT',
      textContent: text,
      linkUrl: null,
      articleId: null,
      repostMomentId: null,
      visibility: 'PUBLIC'
    }
  })
}

async function createComment(token, momentId, content) {
  return request(
    `/api/v1/interactions/MOMENT/${momentId}/comments`,
    {
      method: 'POST',
      token,
      body: { content }
    }
  )
}

function cleanupAdminPrincipal() {
  mysql(`
    SET @test_user := (
      SELECT id FROM sys_user
      WHERE username = ${sqlString(tempAdmin)}
      LIMIT 1
    );
    SET @test_role := (
      SELECT id FROM sys_role
      WHERE code = ${sqlString(tempRole)}
      LIMIT 1
    );
    DELETE FROM sys_user_role WHERE user_id = @test_user;
    DELETE FROM sys_role_menu WHERE role_id = @test_role;
    DELETE FROM sys_user WHERE id = @test_user;
    DELETE FROM sys_role WHERE id = @test_role;
  `)
}

function cleanupCommunityData() {
  mysql(`
    SET @user_a := (
      SELECT id FROM community_user
      WHERE username = ${sqlString(usernameA)}
      LIMIT 1
    );
    SET @user_b := (
      SELECT id FROM community_user
      WHERE username = ${sqlString(usernameB)}
      LIMIT 1
    );
    SET @blog_a := (
      SELECT id FROM blog
      WHERE owner_user_id = @user_a
        AND blog_type = 'PERSONAL'
      LIMIT 1
    );
    SET @blog_b := (
      SELECT id FROM blog
      WHERE owner_user_id = @user_b
        AND blog_type = 'PERSONAL'
      LIMIT 1
    );

    DELETE FROM community_comment_moderation_event
    WHERE comment_id IN (
      SELECT id FROM community_comment
      WHERE target_type = 'MOMENT'
        AND target_id IN (
          SELECT id FROM community_moment
          WHERE actor_user_id IN (@user_a, @user_b)
        )
    );
    DELETE FROM community_moment_moderation_event
    WHERE moment_id IN (
      SELECT id FROM community_moment
      WHERE actor_user_id IN (@user_a, @user_b)
    );
    DELETE FROM community_content_like
    WHERE user_id IN (@user_a, @user_b)
       OR (
         target_type = 'MOMENT'
         AND target_id IN (
           SELECT id FROM community_moment
           WHERE actor_user_id IN (@user_a, @user_b)
         )
       )
       OR (
         target_type = 'COMMENT'
         AND target_id IN (
           SELECT id FROM community_comment
           WHERE target_type = 'MOMENT'
             AND target_id IN (
               SELECT id FROM community_moment
               WHERE actor_user_id IN (@user_a, @user_b)
             )
         )
       );
    DELETE FROM favorite_folder_item
    WHERE favorite_item_id IN (
      SELECT id FROM favorite_item
      WHERE owner_user_id IN (@user_a, @user_b)
         OR (
           target_type = 'MOMENT'
           AND target_id IN (
             SELECT id FROM community_moment
             WHERE actor_user_id IN (@user_a, @user_b)
           )
         )
    );
    DELETE FROM favorite_item
    WHERE owner_user_id IN (@user_a, @user_b)
       OR (
         target_type = 'MOMENT'
         AND target_id IN (
           SELECT id FROM community_moment
           WHERE actor_user_id IN (@user_a, @user_b)
         )
       );
    DELETE FROM favorite_folder
    WHERE owner_user_id IN (@user_a, @user_b);
    DELETE FROM community_comment
    WHERE target_type = 'MOMENT'
      AND target_id IN (
        SELECT id FROM community_moment
        WHERE actor_user_id IN (@user_a, @user_b)
      )
      AND root_comment_id IS NOT NULL;
    DELETE FROM community_comment
    WHERE target_type = 'MOMENT'
      AND target_id IN (
        SELECT id FROM community_moment
        WHERE actor_user_id IN (@user_a, @user_b)
      );
    DELETE FROM community_follow
    WHERE follower_user_id IN (@user_a, @user_b)
       OR (
         target_type = 'BLOG'
         AND target_id IN (@blog_a, @blog_b)
       );
    DROP TEMPORARY TABLE IF EXISTS tmp_m2_t009_notification;
    CREATE TEMPORARY TABLE tmp_m2_t009_notification AS
    SELECT DISTINCT n.id
    FROM community_notification n
    LEFT JOIN community_notification_recipient r
      ON r.notification_id = n.id
    WHERE n.sender_user_id IN (@user_a, @user_b)
       OR r.recipient_user_id IN (@user_a, @user_b);
    DELETE FROM community_notification_recipient
    WHERE recipient_user_id IN (@user_a, @user_b)
       OR notification_id IN (
         SELECT id FROM tmp_m2_t009_notification
       );
    DELETE FROM community_notification
    WHERE id IN (
      SELECT id FROM tmp_m2_t009_notification
    );
    DROP TEMPORARY TABLE tmp_m2_t009_notification;
    DELETE FROM community_moment
    WHERE actor_user_id IN (@user_a, @user_b);
    DELETE FROM content_keyword_rule
    WHERE description = ${sqlString(`M2-T009 E2E ${stamp}`)};
    DELETE FROM blog_category
    WHERE blog_id IN (@blog_a, @blog_b);
    UPDATE community_user
    SET personal_blog_id = NULL
    WHERE id IN (@user_a, @user_b);
    DELETE FROM blog WHERE id IN (@blog_a, @blog_b);
    DELETE FROM community_user WHERE id IN (@user_a, @user_b);
  `)
}

if (cleanupStamp) {
  cleanupAdminPrincipal()
  cleanupCommunityData()
  console.log(JSON.stringify({
    cleanupStamp,
    users: mysql(`
      SELECT COUNT(*) FROM community_user
      WHERE username IN (
        ${sqlString(usernameA)},
        ${sqlString(usernameB)}
      );
    `)
  }))
  process.exit(0)
}

try {
  const health = await request('/api/v1/health')
  assertEqual(health.data.status, 'UP', 'backend health')

  cryptoConfig = (
    await request('/api/crypto/config', { admin: true })
  ).data
  if (!adminToken) {
    adminToken = await loginAdmin('admin', adminPassword)
  }
  const websocketTicket = await adminRequest(
    '/api/auth/websocket-ticket',
    { method: 'POST' }
  )
  assertTrue(
    typeof websocketTicket.data.ticket === 'string'
      && websocketTicket.data.ticket.length >= 40,
    'one-time WebSocket ticket was not issued'
  )
  assertTrue(
    !Object.hasOwn(websocketTicket.data, 'token'),
    'WebSocket ticket response leaked the administrator session token'
  )

  const registeredA = await register(
    usernameA,
    emailA,
    '治理动态作者'
  )
  const registeredB = await register(
    usernameB,
    emailB,
    '治理互动用户'
  )
  userAId = registeredA.data.userId
  userBId = registeredB.data.userId
  blogAId = registeredA.data.blogId
  blogBId = registeredB.data.blogId

  const tokenA = await loginCommunity(emailA)
  const tokenB = await loginCommunity(emailB)

  mysql(`
    INSERT INTO content_keyword_rule
      (id, keyword, normalized_keyword, severity, status,
       description, sort_order)
    VALUES
      (${ruleId}, ${sqlString(reviewKeyword)},
       ${sqlString(reviewKeyword)}, 'REVIEW', 'ACTIVE',
       ${sqlString(`M2-T009 E2E ${stamp}`)}, 1);
  `)

  const publicMoment = await createMoment(
    tokenA,
    `M2-T009 公开动态 ${stamp}`
  )
  const publicMomentId = publicMoment.data.moment.momentId
  const pendingMoment = await createMoment(
    tokenA,
    `包含 ${reviewKeyword} 的待审动态`
  )
  const pendingMomentId = pendingMoment.data.moment.momentId
  assertEqual(
    pendingMoment.data.moment.status,
    'PENDING_REVIEW',
    'pending moment status'
  )

  const pendingComment = await createComment(
    tokenB,
    publicMomentId,
    `包含 ${reviewKeyword} 的待审评论`
  )
  const rejectedComment = await createComment(
    tokenB,
    publicMomentId,
    `第二条 ${reviewKeyword} 待审评论`
  )
  assertEqual(
    pendingComment.data.status,
    'PENDING_REVIEW',
    'pending comment status'
  )

  const batchCommentA = await createComment(
    tokenB,
    publicMomentId,
    `批量评论 A ${stamp}`
  )
  const batchCommentB = await createComment(
    tokenB,
    publicMomentId,
    `批量评论 B ${stamp}`
  )
  const batchMomentA = await createMoment(
    tokenA,
    `批量动态 A ${stamp}`
  )
  const batchMomentB = await createMoment(
    tokenA,
    `批量动态 B ${stamp}`
  )

  await request(
    `/api/v1/interactions/MOMENT/${publicMomentId}/like`,
    { method: 'POST', token: tokenB }
  )
  await request(
    `/api/v1/interactions/MOMENT/${publicMomentId}/favorite`,
    {
      method: 'POST',
      token: tokenB,
      body: { folderIds: [] }
    }
  )
  await request(`/api/v1/blogs/${blogAId}/follow`, {
    method: 'POST',
    token: tokenB,
    body: {
      notificationLevel: 'ALL',
      specialFollow: true
    }
  })

  const commentPage = (
    await adminRequest(
      `/admin-api/community/comments`
      + `?keyword=${reviewKeyword}&pageNum=1&pageSize=50`
    )
  ).data
  const momentPage = (
    await adminRequest(
      `/admin-api/community/moments`
      + `?keyword=${reviewKeyword}&pageNum=1&pageSize=50`
    )
  ).data
  assertEqual(commentPage.total, 2, 'pending comment admin total')
  assertEqual(momentPage.total, 1, 'pending moment admin total')

  const pendingCommentRow = commentPage.list.find(
    (item) => item.id === String(pendingComment.data.commentId)
  )
  const rejectedCommentRow = commentPage.list.find(
    (item) => item.id === String(rejectedComment.data.commentId)
  )
  const pendingMomentRow = momentPage.list.find(
    (item) => item.id === String(pendingMomentId)
  )
  assertTrue(pendingCommentRow, 'pending comment missing from admin list')
  assertTrue(rejectedCommentRow, 'reject comment missing from admin list')
  assertTrue(pendingMomentRow, 'pending moment missing from admin list')

  const approvedComment = (
    await adminRequest(
      `/admin-api/community/comments/${pendingCommentRow.id}/approve`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: pendingCommentRow.lockVersion,
          reason: `M2-T009 审核通过 ${stamp}`
        }
      }
    )
  ).data
  const approvedMoment = (
    await adminRequest(
      `/admin-api/community/moments/${pendingMomentRow.id}/approve`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: pendingMomentRow.lockVersion,
          reason: `M2-T009 审核通过 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(approvedComment.status, 'PUBLISHED', 'comment approve')
  assertEqual(approvedMoment.status, 'PUBLISHED', 'moment approve')

  const replayComment = (
    await adminRequest(
      `/admin-api/community/comments/${pendingCommentRow.id}/approve`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: pendingCommentRow.lockVersion,
          reason: `M2-T009 审核通过 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(replayComment.replay, true, 'comment idempotent replay')

  const downComment = (
    await adminRequest(
      `/admin-api/community/comments/${pendingCommentRow.id}/take-down`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: approvedComment.lockVersion,
          reason: `M2-T009 下架验证 ${stamp}`
        }
      }
    )
  ).data
  const restoredComment = (
    await adminRequest(
      `/admin-api/community/comments/${pendingCommentRow.id}/restore`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: downComment.lockVersion,
          reason: `M2-T009 恢复验证 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(downComment.status, 'TAKEN_DOWN', 'comment take down')
  assertEqual(restoredComment.status, 'PUBLISHED', 'comment restore')

  const downMoment = (
    await adminRequest(
      `/admin-api/community/moments/${pendingMomentRow.id}/take-down`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: approvedMoment.lockVersion,
          reason: `M2-T009 下架验证 ${stamp}`
        }
      }
    )
  ).data
  const restoredMoment = (
    await adminRequest(
      `/admin-api/community/moments/${pendingMomentRow.id}/restore`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: downMoment.lockVersion,
          reason: `M2-T009 恢复验证 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(downMoment.status, 'TAKEN_DOWN', 'moment take down')
  assertEqual(restoredMoment.status, 'PUBLISHED', 'moment restore')

  const rejected = (
    await adminRequest(
      `/admin-api/community/comments/${rejectedCommentRow.id}/reject`,
      {
        method: 'POST',
        body: {
          expectedLockVersion: rejectedCommentRow.lockVersion,
          reason: `M2-T009 驳回验证 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(rejected.status, 'TAKEN_DOWN', 'comment reject')

  const commentBatchTargets = [
    batchCommentA,
    batchCommentB
  ].map((item) => ({
    id: String(item.data.commentId),
    expectedLockVersion: item.data.lockVersion ?? 0
  }))
  const momentBatchTargets = [
    batchMomentA,
    batchMomentB
  ].map((item) => ({
    id: String(item.data.moment.momentId),
    expectedLockVersion: item.data.moment.lockVersion ?? 0
  }))
  const batchDownComments = (
    await adminRequest(
      '/admin-api/community/comments/batch/take-down/execute',
      {
        method: 'POST',
        body: {
          targets: commentBatchTargets,
          reason: `M2-T009 批量下架 ${stamp}`
        }
      }
    )
  ).data
  const batchDownMoments = (
    await adminRequest(
      '/admin-api/community/moments/batch/take-down/execute',
      {
        method: 'POST',
        body: {
          targets: momentBatchTargets,
          reason: `M2-T009 批量下架 ${stamp}`
        }
      }
    )
  ).data
  assertEqual(batchDownComments.length, 2, 'batch comment down size')
  assertEqual(batchDownMoments.length, 2, 'batch moment down size')

  const batchRestoreComments = (
    await adminRequest(
      '/admin-api/community/comments/batch/restore/execute',
      {
        method: 'POST',
        body: {
          targets: batchDownComments.map((item) => ({
            id: item.id,
            expectedLockVersion: item.lockVersion
          })),
          reason: `M2-T009 批量恢复 ${stamp}`
        }
      }
    )
  ).data
  const batchRestoreMoments = (
    await adminRequest(
      '/admin-api/community/moments/batch/restore/execute',
      {
        method: 'POST',
        body: {
          targets: batchDownMoments.map((item) => ({
            id: item.id,
            expectedLockVersion: item.lockVersion
          })),
          reason: `M2-T009 批量恢复 ${stamp}`
        }
      }
    )
  ).data
  assertTrue(
    batchRestoreComments.every((item) => item.status === 'PUBLISHED'),
    'batch comment restore status'
  )
  assertTrue(
    batchRestoreMoments.every((item) => item.status === 'PUBLISHED'),
    'batch moment restore status'
  )

  const commentDetail = (
    await adminRequest(
      `/admin-api/community/comments/${pendingCommentRow.id}`
    )
  ).data
  const momentDetail = (
    await adminRequest(
      `/admin-api/community/moments/${pendingMomentRow.id}`
    )
  ).data
  assertTrue(commentDetail.events.length >= 4, 'comment event timeline')
  assertTrue(momentDetail.events.length >= 3, 'moment event timeline')

  const interactionCounts = {}
  for (const interactionType of ['LIKE', 'FAVORITE', 'FOLLOW']) {
    const page = (
      await adminRequest(
        '/admin-api/community/interactions'
        + `?interactionType=${interactionType}`
        + '&pageNum=1&pageSize=50'
      )
    ).data
    interactionCounts[interactionType] = page.total
    assertTrue(page.total >= 1, `${interactionType} interaction list`)
    assertTrue(
      page.list.every((item) =>
        !Object.hasOwn(item, 'folderId')
        && !Object.hasOwn(item, 'folderName')
      ),
      `${interactionType} leaked favorite folder privacy`
    )
  }

  mysql(`
    INSERT INTO sys_role
      (name, code, sort, status, remark, deleted, data_scope)
    VALUES
      (${sqlString(`M2 governance read only ${stamp}`)},
       ${sqlString(tempRole)}, 998, 1,
       'M2-T009 E2E', 0, 1);
    SET @role_id := LAST_INSERT_ID();
    INSERT INTO sys_user
      (username, password, must_change_password,
       temporary_password_issued_at, nickname, status, user_type, deleted)
    SELECT
      ${sqlString(tempAdmin)}, password, 1, UTC_TIMESTAMP(),
      ${sqlString(`M2 governance reader ${stamp}`)},
      1, 'admin', 0
    FROM sys_user
    WHERE username = 'admin'
    LIMIT 1;
    SET @user_id := LAST_INSERT_ID();
    INSERT INTO sys_user_role (user_id, role_id)
    VALUES (@user_id, @role_id);
    INSERT INTO sys_role_menu (role_id, menu_id)
    VALUES
      (@role_id, 9000),
      (@role_id, 9070),
      (@role_id, 9071),
      (@role_id, 9080),
      (@role_id, 9081),
      (@role_id, 9090),
      (@role_id, 9091);
  `)
  readOnlyToken = await loginAdmin(tempAdmin, adminPassword)
  const forcedPasswordGate = await adminRequest(
    '/api/dashboard/stats',
    { token: readOnlyToken, expectedCodes: [428] }
  )
  assertEqual(
    forcedPasswordGate.code,
    428,
    'mandatory first-login password change gate'
  )
  const forcedPasswordAdminApiGate = await adminRequest(
    '/admin-api/community/comments?pageNum=1&pageSize=5',
    { token: readOnlyToken, expectedCodes: [428] }
  )
  assertEqual(
    forcedPasswordAdminApiGate.code,
    428,
    'mandatory first-login password change gate on admin API'
  )
  await adminRequest('/api/auth/password', {
    method: 'POST',
    token: readOnlyToken,
    body: {
      oldPassword: encryptAdminPassword(adminPassword),
      newPassword: encryptAdminPassword(forcedReplacementPassword)
    }
  })
  assertEqual(
    mysql(`
      SELECT must_change_password
      FROM sys_user
      WHERE username = ${sqlString(tempAdmin)}
      LIMIT 1;
    `),
    '0',
    'password lifecycle flag cleared after password change'
  )
  const readOnlyList = await adminRequest(
    '/admin-api/community/comments?pageNum=1&pageSize=5',
    { token: readOnlyToken }
  )
  assertEqual(readOnlyList.code, 200, 'read-only list permission')
  const readOnlyDenied = await adminRequest(
    `/admin-api/community/moments/${publicMomentId}/take-down`,
    {
      method: 'POST',
      token: readOnlyToken,
      body: {
        expectedLockVersion: publicMoment.data.moment.lockVersion,
        reason: 'RBAC denied action'
      },
      expectedCodes: [403]
    }
  )
  assertEqual(readOnlyDenied.code, 403, 'read-only mutation isolation')

  const eventEvidence = mysql(`
    SELECT CONCAT(
      'commentEvents=', (
        SELECT COUNT(*)
        FROM community_comment_moderation_event e
        JOIN community_comment c ON c.id = e.comment_id
        WHERE c.author_user_id = ${userBId}
      ),
      ',momentEvents=', (
        SELECT COUNT(*)
        FROM community_moment_moderation_event e
        JOIN community_moment m ON m.id = e.moment_id
        WHERE m.actor_user_id = ${userAId}
      ),
      ',operLogs=', (
        SELECT COUNT(*)
        FROM sys_oper_log
        WHERE oper_url LIKE '/admin-api/community/%'
          AND oper_time >= NOW() - INTERVAL 10 MINUTE
          AND status = 0
      )
    );
  `)

  console.log(JSON.stringify({
    stamp,
    backend: 'UP',
    adminAuthentication: 'RSA + Sa-Token',
    websocketAuthentication: '30-second one-time ticket',
    forcedPasswordChange: '428 until changed',
    approveReplay: replayComment.replay,
    commentLifecycle: 'PENDING_REVIEW/PUBLISHED/TAKEN_DOWN/PUBLISHED',
    momentLifecycle: 'PENDING_REVIEW/PUBLISHED/TAKEN_DOWN/PUBLISHED',
    rejectedComment: rejected.status,
    batchComments: batchRestoreComments.length,
    batchMoments: batchRestoreMoments.length,
    interactionCounts,
    privacy: 'favorite folder metadata not exposed',
    rbac: 'list=200, mutation=403',
    eventEvidence,
    keptData: keepData,
    browserTargets: {
      commentId: pendingCommentRow.id,
      momentId: pendingMomentRow.id,
      publicMomentId: String(publicMomentId)
    }
  }, null, 2))
} catch (error) {
  failure = error
  console.error(`E2E_FAILED: ${error.message}`)
} finally {
  try {
    cleanupAdminPrincipal()
  } catch (error) {
    console.error(`ADMIN_CLEANUP_FAILED: ${error.message}`)
    failure ||= error
  }
  if (!keepData) {
    try {
      cleanupCommunityData()
      const evidence = mysql(`
        SELECT CONCAT(
          'users=', (
            SELECT COUNT(*) FROM community_user
            WHERE username IN (
              ${sqlString(usernameA)},
              ${sqlString(usernameB)}
            )
          ),
          ',rules=', (
            SELECT COUNT(*) FROM content_keyword_rule
            WHERE description = ${sqlString(`M2-T009 E2E ${stamp}`)}
          )
        );
      `)
      console.log(`CLEANUP: ${evidence}`)
    } catch (error) {
      console.error(`COMMUNITY_CLEANUP_FAILED: ${error.message}`)
      failure ||= error
    }
  } else {
    console.log(
      `KEEP_DATA: node scripts/e2e/m2-t009-admin-governance.mjs `
      + `--cleanup-stamp=${stamp}`
    )
  }
}

if (failure) {
  process.exitCode = 1
}
