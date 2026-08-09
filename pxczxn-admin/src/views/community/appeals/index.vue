<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <h1>申诉中心</h1>
        <p>复核举报申诉，审批账号措施申请（长期冻结、数据清理、账号删除）。</p>
      </div>
      <n-button :loading="loading" @click="load">刷新</n-button>
    </header>

    <n-tabs v-model:value="tab" type="line">
      <n-tab-pane name="report" tab="举报申诉" />
      <n-tab-pane v-if="canApprove" name="accountAppeals" tab="账号申诉" />
      <n-tab-pane v-if="canApprove" name="applications" tab="申请审批" />
    </n-tabs>

    <!-- 举报申诉 -->
    <n-card v-if="tab === 'report'">
      <n-empty v-if="!loading && appeals.length === 0" description="暂无待复核申诉" />
      <n-data-table
        v-else
        :columns="appealColumns"
        :data="appeals"
        :loading="loading"
        :row-key="(row: CommunityAppeal) => row.id"
        :bordered="false"
      />
    </n-card>

    <!-- 账号申诉 -->
    <n-card v-else-if="tab === 'accountAppeals'" title="账号措施申诉">
      <n-empty v-if="!accountAppealLoading && accountAppeals.length === 0" description="暂无账号申诉" />
      <n-data-table v-else :columns="accountAppealColumns" :data="accountAppeals" :loading="accountAppealLoading" :row-key="(row: CommunityAccountEnforcementAppeal) => row.id" :bordered="false" />
    </n-card>

    <!-- 申请审批 -->
    <n-card v-else-if="tab === 'applications'" title="账号措施申请">
      <n-space align="center" style="margin-bottom: 16px" :wrap="true">
        <n-input v-model:value="queryUserId" inputmode="numeric" placeholder="按用户 ID 筛选" clearable style="width: 220px" @keyup.enter="loadApplications" />
        <n-select v-model:value="statusFilter" :options="statusOptions" clearable placeholder="全部状态" style="width: 160px" />
        <n-button :loading="applicationLoading" @click="loadApplications">查询</n-button>
        <n-button quaternary @click="resetQuery">重置</n-button>
      </n-space>
      <n-empty v-if="!applicationLoading && filteredRows.length === 0" description="暂无申请" />
      <n-data-table
        v-else
        :columns="applicationColumns"
        :data="filteredRows"
        :loading="applicationLoading"
        :row-key="(row: CommunityAccountEnforcementCase) => row.id"
        :pagination="pagination"
        :bordered="false"
      />
    </n-card>

    <n-alert v-if="error" type="error" closable style="margin-top: 16px" @close="error = ''">{{ error }}</n-alert>

    <!-- 审核弹窗 -->
    <n-modal v-model:show="showReviewModal" preset="card" title="申请审核" style="width: min(560px, calc(100vw - 32px))">
      <n-form label-placement="top">
        <n-form-item label="审核决定">
          <n-radio-group v-model:value="reviewDecision">
            <n-radio value="APPROVE">通过</n-radio>
            <n-radio value="REJECT">驳回</n-radio>
            <n-radio value="RETURN_FOR_EVIDENCE">退回补充证据</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="审核意见" required>
          <n-input v-model:value="reviewNote" type="textarea" :maxlength="2000" :autosize="{ minRows: 3, maxRows: 6 }" placeholder="填写审核意见（必填）" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReviewModal = false">取消</n-button>
          <n-button type="primary" :loading="reviewing" :disabled="!reviewNote.trim()" @click="confirmReview">提交审核</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="showAccountAppealModal" preset="card" title="处理账号申诉" style="width: min(620px, calc(100vw - 32px))">
      <template v-if="accountAppealTarget">
        <n-descriptions bordered label-placement="left" :column="1" style="margin-bottom: 16px">
          <n-descriptions-item label="申诉人">用户 #{{ accountAppealTarget.appellantUserId }}</n-descriptions-item>
          <n-descriptions-item label="关联措施">#{{ accountAppealTarget.caseId }}</n-descriptions-item>
          <n-descriptions-item label="申诉说明">{{ accountAppealTarget.statement }}</n-descriptions-item>
          <n-descriptions-item label="补充证据">
            <n-space v-if="accountAppealFiles.length" :wrap="true">
              <a v-for="fileId in accountAppealFiles" :key="fileId" :href="accountAppealFileUrl(accountAppealTarget.id, fileId)" target="_blank" rel="noopener">查看附件 #{{ fileId }}</a>
            </n-space>
            <span v-else>未上传附件</span>
          </n-descriptions-item>
        </n-descriptions>
        <n-form label-placement="top">
          <n-form-item label="裁决">
            <n-radio-group v-model:value="accountAppealDecision">
              <n-radio value="UPHOLD">维持原处置</n-radio><n-radio value="MODIFY">调整期限</n-radio><n-radio value="REVOKE">撤销处置</n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item v-if="accountAppealDecision === 'MODIFY'" label="新的到期时间" required><n-date-picker v-model:formatted-value="accountAppealExpiresAt" value-format="yyyy-MM-dd'T'HH:mm:ss" type="datetime" clearable /></n-form-item>
          <n-form-item label="审核意见" required><n-input v-model:value="accountAppealNote" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" maxlength="2000" placeholder="填写审核意见" /></n-form-item>
        </n-form>
      </template>
      <template #footer><n-space justify="end"><n-button @click="showAccountAppealModal = false">取消</n-button><n-button type="primary" :loading="accountAppealReviewing" :disabled="!accountAppealNote.trim() || (accountAppealDecision === 'MODIFY' && !accountAppealExpiresAt)" @click="confirmAccountAppealReview">{{ accountAppealTarget?.status === 'PRIMARY_REVIEWED' ? '提交终审' : '提交初审' }}</n-button></n-space></template>
    </n-modal>

    <!-- 最终执行弹窗 -->
    <n-modal v-model:show="showExecuteModal" preset="card" title="最终执行" style="width: min(560px, calc(100vw - 32px))">
      <n-alert type="error" :show-icon="true" style="margin-bottom: 12px">
        执行后不可撤销，请仔细核对目标账号与措施类型。
      </n-alert>
      <p class="execute-target">目标：用户 #{{ executeTarget?.targetUserId }} · {{ measureLabel(executeTarget?.measureType) }}</p>
      <n-alert type="warning" :show-icon="true" style="margin-bottom: 12px">
        <div class="confirmation-hint">请完整输入以下确认文字后执行：</div>
        <pre class="confirmation-text">{{ executeConfirmationText }}</pre>
      </n-alert>
      <n-form-item label="输入确认文字">
        <n-input v-model:value="executeConfirmation" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="请完整输入上方确认文字" />
      </n-form-item>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showExecuteModal = false">取消</n-button>
          <n-button type="error" :loading="executing" :disabled="executeConfirmation.trim() !== executeConfirmationText" @click="confirmExecute">确认执行</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 申请详情弹窗 -->
    <n-modal v-model:show="showDetailModal" preset="card" title="申请详情" style="width: min(760px, calc(100vw - 32px))">
      <n-spin v-if="detailLoading">正在加载申请详情</n-spin>
      <template v-else-if="detail">
        <n-descriptions bordered label-placement="left" :column="2">
          <n-descriptions-item label="申请 ID">{{ detail.id }}</n-descriptions-item>
          <n-descriptions-item label="状态">{{ statusLabel(detail.status) }}</n-descriptions-item>
          <n-descriptions-item label="目标用户">
            <n-button text type="primary" :loading="communityUserDetailLoading" @click.stop="openCommunityUserDetail(detail.targetUserId)">
              {{ communityUserLabel(detail.targetUserId) }}
            </n-button>
          </n-descriptions-item>
          <n-descriptions-item label="措施类型">{{ measureLabel(detail.measureType) }}</n-descriptions-item>
          <n-descriptions-item label="标准原因">{{ reasonLabels[detail.reasonCode] || detail.reasonCode }}</n-descriptions-item>
          <n-descriptions-item label="关联举报">{{ detail.sourceReportId || '-' }}</n-descriptions-item>
          <n-descriptions-item label="到期时间">{{ detail.expiresAt ? formatDateTime(detail.expiresAt) : '-' }}</n-descriptions-item>
          <n-descriptions-item label="清理范围">{{ cleanupScopeLabel(detail.cleanupScope) }}</n-descriptions-item>
          <n-descriptions-item label="提交时间">{{ formatDateTime(detail.requestedAt) }}</n-descriptions-item>
          <n-descriptions-item label="提交人">
            <template v-if="detail.requestedByAdminId">
              <n-button text type="primary" @click="openAdminDetail(detail.requestedByAdminId)">{{ adminLabel(detail.requestedByAdminId) }}</n-button>
            </template>
            <template v-else>-</template>
          </n-descriptions-item>
          <n-descriptions-item label="用户可见理由" :span="2">{{ detail.userVisibleReason }}</n-descriptions-item>
          <n-descriptions-item label="内部说明" :span="2">{{ detail.internalReason }}</n-descriptions-item>
          <n-descriptions-item label="证据说明" :span="2">
            <template v-if="evidenceParsed">
              <p v-if="evidenceParsed.description" class="evidence-desc">{{ evidenceParsed.description }}</p>
              <div v-if="evidenceParsed.attachments && evidenceParsed.attachments.length" class="evidence-attachments">
                <a
                  v-for="att in evidenceParsed.attachments"
                  :key="att.url"
                  class="evidence-file"
                  :href="att.url"
                  target="_blank"
                  rel="noopener"
                >
                  <img v-if="isImageFile(att.url)" class="evidence-thumb" :src="att.url" :alt="att.name" />
                  <span v-else class="evidence-file-name">{{ att.name }}</span>
                </a>
              </div>
              <span v-if="!evidenceParsed.description && (!evidenceParsed.attachments || evidenceParsed.attachments.length === 0)" class="evidence-desc">无证据内容</span>
            </template>
            <pre v-else class="evidence-content">{{ detail.evidenceSnapshot }}</pre>
          </n-descriptions-item>
        </n-descriptions>
        <n-divider>审核记录</n-divider>
        <n-empty v-if="reviews.length === 0" description="暂无审核记录" />
        <n-timeline v-else>
          <n-timeline-item
            v-for="review in reviews"
            :key="review.id"
            :title="`${reviewDecisionLabel(review.decision)} · ${reviewStageLabels[review.stage] || review.stage}`"
            :time="formatDateTime(review.createdAt)"
          >
            <span>{{ reviewNoteLabel(review) }} · 审核人：</span>
            <n-button text type="primary" @click="openAdminDetail(review.reviewerAdminId)">
              {{ adminLabel(review.reviewerAdminId) }}
            </n-button>
          </n-timeline-item>
        </n-timeline>
      </template>
      <n-empty v-else description="未找到申请详情" />
    </n-modal>
    <!-- 社区用户资料弹窗 -->
    <n-modal v-model:show="showCommunityUserModal" preset="card" title="用户资料" style="width: min(560px, calc(100vw - 32px))">
      <n-spin v-if="communityUserDetailLoading">正在加载用户资料</n-spin>
      <n-descriptions v-else-if="communityUserDetail" bordered label-placement="left" :column="2">
        <n-descriptions-item label="用户 ID">{{ communityUserDetail.id }}</n-descriptions-item>
        <n-descriptions-item label="昵称">{{ communityUserDetail.displayName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="用户名">{{ communityUserDetail.username }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ communityStatusLabel(communityUserDetail.status) }}</n-descriptions-item>
        <n-descriptions-item label="邮箱">{{ communityUserDetail.email || '-' }}</n-descriptions-item>
        <n-descriptions-item label="认证">{{ communityStatusLabel(communityUserDetail.verificationStatus) }}</n-descriptions-item>
        <n-descriptions-item label="个人博客">{{ communityUserDetail.personalBlogName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="最近登录">{{ formatDateTime(communityUserDetail.lastLoginAt) }}</n-descriptions-item>
        <n-descriptions-item label="注册时间" :span="2">{{ formatDateTime(communityUserDetail.createdAt) }}</n-descriptions-item>
      </n-descriptions>
      <n-empty v-else description="未找到用户资料" />
    </n-modal>
    <!-- 管理员资料弹窗 -->
    <n-modal v-model:show="showAdminModal" preset="card" title="管理员资料" style="width: min(560px, calc(100vw - 32px))">
      <n-spin v-if="adminDetailLoading">正在加载管理员资料</n-spin>
      <n-descriptions v-else-if="adminDetail" bordered label-placement="left" :column="2">
        <n-descriptions-item label="用户 ID">{{ adminDetail.user.id }}</n-descriptions-item>
        <n-descriptions-item label="昵称">{{ adminDetail.user.nickname || '-' }}</n-descriptions-item>
        <n-descriptions-item label="用户名">{{ adminDetail.user.username }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ adminDetail.user.status === 1 ? '启用' : '停用' }}</n-descriptions-item>
        <n-descriptions-item label="邮箱">{{ adminDetail.user.email || '-' }}</n-descriptions-item>
        <n-descriptions-item label="手机">{{ adminDetail.user.phone || '-' }}</n-descriptions-item>
        <n-descriptions-item label="部门">{{ adminDetail.user.deptName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="身份">{{ adminIdentity(adminDetail.roleIds) }}</n-descriptions-item>
      </n-descriptions>
      <n-empty v-else description="未找到管理员资料" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { NButton, NTag, useDialog, useMessage } from 'naive-ui'
import { communityApi, type CommunityAccountEnforcementAppeal, type CommunityAccountEnforcementCase, type CommunityAccountEnforcementReview, type CommunityAppeal, type CommunityUser } from '@/api/community'
import { roleApi, userApi, type SysRole, type SysUser, type UserDetailResult } from '@/api/system'
import { formatDateTime, statusLabel as communityStatusLabel } from '@/utils/community'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const message = useMessage()
const dialog = useDialog()

const canApprove = userStore.hasPermission('community:account:approve')
const canExecute = userStore.hasPermission('community:account:execute')
const isSuperAdmin = userStore.hasRole('admin')

const tab = ref('report')
const loading = ref(false)
const error = ref('')

const describeError = (cause: unknown) => (cause instanceof Error ? cause.message : '请求失败，请稍后重试')

/* ---------- 举报申诉 ---------- */
const appeals = ref<CommunityAppeal[]>([])

const accountAppeals = ref<CommunityAccountEnforcementAppeal[]>([])
const accountAppealLoading = ref(false)
const accountAppealReviewing = ref(false)
const showAccountAppealModal = ref(false)
const accountAppealTarget = ref<CommunityAccountEnforcementAppeal | null>(null)
const accountAppealDecision = ref<'UPHOLD' | 'MODIFY' | 'REVOKE'>('UPHOLD')
const accountAppealNote = ref('')
const accountAppealExpiresAt = ref<string | null>(null)
const accountAppealStatusMeta: Record<string, { label: string, type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  SUBMITTED: { label: '待初审', type: 'warning' }, PRIMARY_REVIEWED: { label: '待终审', type: 'info' }, FINALIZED: { label: '已处理', type: 'success' }, UPHELD: { label: '维持原处置', type: 'default' }, MODIFIED: { label: '已调整', type: 'info' }, REVOKED: { label: '已撤销', type: 'success' }
}
const accountAppealFiles = computed(() => (accountAppealTarget.value?.evidenceSnapshot?.match(/\d+/g) || []).filter((value, index, all) => all.indexOf(value) === index))

async function loadAccountAppeals() { accountAppealLoading.value = true; try { accountAppeals.value = await communityApi.accountAppeals() } catch (cause) { error.value = describeError(cause) } finally { accountAppealLoading.value = false } }
function openAccountAppealReview(row: CommunityAccountEnforcementAppeal) { accountAppealTarget.value = row; accountAppealDecision.value = 'UPHOLD'; accountAppealNote.value = ''; accountAppealExpiresAt.value = null; showAccountAppealModal.value = true }
function accountAppealFileUrl(appealId: string, fileId: string) { return `/admin-api/community/account-appeals/${appealId}/files/${fileId}/content` }
async function confirmAccountAppealReview() { const target = accountAppealTarget.value; if (!target || !accountAppealNote.value.trim()) return; accountAppealReviewing.value = true; try { const data = { decision: accountAppealDecision.value, reviewNote: accountAppealNote.value.trim(), ...(accountAppealDecision.value === 'MODIFY' ? { modifiedExpiresAt: accountAppealExpiresAt.value || undefined } : {}) }; if (target.status === 'PRIMARY_REVIEWED') await communityApi.finalReviewAccountAppeal(target.id, data); else await communityApi.primaryReviewAccountAppeal(target.id, data); message.success(target.status === 'PRIMARY_REVIEWED' ? '终审已完成' : '初审已完成'); showAccountAppealModal.value = false; await loadAccountAppeals() } catch (cause) { if (!(cause as { handled?: boolean })?.handled) message.error(describeError(cause)) } finally { accountAppealReviewing.value = false } }
const accountAppealColumns = [
  { title: '申诉 ID', key: 'id', width: 160 }, { title: '申诉人', key: 'appellantUserId', width: 150, render: (row: CommunityAccountEnforcementAppeal) => `用户 #${row.appellantUserId}` }, { title: '关联措施', key: 'caseId', width: 160 },
  { title: '申诉说明', key: 'statement', minWidth: 220, ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 120, render: (row: CommunityAccountEnforcementAppeal) => { const meta = accountAppealStatusMeta[row.status]; return h(NTag, { size: 'small', type: meta?.type || 'default' }, { default: () => meta?.label || row.status }) } },
  { title: '提交时间', key: 'createdAt', width: 170, render: (row: CommunityAccountEnforcementAppeal) => formatDateTime(row.createdAt) },
  { title: '操作', key: 'actions', width: 120, render: (row: CommunityAccountEnforcementAppeal) => canApprove && (row.status === 'SUBMITTED' || (row.status === 'PRIMARY_REVIEWED' && isSuperAdmin)) ? h(NButton, { size: 'small', type: 'primary', onClick: () => openAccountAppealReview(row) }, { default: () => row.status === 'PRIMARY_REVIEWED' ? '终审' : '初审' }) : '-' }
]

const appealStatusMeta: Record<string, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  PENDING: { label: '待复核', type: 'warning' },
  UPHELD: { label: '已维持', type: 'default' },
  REVOKED: { label: '已撤销', type: 'success' }
}

async function loadAppeals() {
  loading.value = true
  error.value = ''
  try {
    appeals.value = await communityApi.appeals()
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loading.value = false
  }
}

function reviewAppeal(row: CommunityAppeal, revoke: boolean) {
  dialog.warning({
    title: revoke ? '撤销原决定' : '维持原决定',
    content: revoke
      ? `确认撤销举报 #${row.reportId} 的原处理决定？撤销后内容状态将恢复，并记录本次复核。`
      : `确认维持举报 #${row.reportId} 的原处理决定？该申诉将标记为已处理。`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await communityApi.reviewAppeal(
          row.id,
          row.lockVersion,
          revoke,
          revoke ? '申诉复核后撤销原决定' : '申诉复核后维持原决定'
        )
        message.success(revoke ? '已撤销原决定' : '已维持原决定')
        await loadAppeals()
      } catch (cause) {
        const detail = describeError(cause)
        if (!(cause as { handled?: boolean })?.handled) message.error(detail)
      }
    }
  })
}

const appealColumns = [
  { title: '举报 ID', key: 'reportId', width: 140 },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row: CommunityAppeal) => {
      const meta = appealStatusMeta[row.status]
      return h(NTag, { size: 'small', type: meta?.type ?? 'default' }, { default: () => meta?.label ?? row.status })
    }
  },
  {
    title: '审核意见',
    key: 'reviewNote',
    minWidth: 220,
    ellipsis: { tooltip: true },
    render: (row: CommunityAppeal) => row.reviewNote || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row: CommunityAppeal) =>
      h('span', [
        h(
          NButton,
          {
            size: 'small',
            type: 'success',
            disabled: row.status !== 'PENDING',
            onClick: () => reviewAppeal(row, false)
          },
          { default: () => '维持' }
        ),
        h(
          NButton,
          {
            size: 'small',
            style: 'margin-left: 8px',
            disabled: row.status !== 'PENDING',
            onClick: () => reviewAppeal(row, true)
          },
          { default: () => '撤销' }
        )
      ])
  }
]

