<template>
  <div class="community-page">
    <header class="page-heading"><div><div class="page-eyebrow">ACCOUNT OPERATIONS</div><h1>账号操作中心</h1><p>按账号安全、账号控制和账号清理分级执行，并保留完整审计记录。</p></div></header>
    <n-tabs v-model:value="tab" type="line" class="operation-tabs">
      <n-tab-pane v-if="canUseSecurity" name="security" tab="账号安全" /><n-tab-pane v-if="canFreeze" name="control" tab="账号控制" /><n-tab-pane v-if="canApply" name="cleanup" tab="账号清理" /><n-tab-pane v-if="canApprove" name="review" tab="申请审批" /><n-tab-pane v-if="canApprove" name="appeals" tab="申诉复核" /><n-tab-pane name="records" tab="操作记录" />
    </n-tabs>

    <n-card v-if="isOperationTab" :title="operationTitle">
      <n-alert v-if="tab === 'security'" type="info" :show-icon="true" style="margin-bottom:16px">强制下线、重置登录凭证、锁定和解除锁定仅处理账号安全。</n-alert>
      <n-alert v-if="tab === 'control'" type="info" :show-icon="true" style="margin-bottom:16px">普通管理员只能执行最长 7 天的临时冻结或登录限制；长期措施须经超级管理员审批。</n-alert>
      <n-alert v-if="tab === 'cleanup'" type="warning" :show-icon="true" style="margin-bottom:16px">数据清理与账号删除仅提交申请；数据清理会在申诉期结束后按所选范围执行。</n-alert>
      <n-form class="grid" label-placement="top">
        <n-form-item label="目标用户" required><n-auto-complete v-model:value="form.targetUserId" clearable :options="userOptions" :loading="userSearching" placeholder="搜索用户 ID、用户名、昵称或邮箱" @update:value="searchUsers" /></n-form-item>
        <n-form-item label="操作" required><n-select v-model:value="form.measureType" :options="measures" /></n-form-item>
        <n-form-item v-if="requiresExpiry" label="生效截止时间" required><n-date-picker v-model:value="form.expiresAt" type="datetime" clearable /></n-form-item>
        <n-form-item label="标准原因" required><n-select v-model:value="form.reasonCode" :options="reasons" /></n-form-item>
      </n-form>
      <n-form-item v-if="form.measureType === 'DATA_CLEANUP'" label="清理范围" required><n-select v-model:value="form.cleanupScopes" multiple :options="cleanupScopeOptions" placeholder="至少选择一项" /></n-form-item>
      <n-form-item label="用户可见理由" required><n-input v-model:value="form.userVisibleReason" type="textarea" :maxlength="1000" placeholder="向用户说明操作原因，不得包含举报人或内部风控信息" /></n-form-item>
      <n-form-item label="内部处理说明" required><n-input v-model:value="form.internalReason" type="textarea" :maxlength="2000" placeholder="供管理员和审计查看的详细说明" /></n-form-item>
      <n-form-item label="证据说明" required><n-input v-model:value="form.evidenceDescription" type="textarea" :maxlength="20000" placeholder="关联内容、举报或事件时间线" /></n-form-item>
      <n-form-item label="证据附件"><n-upload multiple :max="10" :custom-request="uploadEvidence"><n-button>上传图片或文件</n-button></n-upload><n-space v-if="evidenceFiles.length" style="margin-top:8px" wrap><n-button v-for="file in evidenceFiles" :key="file.id" size="small" @click="previewEvidence(file)">{{ file.name }}</n-button></n-space></n-form-item>
      <n-space><n-button type="primary" :loading="submitting" @click="submit">{{ submitLabel }}</n-button><n-button @click="load">查看记录</n-button></n-space>
    </n-card>

    <n-card v-else-if="tab === 'review'" title="账号操作申请审批与最终确认">
      <n-form class="grid" label-placement="top"><n-form-item label="申请 ID"><n-auto-complete v-model:value="review.caseId" clearable :options="caseOptions" placeholder="搜索申请 ID、用户 ID 或操作类型" /></n-form-item><n-form-item label="当前审批环节"><n-input :value="currentReviewStageLabel" readonly /></n-form-item><n-form-item label="审核意见"><n-input v-model:value="review.note" placeholder="通过或驳回时必须填写审核意见" /></n-form-item></n-form>
      <n-card v-if="selectedReviewApplication" embedded title="申请详情" class="application-details">
        <n-descriptions bordered label-placement="left" :column="3">
          <n-descriptions-item label="申请 ID">{{ selectedReviewApplication.id }}</n-descriptions-item>
          <n-descriptions-item label="目标用户"><n-button text type="primary" :loading="reviewDetailsLoading" @click="showTargetUserDetails">{{ targetUserLabel }}</n-button></n-descriptions-item>
          <n-descriptions-item label="申请状态">{{ statusLabel(selectedReviewApplication.status) }}</n-descriptions-item>
          <n-descriptions-item label="申请操作">{{ operationLabels[selectedReviewApplication.measureType] || selectedReviewApplication.measureType }}</n-descriptions-item>
          <n-descriptions-item label="标准原因">{{ reasonLabel(selectedReviewApplication.reasonCode) }}</n-descriptions-item>
          <n-descriptions-item label="申请来源">{{ requestSourceLabel(selectedReviewApplication) }}</n-descriptions-item>
          <n-descriptions-item label="申请时间" :span="3">{{ formatDisplayDate(selectedReviewApplication.requestedAt) }}</n-descriptions-item>
          <n-descriptions-item label="生效截止时间" :span="3">{{ displayExpiry(selectedReviewApplication) }}</n-descriptions-item>
          <n-descriptions-item label="用户可见理由" :span="3">{{ selectedReviewApplication.userVisibleReason }}</n-descriptions-item>
          <n-descriptions-item label="内部处理说明" :span="3">{{ selectedReviewApplication.internalReason }}</n-descriptions-item>
          <n-descriptions-item label="证据快照" :span="3"><pre class="evidence-content">{{ formatEvidence(selectedReviewApplication.evidenceSnapshot) }}</pre></n-descriptions-item>
        </n-descriptions>
      </n-card>
      <n-card v-if="selectedReviewApplication" embedded title="审批流转记录" class="application-details">
        <n-timeline v-if="reviewHistory.length">
          <n-timeline-item v-for="item in reviewHistory" :key="item.id" :type="item.decision === 'APPROVE' ? 'success' : 'error'" :title="`${reviewStageLabel(item.stage)} · ${decisionLabel(item.decision)}`" :time="formatDisplayDate(item.createdAt)">
            审核人：{{ item.reviewerAdminId }}；意见：{{ item.reviewNote }}
          </n-timeline-item>
        </n-timeline>
        <n-empty v-else description="暂无审批记录" />
      </n-card>
      <n-alert v-if="selectedReviewApplication" :type="canReviewSelection || canFinalExecute ? 'info' : 'default'" :show-icon="true" style="margin-bottom:16px">{{ reviewGuidance }}</n-alert>
      <n-space v-if="canReviewSelection"><n-button type="primary" @click="reviewAction('APPROVE')">通过</n-button><n-button type="error" secondary @click="reviewAction('REJECT')">驳回</n-button></n-space>
      <n-button v-else-if="canFinalExecute" type="primary" @click="execute">最终确认并执行</n-button>
      <p v-if="canFinalExecute" class="hint">最终执行仅超级管理员可用，必须输入服务端要求的完整确认文字。</p>
    </n-card>
    <n-card v-else-if="tab === 'appeals'" title="申诉复核">
      <n-form class="grid" label-placement="top"><n-form-item label="申诉 ID"><n-auto-complete v-model:value="appeal.id" clearable :options="appealOptions" placeholder="搜索申诉 ID、申请 ID 或用户 ID" /></n-form-item><n-form-item label="裁决"><n-select v-model:value="appeal.decision" :options="appealDecisions" /></n-form-item><n-form-item v-if="appeal.decision === 'MODIFY'" label="修改后期限" required><n-date-picker v-model:value="appeal.modifiedExpiresAt" type="datetime" clearable /></n-form-item><n-form-item label="复核意见"><n-input v-model:value="appeal.note" /></n-form-item></n-form>
      <n-card v-if="selectedAppeal" embedded title="申诉与关联申请详情" class="application-details">
        <n-descriptions bordered label-placement="left" :column="3">
          <n-descriptions-item label="申诉 ID">{{ selectedAppeal.id }}</n-descriptions-item>
          <n-descriptions-item label="关联申请">{{ selectedAppeal.caseId }}</n-descriptions-item>
          <n-descriptions-item label="申诉用户">{{ selectedAppeal.appellantUserId }}</n-descriptions-item>
          <n-descriptions-item label="申诉状态">{{ statusLabel(selectedAppeal.status) }}</n-descriptions-item>
          <n-descriptions-item label="提交时间" :span="2">{{ selectedAppeal.createdAt || '—' }}</n-descriptions-item>
          <n-descriptions-item label="申诉说明" :span="3">{{ selectedAppeal.statement }}</n-descriptions-item>
          <template v-if="selectedAppealApplication">
            <n-descriptions-item label="申请操作">{{ operationLabels[selectedAppealApplication.measureType] || selectedAppealApplication.measureType }}</n-descriptions-item>
            <n-descriptions-item label="标准原因">{{ reasonLabel(selectedAppealApplication.reasonCode) }}</n-descriptions-item>
            <n-descriptions-item label="申请状态">{{ statusLabel(selectedAppealApplication.status) }}</n-descriptions-item>
            <n-descriptions-item label="用户可见理由" :span="3">{{ selectedAppealApplication.userVisibleReason }}</n-descriptions-item>
            <n-descriptions-item label="内部处理说明" :span="3">{{ selectedAppealApplication.internalReason }}</n-descriptions-item>
            <n-descriptions-item label="证据快照" :span="3"><pre class="evidence-content">{{ formatEvidence(selectedAppealApplication.evidenceSnapshot) }}</pre></n-descriptions-item>
          </template>
        </n-descriptions>
      </n-card>
      <n-button type="primary" @click="reviewAppeal">提交申诉裁决</n-button>
      <n-data-table style="margin-top:16px" :columns="appealColumns" :data="appeals" :loading="appealsLoading" :row-key="(row: CommunityAccountEnforcementAppeal) => row.id" />
    </n-card>
    <n-card v-else title="账号操作记录">
      <n-space style="margin-bottom:16px"><n-input v-model:value="queryUserId" placeholder="按用户 ID 查询" /><n-button @click="load">查询</n-button></n-space>
      <n-data-table :columns="columns" :data="rows" :loading="loading" :row-key="(row: CommunityAccountEnforcementCase) => row.id" />
      <template v-if="canApply">
        <n-divider />
        <h3>补充申请证据</h3>
        <n-alert type="info" :show-icon="true" style="margin-bottom:16px">仅申请发起人可为“要求补充证据”后退回的申请追加材料。</n-alert>
        <n-form class="grid" label-placement="top">
          <n-form-item label="申请 ID" required><n-auto-complete v-model:value="supplement.caseId" clearable :options="caseOptions" placeholder="搜索申请 ID、用户 ID 或操作类型" /></n-form-item>
          <n-form-item label="补充证据说明" required><n-input v-model:value="supplement.evidence" type="textarea" :maxlength="20000" placeholder="补充新的证据、关联内容或事件时间线" /></n-form-item>
        </n-form>
        <n-button type="primary" @click="submitSupplement">提交补充证据</n-button>
      </template>
    </n-card>
    <n-alert v-if="error" type="error" closable style="margin-top:16px" @close="error = ''">{{ error }}</n-alert>
    <n-modal v-model:show="showUserModal" preset="card" title="目标账户基本信息" style="width:min(720px, calc(100vw - 32px))">
      <n-spin v-if="reviewDetailsLoading" size="small">正在加载账户资料...</n-spin>
      <n-descriptions v-else-if="selectedTargetUser" bordered label-placement="left" :column="2">
        <n-descriptions-item label="用户 ID">{{ selectedTargetUser.id }}</n-descriptions-item>
        <n-descriptions-item label="账号状态">{{ accountStatusLabel(selectedTargetUser.status) }}</n-descriptions-item>
        <n-descriptions-item label="用户名">{{ selectedTargetUser.username }}</n-descriptions-item>
        <n-descriptions-item label="显示名称">{{ selectedTargetUser.displayName }}</n-descriptions-item>
        <n-descriptions-item label="邮箱"><span class="sensitive-reveal" title="悬停查看完整信息"><span class="sensitive-reveal__masked">{{ maskedEmail(selectedTargetUser.email) }}</span><span class="sensitive-reveal__full">{{ selectedTargetUser.email }}</span></span></n-descriptions-item>
        <n-descriptions-item label="认证状态">{{ verificationLabel(selectedTargetUser.verificationStatus) }}</n-descriptions-item>
        <n-descriptions-item label="关联博客">{{ selectedTargetUser.personalBlogName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="最近登录">{{ formatDisplayDate(selectedTargetUser.lastLoginAt) }}</n-descriptions-item>
        <n-descriptions-item label="注册时间" :span="2">{{ formatDisplayDate(selectedTargetUser.createdAt) }}</n-descriptions-item>
      </n-descriptions>
      <n-empty v-else description="未能加载账户资料" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NInput, type DataTableColumns, type UploadCustomRequestOptions, useDialog, useMessage } from 'naive-ui'
import { communityApi, type CommunityAccountEnforcementAppeal, type CommunityAccountEnforcementCase, type CommunityAccountEnforcementReview, type CommunityUser } from '@/api/community'
import { configGroupApi } from '@/api/org'
import { fileApi } from '@/api/system'
import { useUserStore } from '@/stores/user'

type EvidenceFile = { id: number; name: string; previewUrl: string }
const route = useRoute(), message = useMessage(), dialog = useDialog(), userStore = useUserStore()
const tab = ref('security'), loading = ref(false), submitting = ref(false), error = ref(''), queryUserId = ref(''), rows = ref<CommunityAccountEnforcementCase[]>([])
const form = reactive({ targetUserId: '', measureType: '', reasonCode: 'SECURITY_RISK', userVisibleReason: '', internalReason: '', evidenceDescription: '', cleanupScopes: [] as string[], expiresAt: null as number | null })
const evidenceFiles = ref<EvidenceFile[]>([]), userOptions = ref<Array<{ label: string; value: string }>>([]), userSearching = ref(false)
const review = reactive({ caseId: '', note: '' }), appeal = reactive({ id: '', decision: 'UPHOLD', note: '', modifiedExpiresAt: null as number | null }), supplement = reactive({ caseId: '', evidence: '' }), appeals = ref<CommunityAccountEnforcementAppeal[]>([]), appealsLoading = ref(false)
const reviewHistory = ref<CommunityAccountEnforcementReview[]>([]), selectedTargetUser = ref<CommunityUser | null>(null), reviewDetailsLoading = ref(false), showUserModal = ref(false), allowSuperAdminSelfReview = ref(true)
const groups = { security: { title: '账号安全', measures: [['PASSWORD_RESET', '强制重置登录凭证'], ['SECURITY_LOGOUT', '强制下线'], ['ACCOUNT_LOCK', '锁定账号'], ['ACCOUNT_UNLOCK', '解除锁定']] }, control: { title: '账号控制', measures: [['TEMP_FREEZE', '临时冻结'], ['FREEZE_EXTEND', '延长冻结'], ['FREEZE_RELEASE', '提前解冻'], ['LOGIN_BLOCK', '禁止登录'], ['LOGIN_RESTORE', '恢复登录'], ['LONG_FREEZE', '提交长期冻结申请']] }, cleanup: { title: '账号清理', measures: [['ACCOUNT_DEACTIVATE', '停用账号'], ['ACCOUNT_RESTORE', '恢复账号'], ['DATA_CLEANUP', '提交数据清理申请'], ['ACCOUNT_DELETE', '提交账号删除申请']] } }
const securityReasons = [['SECURITY_RISK', '账号安全风险'], ['ABNORMAL_LOGIN', '异常登录'], ['ACCOUNT_RECOVERY', '账号找回协助'], ['OTHER', '其他原因']], controlReasons = [['SECURITY_RISK', '安全风险'], ['ABNORMAL_LOGIN', '异常登录或疑似被盗'], ['RISK_INVESTIGATION', '风险调查'], ['OTHER', '其他原因']], cleanupReasons = [['TEST_ACCOUNT', '测试账号'], ['DUPLICATE_ACCOUNT', '重复账号'], ['UNVERIFIED_ABANDONED', '未验证废弃账号'], ['BULK_ABNORMAL', '异常批量账号'], ['CANCELLATION_REMAINDER', '注销遗留账号'], ['OTHER', '其他原因']]
const cleanupScopeOptions = [['PROFILE', '个人资料（昵称、简介、头像）'], ['ARTICLES', '文章'], ['MOMENTS', '动态'], ['COMMENTS', '评论'], ['CHAT_MESSAGES', '私信记录'], ['FILE_REFERENCES', '上传文件引用']].map(([value, label]) => ({ value, label }))
const canFreeze = computed(() => userStore.hasPermission('community:account:freeze')), canApply = computed(() => userStore.hasPermission('community:account:apply')), canApprove = computed(() => userStore.hasPermission('community:account:approve')), canExecute = computed(() => userStore.hasPermission('community:account:execute')), canUseSecurity = computed(() => canFreeze.value || canExecute.value), isSuperAdmin = computed(() => userStore.hasRole('admin'))
function availableMeasures(value: string) { const source = groups[value as keyof typeof groups]?.measures || []; return source.filter(([measure]) => measure !== 'PASSWORD_RESET' || canExecute.value).map(([measure, label]) => ({ value: measure, label })) }
const isOperationTab = computed(() => ['security', 'control', 'cleanup'].includes(tab.value)), operationTitle = computed(() => groups[tab.value as keyof typeof groups]?.title || '账号操作'), measures = computed(() => availableMeasures(tab.value)), reasons = computed(() => (tab.value === 'cleanup' ? cleanupReasons : tab.value === 'control' ? controlReasons : securityReasons).map(([value, label]) => ({ value, label }))), requiresExpiry = computed(() => ['TEMP_FREEZE', 'FREEZE_EXTEND', 'LOGIN_BLOCK', 'ACCOUNT_LOCK'].includes(form.measureType)), submitLabel = computed(() => ['LONG_FREEZE', 'DATA_CLEANUP', 'ACCOUNT_DELETE'].includes(form.measureType) ? '提交审批申请' : '执行操作')
const appealDecisions = [{ value: 'UPHOLD', label: '维持原处理' }, { value: 'MODIFY', label: '修改处理' }, { value: 'REVOKE', label: '撤销处理' }]
const operationLabels = Object.fromEntries([...groups.security.measures, ...groups.control.measures, ...groups.cleanup.measures]), caseOptions = computed(() => rows.value.map(item => ({ value: item.id, label: `${item.id} · 用户 ${item.targetUserId} · ${operationLabels[item.measureType] || item.measureType} · ${statusLabel(item.status)}` }))), appealOptions = computed(() => appeals.value.map(item => ({ value: item.id, label: `${item.id} · 申请 ${item.caseId} · 用户 ${item.appellantUserId} · ${statusLabel(item.status)}` })))
const selectedReviewApplication = computed(() => rows.value.find(item => item.id === numericId(review.caseId)))
const selectedAppeal = computed(() => appeals.value.find(item => item.id === numericId(appeal.id)))
const selectedAppealApplication = computed(() => rows.value.find(item => item.id === selectedAppeal.value?.caseId))
const currentAdminId = computed(() => String(userStore.user?.id || ''))
const isOwnReviewApplication = computed(() => !!selectedReviewApplication.value?.requestedByAdminId && selectedReviewApplication.value.requestedByAdminId === currentAdminId.value)
const hasReviewedSelection = computed(() => reviewHistory.value.some(item => item.reviewerAdminId === currentAdminId.value))
const currentReviewStageLabel = computed(() => {
  if (!selectedReviewApplication.value) return '请先选择申请'
  return selectedReviewApplication.value.status === 'SUBMITTED' ? '初审' : selectedReviewApplication.value.status === 'UNDER_REVIEW' ? '超级管理员复审' : selectedReviewApplication.value.status === 'APPROVED' ? '最终执行确认' : '流程已结束'
})
const canReviewSelection = computed(() => canApprove.value && !reviewDetailsLoading.value && (!isOwnReviewApplication.value || (isSuperAdmin.value && allowSuperAdminSelfReview.value)) && !hasReviewedSelection.value && !!selectedReviewApplication.value && (selectedReviewApplication.value.status === 'SUBMITTED' || (selectedReviewApplication.value.status === 'UNDER_REVIEW' && isSuperAdmin.value)))
const canFinalExecute = computed(() => canExecute.value && isSuperAdmin.value && selectedReviewApplication.value?.status === 'APPROVED')
const reviewGuidance = computed(() => {
  const item = selectedReviewApplication.value
  if (!item) return ''
  if (isOwnReviewApplication.value && (!isSuperAdmin.value || !allowSuperAdminSelfReview.value)) return '申请发起人不能审核自己的申请，请由其他具备审批权限的管理员处理。'
  if (hasReviewedSelection.value) return '你已参与过该申请的审批，后续环节须由其他符合权限要求的管理员处理。'
  if (item.status === 'SUBMITTED') return '当前为初审。通过后申请将流转至超级管理员复审；驳回后流程终止。'
  if (item.status === 'UNDER_REVIEW') return isSuperAdmin.value ? '当前为超级管理员复审。通过后进入最终执行确认；驳回后流程终止。' : '初审已通过，正在等待超级管理员复审。'
  if (item.status === 'APPROVED') return canFinalExecute.value ? '多级审批已完成，请复核全部资料后执行最终确认。' : '多级审批已完成，等待超级管理员最终执行。'
  return `当前申请状态为“${statusLabel(item.status)}”，暂无可执行的审批操作。`
})
const targetUserLabel = computed(() => selectedTargetUser.value ? `${selectedTargetUser.value.displayName}（${selectedTargetUser.value.username}，ID：${selectedTargetUser.value.id}）` : selectedReviewApplication.value?.targetUserId || '-')
const statusLabels: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '已提交', UNDER_REVIEW: '审核中', APPROVED: '已批准', ACTIVE: '执行中', APPEAL_WINDOW: '申诉等待期', APPEALED: '申诉中', FINALIZED: '已完成', REJECTED: '已驳回', REVOKED: '已撤销', EXECUTION_FAILED: '执行失败', UPHELD: '维持原处理', MODIFIED: '已修改处理' }
const reasonLabels = Object.fromEntries([...securityReasons, ...controlReasons, ...cleanupReasons])
const statusLabel = (value: string) => statusLabels[value] || value, reasonLabel = (value: string) => reasonLabels[value] || value
const reviewStageLabel = (value: string) => ({ INITIAL: '初审', SECONDARY: '超级管理员复审', FINAL: '最终确认' }[value] || value)
const decisionLabel = (value: string) => ({ APPROVE: '通过', REJECT: '驳回', RETURN_FOR_EVIDENCE: '要求补充证据' }[value] || value)
function formatEvidence(value: string) { try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value || '—' } }
function formatDisplayDate(value?: string | null) { return value ? value.replace('T', ' ').slice(0, 19) : '-' }
function displayExpiry(item: CommunityAccountEnforcementCase) { if (item.expiresAt) return formatDisplayDate(item.expiresAt); return item.measureType === 'LONG_FREEZE' ? '长期' : '-' }
function requestSourceLabel(item: CommunityAccountEnforcementCase) { return item.requestedByTeamId ? `团队 ${item.requestedByTeamId}` : item.requestedByAdminId ? `管理员 ${item.requestedByAdminId}` : '-' }
function maskedValue(value: string, visiblePrefix = 2) { return value.length <= visiblePrefix + 1 ? `${value.slice(0, 1)}***` : `${value.slice(0, visiblePrefix)}***${value.slice(-1)}` }
function maskedEmail(email: string) { const [local = '', domain = ''] = email.split('@'); return `${maskedValue(local)}@${maskedValue(domain, 1)}` }
function accountStatusLabel(value: string) { return ({ NORMAL: '正常', FROZEN: '已冻结', DEACTIVATED: '已停用', DISABLED: '已禁用', BANNED: '已封禁', DELETED: '已删除' }[value] || value) }
function verificationLabel(value: string) { return ({ VERIFIED: '已认证', PENDING: '待认证', UNVERIFIED: '未认证' }[value] || value) }
const formatDate = (value: number) => new Date(value).toISOString().slice(0, 19).replace('T', ' '), failure = (value: unknown) => value instanceof Error ? value.message : '请求失败，请稍后重试'
const numericId = (value: string) => value.trim().match(/^\d+/)?.[0] || ''
function resetOperation(value: string) { const first = availableMeasures(value)[0]; if (first) { form.measureType = first.value; form.reasonCode = value === 'cleanup' ? 'TEST_ACCOUNT' : 'SECURITY_RISK'; form.expiresAt = null; form.cleanupScopes = [] } }
watch(tab, value => resetOperation(value))
async function searchUsers(keyword: string) { if (!keyword.trim()) { userOptions.value = []; return }; userSearching.value = true; try { const page = await communityApi.users({ keyword: keyword.trim(), pageNum: 1, pageSize: 20 }); userOptions.value = page.list.map(user => ({ value: user.id, label: `${user.username}（${user.displayName}，ID：${user.id}）` })) } finally { userSearching.value = false } }
function resolvedTargetUserId() { const raw = form.targetUserId.trim(); return /^\d+$/.test(raw) ? raw : userOptions.value.find(option => option.label === raw)?.value || raw.match(/ID[：:]\s*(\d+)/)?.[1] || '' }
function evidenceSnapshot() { return JSON.stringify({ description: form.evidenceDescription.trim(), attachments: evidenceFiles.value.map(({ id, name }) => ({ id, name })) }) }
function valid() { if (!resolvedTargetUserId()) { error.value = '请选择联想列表中的用户，或输入纯数字用户 ID'; return false } if (!form.userVisibleReason.trim() || !form.internalReason.trim() || !form.evidenceDescription.trim()) { error.value = '请完整填写双理由与证据说明'; return false } if (form.measureType === 'DATA_CLEANUP' && !form.cleanupScopes.length) { error.value = '请至少选择一项清理范围'; return false } if (requiresExpiry.value && !form.expiresAt) { error.value = '该操作必须设置到期时间'; return false } return true }
function payload() { return { targetUserId: resolvedTargetUserId(), measureType: form.measureType, reasonCode: form.reasonCode, userVisibleReason: form.userVisibleReason.trim(), internalReason: form.internalReason.trim(), evidenceSnapshot: evidenceSnapshot(), cleanupScope: form.measureType === 'DATA_CLEANUP' ? JSON.stringify(form.cleanupScopes) : undefined, expiresAt: form.expiresAt ? formatDate(form.expiresAt) : undefined } }
async function uploadEvidence(options: UploadCustomRequestOptions) { try { const item = await fileApi.upload(options.file.file as File); if (!item.id) throw new Error('文件上传后未返回文件 ID'); evidenceFiles.value.push({ id: item.id, name: item.originalName, previewUrl: fileApi.getPreviewUrl(item.id) }); options.onFinish() } catch (cause) { options.onError(); error.value = failure(cause) } }
function previewEvidence(file: EvidenceFile) { window.open(file.previewUrl, '_blank', 'noopener,noreferrer') }
async function submit() { if (!valid()) return; submitting.value = true; error.value = ''; try { const data = payload(), userId = data.targetUserId; if (data.measureType === 'PASSWORD_RESET') { const result = await communityApi.forcePasswordReset(userId, data); dialog.success({ title: '一次性临时密码', content: `请立即安全转交给用户：${result.temporaryPassword}。原密码和全部会话已失效，首次登录后必须修改密码。`, positiveText: '我已记录并安全转交' }) } else if (data.measureType === 'SECURITY_LOGOUT') await communityApi.forceLogout(userId, data); else if (data.measureType === 'ACCOUNT_LOCK') await communityApi.lockAccount(userId, data); else if (data.measureType === 'ACCOUNT_UNLOCK') await communityApi.unlockAccount(userId, data); else if (data.measureType === 'TEMP_FREEZE') await communityApi.freezeAccount(data); else if (data.measureType === 'FREEZE_EXTEND') await communityApi.extendAccountFreeze(data); else if (data.measureType === 'FREEZE_RELEASE') await communityApi.releaseAccountFreeze(userId, data); else if (data.measureType === 'LOGIN_BLOCK') await communityApi.blockAccountLogin(userId, data); else if (data.measureType === 'LOGIN_RESTORE') await communityApi.restoreAccountLogin(userId, data); else if (data.measureType === 'ACCOUNT_DEACTIVATE') await communityApi.deactivateAccount(userId, data); else if (data.measureType === 'ACCOUNT_RESTORE') await communityApi.restoreAccount(userId, data); else await communityApi.submitAccountEnforcement(data); queryUserId.value = userId; tab.value = 'records'; message.success(`${submitLabel.value}成功`); await load() } catch (cause) { error.value = failure(cause) } finally { submitting.value = false } }
async function load() { loading.value = true; try { rows.value = await communityApi.accountEnforcements(queryUserId.value || resolvedTargetUserId() || undefined) } catch (cause) { error.value = failure(cause) } finally { loading.value = false } }
async function loadAppeals() { appealsLoading.value = true; try { appeals.value = await communityApi.accountEnforcementAppeals() } catch (cause) { error.value = failure(cause) } finally { appealsLoading.value = false } }
async function loadReviewContext(item?: CommunityAccountEnforcementCase) {
  selectedTargetUser.value = null
  reviewHistory.value = []
  if (!item) return
  reviewDetailsLoading.value = true
  try {
    const [usersResult, reviewsResult] = await Promise.allSettled([
      communityApi.users({ keyword: item.targetUserId, pageNum: 1, pageSize: 20 }),
      communityApi.accountEnforcementReviews(item.id)
    ])
    if (usersResult.status === 'fulfilled') selectedTargetUser.value = usersResult.value.list.find(user => user.id === item.targetUserId) || null
    if (reviewsResult.status === 'fulfilled') reviewHistory.value = reviewsResult.value
  } finally {
    reviewDetailsLoading.value = false
  }
}
async function showTargetUserDetails() {
  showUserModal.value = true
  const userId = selectedReviewApplication.value?.targetUserId
  if (!userId || selectedTargetUser.value?.id === userId) return
  reviewDetailsLoading.value = true
  try {
    const result = await communityApi.users({ keyword: userId, pageNum: 1, pageSize: 20 })
    selectedTargetUser.value = result.list.find(user => user.id === userId) || null
  } catch (cause) {
    error.value = failure(cause)
  } finally {
    reviewDetailsLoading.value = false
  }
}
async function reviewAction(decision: 'APPROVE' | 'REJECT') {
  const caseId = numericId(review.caseId)
  if (!caseId || !review.note.trim()) { error.value = '申请 ID 和审核意见必填'; return }
  if (!canReviewSelection.value) { error.value = '当前身份或申请状态不允许执行该审批操作'; return }
  try {
    await communityApi.reviewAccountEnforcement(caseId, { decision, reviewNote: review.note.trim() })
    review.note = ''
    message.success(decision === 'APPROVE' ? '审批通过，已流转至下一环节' : '申请已驳回')
    await load()
    await loadReviewContext(selectedReviewApplication.value)
  } catch (cause) { error.value = failure(cause) }
}
async function submitSupplement() { const caseId = numericId(supplement.caseId); if (!caseId || !supplement.evidence.trim()) { error.value = '申请 ID 和补充证据说明必填'; return }; try { const snapshot = JSON.stringify({ description: supplement.evidence.trim(), supplementedAt: new Date().toISOString() }); await communityApi.supplementAccountEnforcementEvidence(caseId, snapshot); supplement.evidence = ''; message.success('补充证据已提交'); await load() } catch (cause) { error.value = failure(cause) } }
async function execute() {
  const caseId = numericId(review.caseId)
  if (!caseId) { error.value = '请输入申请 ID'; return }
  let expectedConfirmation = ''
  try {
    const result = await communityApi.accountEnforcementConfirmationText(caseId)
    expectedConfirmation = result.confirmationText
  } catch (cause) {
    error.value = failure(cause)
    return
  }
  let confirmation = ''
  dialog.warning({
    title: '最终确认',
    content: () => h('div', { class: 'final-confirmation-field' }, [
      h('div', { class: 'final-confirmation-prompt' }, [
        h('div', { class: 'final-confirmation-prompt-label' }, '请完整输入以下文字'),
        h('div', { class: 'final-confirmation-required-text' }, expectedConfirmation)
      ]),
      h('div', { class: 'final-confirmation-label' }, '确认文字'),
      h(NInput, {
        value: confirmation,
        type: 'textarea',
        autosize: { minRows: 2, maxRows: 4 },
        placeholder: '请在此输入上方完整确认文字',
        clearable: true,
        autofocus: true,
        'onUpdate:value': (value: string) => { confirmation = value }
      }),
      h('div', { class: 'final-confirmation-hint' }, '确认文字必须完整匹配，提交后账号将进入申诉等待期。')
    ]),
    positiveText: '确认执行',
    negativeText: '取消',
    onPositiveClick: async () => {
      if (!confirmation.trim()) { message.warning('请输入完整确认文字'); return false }
      try {
        await communityApi.executeAccountEnforcement(caseId, confirmation.trim())
        message.success('已进入申诉等待期')
        await load()
      } catch (cause) {
        error.value = failure(cause)
        return false
      }
    }
  })
}
async function reviewAppeal() { const appealId = numericId(appeal.id); if (!appealId || !appeal.note.trim()) { error.value = '申诉 ID 和复核意见必填'; return } if (appeal.decision === 'MODIFY' && !appeal.modifiedExpiresAt) { error.value = '修改处理必须设置新的期限'; return }; try { await communityApi.reviewAccountEnforcementAppeal(appealId, { decision: appeal.decision, reviewNote: appeal.note.trim(), modifiedExpiresAt: appeal.modifiedExpiresAt ? formatDate(appeal.modifiedExpiresAt) : undefined }); message.success('申诉已裁决'); appeal.modifiedExpiresAt = null; await loadAppeals() } catch (cause) { error.value = failure(cause) } }
watch(selectedReviewApplication, item => { void loadReviewContext(item) })
async function loadApprovalSettings() { try { const group = await configGroupApi.getByCode('system'); if (!group?.configValue) return; const config = JSON.parse(group.configValue); if (typeof config.allowSuperAdminSelfReview === 'boolean') allowSuperAdminSelfReview.value = config.allowSuperAdminSelfReview } catch { /* system setting is optional; backend remains authoritative */ } }
onMounted(() => { const userId = route.query.userId; if (typeof userId === 'string' && /^\d+$/.test(userId)) { form.targetUserId = userId; queryUserId.value = userId }; if (!canUseSecurity.value) tab.value = canFreeze.value ? 'control' : canApply.value ? 'cleanup' : canApprove.value ? 'review' : 'records'; resetOperation(tab.value); void load(); void loadApprovalSettings(); if (canApprove.value) void loadAppeals() })
const columns: DataTableColumns<CommunityAccountEnforcementCase> = [{ title: '申请', key: 'id', width: 180 }, { title: '用户', key: 'targetUserId', width: 150 }, { title: '操作', key: 'measureType', width: 170, render: row => operationLabels[row.measureType] || row.measureType }, { title: '状态', key: 'status', width: 140, render: row => statusLabel(row.status) }, { title: '原因', key: 'reasonCode', width: 160, render: row => reasonLabel(row.reasonCode) }, { title: '申请时间', key: 'requestedAt', minWidth: 180 }]
const appealColumns: DataTableColumns<CommunityAccountEnforcementAppeal> = [{ title: '申诉', key: 'id', width: 180 }, { title: '关联申请', key: 'caseId', width: 180 }, { title: '用户', key: 'appellantUserId', width: 150 }, { title: '状态', key: 'status', width: 140, render: row => statusLabel(row.status) }, { title: '申诉说明', key: 'statement', minWidth: 240 }]
</script>

<style scoped>
.community-page { padding: 24px; }.page-heading { margin-bottom: 24px; }.page-eyebrow { color: var(--n-text-color-3); font-size: 12px; }.page-heading h1 { margin: 8px 0; }.page-heading p,.hint { color: var(--n-text-color-3); margin: 0; }.operation-tabs :deep(.n-tabs-nav) { margin-bottom: 16px; }.grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:4px 16px; }
.final-confirmation-field { display: grid; gap: 8px; min-width: 0; padding-top: 2px; }
.final-confirmation-label { color: var(--n-text-color); font-size: 14px; font-weight: 600; }
.final-confirmation-prompt { display: grid; gap: 6px; padding: 12px 14px; border: 1px solid var(--n-border-color); border-radius: 8px; background: var(--n-color-embedded); }
.final-confirmation-prompt-label { color: var(--n-text-color-3); font-size: 13px; }
.final-confirmation-required-text { color: var(--n-text-color); font-size: 14px; font-weight: 600; line-height: 1.6; overflow-wrap: anywhere; user-select: text; }
.final-confirmation-hint { color: var(--n-text-color-3); font-size: 13px; line-height: 1.6; }
.application-details { margin-bottom: 16px; }
.evidence-content { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font-family: inherit; }
</style>