/* ---------- 申请审批 ---------- */
const applicationLoading = ref(false)
const rows = ref<CommunityAccountEnforcementCase[]>([])
const queryUserId = ref('')
const statusFilter = ref<string | null>(null)

const reviewing = ref(false)
const executing = ref(false)
const detailLoading = ref(false)

const showReviewModal = ref(false)
const reviewTarget = ref<CommunityAccountEnforcementCase | null>(null)
const reviewDecision = ref<'APPROVE' | 'REJECT' | 'RETURN_FOR_EVIDENCE'>('APPROVE')
const reviewNote = ref('')

const showExecuteModal = ref(false)
const executeTarget = ref<CommunityAccountEnforcementCase | null>(null)
const executeConfirmationText = ref('')
const executeConfirmation = ref('')

const showDetailModal = ref(false)
const detail = ref<CommunityAccountEnforcementCase | null>(null)
const reviews = ref<CommunityAccountEnforcementReview[]>([])
const showCommunityUserModal = ref(false)
const communityUserDetail = ref<CommunityUser | null>(null)
const communityUserDetailLoading = ref(false)

const measureLabels: Record<string, string> = {
  LONG_FREEZE: '长期冻结',
  DATA_CLEANUP: '数据清理',
  ACCOUNT_DELETE: '账号删除'
}

const statusMeta: Record<string, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  SUBMITTED: { label: '待初审', type: 'warning' },
  UNDER_REVIEW: { label: '待终审', type: 'info' },
  APPROVED: { label: '已批准', type: 'success' },
  ACTIVE: { label: '生效中', type: 'success' },
  APPEAL_WINDOW: { label: '申诉等待期', type: 'warning' },
  APPEALED: { label: '已申诉', type: 'info' },
  REJECTED: { label: '已驳回', type: 'error' },
  REVOKED: { label: '已撤销', type: 'default' },
  FINALIZED: { label: '已完结', type: 'default' }
}

const statusOptions = Object.entries(statusMeta).map(([value, meta]) => ({ value, label: meta.label }))

const reviewDecisionLabels: Record<string, string> = {
  APPROVE: '通过',
  REJECT: '驳回',
  RETURN_FOR_EVIDENCE: '退回补充证据'
}

const reviewStageLabels: Record<string, string> = {
  INITIAL: '初审',
  SECONDARY: '终审',
  FINAL: '终审'
}

/* ---------- 管理员信息 ---------- */
const adminMap = ref<Map<number, SysUser>>(new Map())
const showAdminModal = ref(false)
const adminDetail = ref<UserDetailResult | null>(null)
const adminDetailLoading = ref(false)
const roleNames = ref<Map<number, string>>(new Map())

async function loadAdmins() {
  try {
    const result = await userApi.page({ page: 1, pageSize: 100, userType: 'admin' })
    adminMap.value = new Map(result.list.filter(u => u.id != null).map(u => [u.id as number, u]))
  } catch {
    // 管理员列表加载失败不影响页面使用，退化为“管理员 ID”显示
  }
}

function adminLabel(id?: string): string {
  if (!id) return '-'
  const admin = adminMap.value.get(Number(id))
  return admin ? (admin.nickname || admin.username) : `管理员 ${id}`
}

async function openAdminDetail(id: string) {
  showAdminModal.value = true
  adminDetailLoading.value = true
  adminDetail.value = null
  try {
    const [detailResult, roles] = await Promise.all([userApi.detail(Number(id)), roleApi.list()])
    adminDetail.value = detailResult
    roleNames.value = new Map(roles.filter((role: SysRole) => role.id != null).map((role: SysRole) => [role.id as number, role.name]))
  } catch (cause) {
    message.error(describeError(cause))
  } finally {
    adminDetailLoading.value = false
  }
}

function adminIdentity(roleIds: number[]): string {
  return roleIds.map(id => roleNames.value.get(id) || `角色 ${id}`).join('、') || '-'
}

const cleanupScopeLabels: Record<string, string> = {
  PROFILE: '个人资料',
  ARTICLES: '文章',
  MOMENTS: '动态',
  COMMENTS: '评论',
  CHAT_MESSAGES: '私信',
  FILE_REFERENCES: '文件引用'
}

const reasonLabels: Record<string, string> = {
  POLICY_VIOLATION: '违反社区规范',
  SPAM_OR_BOT: '垃圾内容或机器人',
  HARASSMENT: '骚扰或攻击行为',
  COPYRIGHT: '侵权',
  FRAUD: '欺诈风险',
  SECURITY_RISK: '安全风险',
  OTHER: '其他原因'
}

const pagination = {
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 条`
}

const filteredRows = computed(() => {
  if (!statusFilter.value) return rows.value
  return rows.value.filter(row => row.status === statusFilter.value)
})

function measureLabel(value?: string): string {
  return value ? measureLabels[value] || value : '-'
}

function statusLabel(value: string): string {
  return statusMeta[value]?.label ?? value
}

function reviewDecisionLabel(value: string): string {
  return reviewDecisionLabels[value] || value
}

function communityUserLabel(id: string): string {
  if (communityUserDetail.value?.id === id && communityUserDetail.value.displayName) {
    return `${communityUserDetail.value.displayName}（${id}）`
  }
  return id
}

function reviewNoteLabel(review: CommunityAccountEnforcementReview): string {
  return review.reviewNote === '超管自审,应直接批准'
    ? '系统自动通过（超管自审）'
    : review.reviewNote || '-'
}

async function openCommunityUserDetail(id: string) {
  if (communityUserDetailLoading.value) return
  showCommunityUserModal.value = true
  communityUserDetailLoading.value = true
  communityUserDetail.value = null
  try {
    communityUserDetail.value = await communityApi.user(id)
  } catch (cause) {
    if (!(cause as { handled?: boolean })?.handled) message.error(describeError(cause))
  } finally {
    communityUserDetailLoading.value = false
  }
}

function cleanupScopeLabel(value?: string | null): string {
  if (!value) return '-'
  try {
    const scopes = JSON.parse(value) as string[]
    return scopes.map(scope => cleanupScopeLabels[scope] || scope).join('、') || '-'
  } catch {
    return value
  }
}

const evidenceParsed = computed(() => {
  const raw = detail.value?.evidenceSnapshot
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      return parsed as { description?: string; attachments?: Array<{ name: string; url: string }> }
    }
  } catch {
    // 兼容旧版纯文本证据
  }
  return null
})

function isImageFile(url: string): boolean {
  return /^\.(png|jpe?g|gif|webp|bmp|svg|avif|ico)$/i.test(url) || /^data:image\//.test(url)
}

async function loadApplications() {
  applicationLoading.value = true
  error.value = ''
  try {
    const userId = queryUserId.value.trim() || undefined
    rows.value = await communityApi.accountEnforcements(userId)
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    applicationLoading.value = false
  }
}

function resetQuery() {
  queryUserId.value = ''
  statusFilter.value = null
  void loadApplications()
}

function openReview(row: CommunityAccountEnforcementCase) {
  reviewTarget.value = row
  reviewDecision.value = 'APPROVE'
  reviewNote.value = ''
  showReviewModal.value = true
}

async function confirmReview() {
  if (!reviewTarget.value || !reviewNote.value.trim()) return
  reviewing.value = true
  error.value = ''
  try {
    await communityApi.reviewAccountEnforcement(reviewTarget.value.id, {
      decision: reviewDecision.value,
      reviewNote: reviewNote.value.trim()
    })
    message.success('审核完成')
    showReviewModal.value = false
    await loadApplications()
  } catch (cause) {
    const detail = describeError(cause)
    if (!(cause as { handled?: boolean })?.handled) message.error(detail)
  } finally {
    reviewing.value = false
  }
}

async function openExecute(row: CommunityAccountEnforcementCase) {
  error.value = ''
  try {
    const { confirmationText } = await communityApi.accountEnforcementConfirmationText(row.id)
    executeTarget.value = row
    executeConfirmationText.value = confirmationText
    executeConfirmation.value = ''
    showExecuteModal.value = true
  } catch (cause) {
    const detail = describeError(cause)
    if (!(cause as { handled?: boolean })?.handled) message.error(detail)
  }
}

async function confirmExecute() {
  if (!executeTarget.value) return
  executing.value = true
  error.value = ''
  try {
    await communityApi.executeAccountEnforcement(executeTarget.value.id, executeConfirmation.value.trim())
    message.success('执行成功')
    showExecuteModal.value = false
    await loadApplications()
  } catch (cause) {
    const detail = describeError(cause)
    if (!(cause as { handled?: boolean })?.handled) message.error(detail)
  } finally {
    executing.value = false
  }
}

async function showDetail(row: CommunityAccountEnforcementCase) {
  showDetailModal.value = true
  detailLoading.value = true
  detail.value = row
  reviews.value = []
  try {
    reviews.value = await communityApi.accountEnforcementReviews(row.id)
  } catch (cause) {
    message.error(describeError(cause))
  } finally {
    detailLoading.value = false
  }
}

function canReview(row: CommunityAccountEnforcementCase): boolean {
  if (!canApprove) return false
  if (row.status === 'SUBMITTED') return true
  if (row.status === 'UNDER_REVIEW') return isSuperAdmin
  return false
}

const applicationColumns = [
  {
    title: 'ID',
    key: 'id',
    width: 150,
    render: (row: CommunityAccountEnforcementCase) =>
      h(NButton, { text: true, type: 'primary', onClick: () => showDetail(row) }, { default: () => row.id })
  },
  { title: '用户', key: 'targetUserId', width: 100 },
  {
    title: '类型',
    key: 'measureType',
    width: 120,
    render: (row: CommunityAccountEnforcementCase) =>
      h(NTag, { size: 'small' }, { default: () => measureLabel(row.measureType) })
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row: CommunityAccountEnforcementCase) => {
      const meta = statusMeta[row.status]
      return h(NTag, { size: 'small', type: meta?.type ?? 'default' }, { default: () => meta?.label ?? row.status })
    }
  },
  { title: '原因', key: 'reasonCode', minWidth: 160, render: (row: CommunityAccountEnforcementCase) => reasonLabels[row.reasonCode] || row.reasonCode },
  { title: '时间', key: 'requestedAt', width: 170, render: (row: CommunityAccountEnforcementCase) => formatDateTime(row.requestedAt) },
  {
    title: '提交人',
    key: 'requestedByAdminId',
    width: 140,
    render: (row: CommunityAccountEnforcementCase) => {
      if (!row.requestedByAdminId) return '-'
      return h(
        NButton,
        { text: true, type: 'primary', onClick: () => openAdminDetail(row.requestedByAdminId as string) },
        { default: () => adminLabel(row.requestedByAdminId) }
      )
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 230,
    render: (row: CommunityAccountEnforcementCase) => {
      const buttons: ReturnType<typeof h>[] = []
      if (canReview(row)) {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'primary', style: 'margin-right: 8px', onClick: () => openReview(row) },
            { default: () => (row.status === 'UNDER_REVIEW' ? '终审' : '初审') }
          )
        )
      }
      if (row.status === 'APPROVED' && canExecute && isSuperAdmin) {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'error', style: 'margin-right: 8px', onClick: () => openExecute(row) },
            { default: () => '最终执行' }
          )
        )
      }
      buttons.push(
        h(NButton, { size: 'small', quaternary: true, onClick: () => showDetail(row) }, { default: () => '详情' })
      )
      return buttons.length > 0 ? h('span', buttons) : '-'
    }
  }
]

async function load() {
  if (tab.value === 'report') await loadAppeals()
  else if (tab.value === 'accountAppeals') await loadAccountAppeals()
  else await loadApplications()
}

onMounted(() => {
  if (!canApprove) tab.value = 'report'
  void loadAppeals()
  if (canApprove) void loadAccountAppeals()
  if (canApprove) void loadApplications()
  void loadAdmins()
})
</script>

<style scoped>
.community-page {
  padding: 24px;
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.page-eyebrow {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--n-text-color-3);
  margin-bottom: 8px;
}
.page-heading h1 {
  font-size: 24px;
  margin: 0 0 8px;
}
.page-heading p {
  margin: 0;
  color: var(--n-text-color-3);
}
.execute-target {
  margin: 0 0 12px;
  font-weight: 600;
}
.confirmation-hint {
  margin-bottom: 4px;
  font-weight: 600;
}
.confirmation-text {
  margin: 0;
  padding: 8px 12px;
  background: var(--n-color);
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
  user-select: all;
  cursor: text;
}
.evidence-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
}
.evidence-desc {
  margin: 0 0 8px;
  white-space: pre-wrap;
  word-break: break-all;
}
.evidence-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.evidence-file {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  overflow: hidden;
  text-decoration: none;
}
.evidence-thumb {
  width: 96px;
  height: 96px;
  object-fit: cover;
  display: block;
}
.evidence-file-name {
  max-width: 180px;
  padding: 6px 10px;
  font-size: 12px;
  color: var(--n-text-color-3);
  word-break: break-all;
}
</style>
