<template>
  <div class="community-page">
    <header class="page-heading">
      <div class="heading-content">
        <div class="page-eyebrow">
          <n-icon size="14" class="eyebrow-icon"><ShieldOutline /></n-icon>
          <span>ACCOUNT MANAGEMENT</span>
        </div>
        <h1>账号管理</h1>
        <p>账号处置、账号安全、冻结管理与操作记录的统一处理入口。</p>
      </div>
      <n-button v-if="canApply" type="primary" size="medium" class="apply-btn" @click="openSubmit">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        发起申请
      </n-button>
    </header>

    <n-tabs v-model:value="tab" type="line" class="main-tabs">
      <n-tab-pane name="sanction" tab="账号处置" />
      <n-tab-pane name="security" tab="账号安全" />
      <n-tab-pane name="freeze" tab="冻结管理" />
      <n-tab-pane name="records" tab="操作记录" />
    </n-tabs>

    <!-- 账号处置（社区违规处置） -->
    <n-tabs v-if="tab === 'sanction'" v-model:value="sanctionTab" type="segment" class="governance-sub-tabs">
      <n-tab-pane name="sanction-issue" tab="发起社区处置">
        <n-card class="form-card" :bordered="false">
          <template #header>
            <div class="card-header-title">
              <n-icon class="header-icon"><ShieldOutline /></n-icon>
              <span>社区能力限制</span>
            </div>
          </template>
          <n-alert type="info" :show-icon="true" class="notice-alert">
            社区处置不会冻结账号或禁止登录。需要冻结账号，请到本页“冻结管理”；需要长期冻结、数据清理或账号删除，请点击右上角“发起申请”。
          </n-alert>

          <n-form class="sanction-form-layout" label-placement="top">
            <div class="form-section-title">处置参数配置</div>
            <div class="form-grid-3">
              <n-form-item label="目标用户 ID" required>
                <n-select
                  v-model:value="sanctionUserId"
                  filterable
                  remote
                  clearable
                  :options="userOptions"
                  :loading="userSearching"
                  placeholder="搜索用户 ID、用户名、邮箱或昵称"
                  @search="onSanctionUserSearch"
                  @focus="onUserSelectFocus"
                />
              </n-form-item>
              <n-form-item label="处置类型" required>
                <n-select v-model:value="sanctionType" :options="sanctionTypes" />
              </n-form-item>
              <n-form-item label="处置原因" required>
                <n-select v-model:value="sanctionReasonCode" :options="sanctionReasonOptions" />
              </n-form-item>
              <n-form-item v-if="isTimed" label="到期时间" required>
                <n-date-picker v-model:value="sanctionExpiresAt" type="datetime" clearable style="width: 100%" />
              </n-form-item>
              <n-form-item label="关联举报 ID">
                <n-select v-model:value="sanctionSourceReportId" filterable remote clearable :options="reportOptions" :loading="reportSearching" placeholder="搜索举报 ID、目标或原因" @search="searchReports" />
              </n-form-item>
            </div>

            <div class="form-section-title" style="margin-top: 16px">案由备注说明</div>
            <n-form-item label="处理备注">
              <n-input
                v-model:value="sanctionReasonNote"
                type="textarea"
                :maxlength="1000"
                show-count
                :autosize="{ minRows: 3, maxRows: 5 }"
                placeholder="向用户说明社区能力限制的原因（可选）"
              />
            </n-form-item>

            <div class="form-actions-bar">
              <n-space>
                <n-button type="primary" :loading="issuing" @click="issue">
                  <template #icon><n-icon><ShieldOutline /></n-icon></template>
                  发起处置
                </n-button>
                <n-button secondary :loading="sanctionLoading" @click="loadSanctions">
                  <template #icon><n-icon><SearchOutline /></n-icon></template>
                  查询记录
                </n-button>
              </n-space>
            </div>
          </n-form>
        </n-card>
      </n-tab-pane>

      <n-tab-pane name="sanction-history" tab="处置记录">
        <n-card class="history-card" :bordered="false">
          <template #header>
            <div class="card-header-title">
              <n-icon class="header-icon"><DocumentTextOutline /></n-icon>
              <span>社区处置记录</span>
            </div>
          </template>
          <div class="filter-header">
            <n-space align="center">
              <n-select
                v-model:value="sanctionUserId"
                filterable
                remote
                clearable
                :options="userOptions"
                :loading="userSearching"
                placeholder="输入社区用户 ID 或关键词"
                style="width: 280px"
                @search="onSanctionUserSearch"
                @focus="onUserSelectFocus"
              />
              <n-button type="primary" secondary :loading="sanctionLoading" @click="loadSanctions">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                查询记录
              </n-button>
            </n-space>
          </div>
          <n-empty v-if="!sanctionLoading && sanctionRows.length === 0" description="暂无处置记录" style="padding: 40px 0" />
          <n-data-table
            v-else
            :data="sanctionRows"
            :columns="sanctionColumns"
            :loading="sanctionLoading"
            :row-key="(row: CommunitySanction) => row.id"
            :bordered="false"
            class="custom-table"
          />
        </n-card>
      </n-tab-pane>
    </n-tabs>

    <!-- 账号安全 / 冻结管理 -->
    <n-card v-else-if="tab !== 'records'" class="governance-card" :bordered="false">
      <template #header>
        <div class="card-header-title">
          <n-icon class="header-icon">
            <LockClosedOutline v-if="tab === 'security'" />
            <SnowOutline v-else />
          </n-icon>
          <span>{{ titles[tab] }}</span>
        </div>
      </template>

      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" class="split-form-layout">
        <div class="layout-grid">
          <!-- 左侧：目标与参数设置 + 执行按钮 -->
          <div class="grid-panel left-panel">
            <div class="panel-title">1. 目标账号与核心配置</div>

            <n-form-item label="目标用户" path="userId">
              <n-select
                v-model:value="form.userId"
                filterable
                remote
                clearable
                :options="userOptions"
                :loading="userSearching"
                placeholder="搜索用户 ID、用户名、邮箱或昵称"
                @search="searchUsers"
                @focus="onUserSelectFocus"
              />
            </n-form-item>

            <n-form-item label="标准原因" path="reasonCode">
              <n-select v-model:value="form.reasonCode" :options="reasonOptions" placeholder="选择标准原因" />
            </n-form-item>

            <n-form-item v-if="tab === 'freeze'" label="到期时间" path="expiresAt">
              <n-date-picker v-model:value="form.expiresAt" type="datetime" clearable style="width: 100%" />
            </n-form-item>

            <div class="action-card-box">
              <div class="panel-subtitle">操作执行</div>
              <div v-if="tab === 'security'" class="action-buttons-stack">
                <n-button type="primary" secondary :loading="submitting" class="action-btn" @click="security('logout')">
                  <template #icon><n-icon><LogOutOutline /></n-icon></template>
                  强制下线
                </n-button>
                <n-button type="warning" secondary :loading="submitting" class="action-btn" @click="security('reset')">
                  <template #icon><n-icon><KeyOutline /></n-icon></template>
                  强制重置登录凭证
                </n-button>
                <n-button type="primary" secondary :loading="submitting" class="action-btn" @click="security('unlock')">
                  <template #icon><n-icon><LockOpenOutline /></n-icon></template>
                  解除登录失败锁定
                </n-button>
              </div>
              <div v-else class="action-buttons-stack">
                <n-button type="primary" :loading="submitting" class="action-btn" @click="freeze('freeze')">
                  <template #icon><n-icon><SnowOutline /></n-icon></template>
                  冻结账号
                </n-button>
                <n-button type="warning" secondary :loading="submitting" class="action-btn" @click="freeze('extend')">
                  <template #icon><n-icon><TimeOutline /></n-icon></template>
                  延长冻结
                </n-button>
                <n-button type="info" secondary :loading="submitting" class="action-btn" @click="freeze('release')">
                  <template #icon><n-icon><LockOpenOutline /></n-icon></template>
                  提前解除冻结
                </n-button>
              </div>
            </div>
          </div>

          <!-- 右侧：案由与证据存证说明 -->
          <div class="grid-panel right-panel">
            <div class="panel-title">2. 操作案由与存证说明</div>

            <n-form-item label="用户可见理由" path="userVisibleReason">
              <n-input
                v-model:value="form.userVisibleReason"
                type="textarea"
                :maxlength="1000"
                show-count
                :autosize="{ minRows: 3, maxRows: 5 }"
                placeholder="将展示给用户的原因（必填）"
              />
            </n-form-item>

            <n-form-item label="内部说明" path="internalReason">
              <n-input
                v-model:value="form.internalReason"
                type="textarea"
                :maxlength="2000"
                show-count
                :autosize="{ minRows: 3, maxRows: 5 }"
                placeholder="仅内部可见的处理说明（必填）"
              />
            </n-form-item>

            <n-form-item label="证据说明" path="evidence">
              <div class="evidence-row">
                <n-input
                  v-model:value="form.evidence"
                  class="evidence-desc"
                  type="textarea"
                  :maxlength="2000"
                  show-count
                  :autosize="{ minRows: 3, maxRows: 5 }"
                  placeholder="证据描述（必填）"
                />
                <EvidenceUpload v-model:value="form.evidenceAttachments" class="evidence-upload" />
              </div>
            </n-form-item>
          </div>
        </div>
      </n-form>
    </n-card>

    <!-- 操作记录 -->
    <n-card v-else class="records-card" :bordered="false">
      <template #header>
        <div class="card-header-title">
          <n-icon class="header-icon"><DocumentTextOutline /></n-icon>
          <span>操作记录</span>
        </div>
      </template>

      <div class="filter-header">
        <n-space align="center">
          <n-input
            v-model:value="queryUserId"
            inputmode="numeric"
            placeholder="按用户 ID 筛选"
            clearable
            style="width: 260px"
            @keyup.enter="load"
          >
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
          <n-button type="primary" secondary :loading="loading" @click="load">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            查询
          </n-button>
          <n-button quaternary @click="resetQuery">
            <template #icon><n-icon><RefreshOutline /></n-icon></template>
            重置
          </n-button>
        </n-space>
      </div>

      <n-empty v-if="!loading && rows.length === 0" description="暂无操作记录" style="padding: 40px 0" />
      <n-data-table
        v-else
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="(row: CommunityAccountEnforcementCase) => row.id"
        :pagination="pagination"
        :bordered="false"
        class="custom-table"
      />
    </n-card>

    <n-alert v-if="error" type="error" closable class="error-alert" @close="error = ''">{{ error }}</n-alert>

    <!-- 发起申请弹窗 -->
    <n-modal
      v-model:show="showApplyModal"
      preset="card"
      title="发起申请"
      style="width: min(760px, calc(100vw - 32px))"
      :mask-closable="false"
      class="apply-modal"
    >
      <n-alert type="info" :show-icon="true" style="margin-bottom: 20px">
        申请需经两级审核（初审 + 超级管理员终审），批准后由超级管理员在“申诉中心 → 申请审批”执行。
      </n-alert>
      <n-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-placement="top" class="apply-form-grid">
        <n-form-item label="目标用户" path="targetUserId">
          <div class="target-user-select">
            <n-select
              v-model:value="applyForm.targetUserId"
              filterable
              remote
              clearable
              :options="applyUserOptions"
              :loading="applyUserSearching"
              placeholder="搜索用户 ID、用户名、邮箱或昵称"
              @search="searchApplyUsers"
              @focus="onUserSelectFocus"
            />
            <small>输入用户 ID、用户名、邮箱或昵称即可搜索并选择目标用户。</small>
          </div>
        </n-form-item>
        <n-form-item label="申请类型" path="measureType">
          <div class="target-user-select">
            <n-select v-model:value="applyForm.measureType" :options="measureOptions" />
            <small class="field-helper-spacer" aria-hidden="true">占位</small>
          </div>
        </n-form-item>
        <n-form-item v-if="applyForm.measureType === 'LONG_FREEZE'" label="到期时间" path="expiresAt">
          <n-date-picker v-model:value="applyForm.expiresAt" type="datetime" clearable style="width: 100%" />
        </n-form-item>
        <n-form-item v-if="applyForm.measureType === 'DATA_CLEANUP'" label="清理范围" path="cleanupScope" class="span-2">
          <n-select v-model:value="applyForm.cleanupScope" multiple :options="cleanupScopeOptions" placeholder="选择要清理的数据范围" />
        </n-form-item>
        <n-form-item label="标准原因" path="reasonCode">
          <div class="target-user-select">
            <n-select v-model:value="applyForm.reasonCode" :options="reasonOptions" placeholder="选择标准原因" />
            <small class="field-helper-spacer" aria-hidden="true">占位</small>
          </div>
        </n-form-item>
        <n-form-item label="关联举报 ID">
          <div class="target-user-select">
            <n-select v-model:value="applyForm.sourceReportId" filterable remote clearable :options="reportOptions" :loading="reportSearching" placeholder="输入已有举报 ID、目标或原因进行关联" @search="searchReports" />
            <small>仅在本申请由已有举报触发时关联；没有关联举报可留空。</small>
          </div>
        </n-form-item>
        <n-form-item label="用户可见理由" path="userVisibleReason" class="span-2">
          <n-input
            v-model:value="applyForm.userVisibleReason"
            type="textarea"
            :maxlength="1000"
            show-count
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="将展示给用户的原因（必填）"
          />
        </n-form-item>
        <n-form-item label="内部说明" path="internalReason" class="span-2">
          <n-input
            v-model:value="applyForm.internalReason"
            type="textarea"
            :maxlength="2000"
            show-count
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="仅内部可见的处理说明（必填）"
          />
        </n-form-item>
        <n-form-item label="证据说明" path="evidence" class="span-2">
          <div class="evidence-row">
            <n-input
              v-model:value="applyForm.evidence"
              class="evidence-desc"
              type="textarea"
              :maxlength="2000"
              show-count
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="证据描述（必填）"
            />
            <EvidenceUpload v-model:value="applyForm.evidenceAttachments" class="evidence-upload" />
          </div>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showApplyModal = false">取消</n-button>
          <n-button type="primary" :loading="applying" @click="submitApplication">提交申请</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 重置凭证结果 -->
    <n-modal v-model:show="showTemporaryPassword" preset="card" title="一次性临时密码" style="width: min(480px, calc(100vw - 32px))">
      <p class="temp-password-hint">请立即将以下临时密码告知用户，该密码仅显示一次：</p>
      <n-input-group>
        <n-input :value="temporaryPassword" readonly />
        <n-button type="primary" @click="copyTemporaryPassword">复制</n-button>
      </n-input-group>
    </n-modal>

    <n-modal v-model:show="showRecordDetail" preset="card" title="申请详情" style="width: min(680px, calc(100vw - 32px))">
      <n-descriptions v-if="recordDetail" bordered label-placement="left" :column="2">
        <n-descriptions-item label="申请 ID">{{ recordDetail.id }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ statusMeta[recordDetail.status]?.label || recordDetail.status }}</n-descriptions-item>
        <n-descriptions-item label="目标用户">{{ recordDetail.targetUserId }}</n-descriptions-item>
        <n-descriptions-item label="措施类型">{{ measureLabels[recordDetail.measureType] || recordDetail.measureType }}</n-descriptions-item>
        <n-descriptions-item label="标准原因">{{ reasonLabels[recordDetail.reasonCode] || recordDetail.reasonCode }}</n-descriptions-item>
        <n-descriptions-item label="申请时间">{{ formatDateTime(recordDetail.requestedAt) }}</n-descriptions-item>
        <n-descriptions-item label="用户可见理由" :span="2">{{ recordDetail.userVisibleReason }}</n-descriptions-item>
        <n-descriptions-item label="内部说明" :span="2">{{ recordDetail.internalReason }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>

    <n-modal v-model:show="showCommunityUserDetail" preset="card" title="用户资料" style="width: min(560px, calc(100vw - 32px))">
      <n-spin v-if="communityUserDetailLoading">正在加载用户资料</n-spin>
      <n-descriptions v-else-if="communityUserDetail" bordered label-placement="left" :column="2">
        <n-descriptions-item label="用户 ID">{{ communityUserDetail.id }}</n-descriptions-item>
        <n-descriptions-item label="昵称">{{ communityUserDetail.displayName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="用户名">{{ communityUserDetail.username }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ statusLabel(communityUserDetail.status) }}</n-descriptions-item>
        <n-descriptions-item label="邮箱">{{ communityUserDetail.email || '-' }}</n-descriptions-item>
        <n-descriptions-item label="认证">{{ statusLabel(communityUserDetail.verificationStatus) }}</n-descriptions-item>
        <n-descriptions-item label="个人博客">{{ communityUserDetail.personalBlogName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="最近登录">{{ formatDateTime(communityUserDetail.lastLoginAt) }}</n-descriptions-item>
        <n-descriptions-item label="注册时间" :span="2">{{ formatDateTime(communityUserDetail.createdAt) }}</n-descriptions-item>
      </n-descriptions>
      <n-empty v-else description="未找到用户资料" />
    </n-modal>

    <n-modal v-model:show="showAdminDetail" preset="card" title="管理员资料" style="width: min(560px, calc(100vw - 32px))">
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
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NTag, useDialog, useMessage, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import {
  AddOutline,
  ShieldOutline,
  LockClosedOutline,
  KeyOutline,
  LockOpenOutline,
  SnowOutline,
  TimeOutline,
  LogOutOutline,
  SearchOutline,
  RefreshOutline,
  DocumentTextOutline
} from '@vicons/ionicons5'
import { useRoute } from 'vue-router'
import { communityApi, type CommunityAccountEnforcementCase, type CommunityReport, type CommunitySanction, type CommunityUser } from '@/api/community'
import { roleApi, userApi, type SysRole, type UserDetailResult } from '@/api/system'
import { formatDateTime, statusLabel } from '@/utils/community'
import { useUserStore } from '@/stores/user'
import EvidenceUpload, { type EvidenceAttachment } from '@/components/EvidenceUpload.vue'

const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

const canApply = userStore.hasPermission('community:account:apply')

/* ---------- 通用 ---------- */
const error = ref('')
const describeError = (cause: unknown) => (cause instanceof Error ? cause.message : '请求失败，请稍后重试')

/* ---------- 账号处置（社区处置） ---------- */
const timedTypes = new Set(['RATE_LIMIT', 'COMMENT_BAN', 'MOMENT_BAN', 'SUBMISSION_BAN', 'PUBLISH_SUSPEND', 'MESSAGE_BAN'])
const sanctionTypes = [
  ['WARNING', '警告'], ['RATE_LIMIT', '限制互动'], ['COMMENT_BAN', '禁止评论'], ['MOMENT_BAN', '禁止发布动态'],
  ['SUBMISSION_BAN', '禁止投稿'], ['PUBLISH_SUSPEND', '禁止发布文章'], ['MESSAGE_BAN', '禁止私信']
].map(([value, label]) => ({ value, label }))
const sanctionReasonOptions = [
  ['POLICY_VIOLATION', '违反社区规范'], ['SPAM_OR_BOT', '垃圾内容或机器人'], ['HARASSMENT', '骚扰或攻击行为'],
  ['COPYRIGHT', '侵权'], ['FRAUD', '欺诈风险'], ['OTHER', '其他原因']
].map(([value, label]) => ({ value, label }))
// LOGIN_SUSPEND 由“冻结管理”内部产生，不在社区处置下拉中提供，仅用于历史记录展示映射
const sanctionLabels: Record<string, string> = { ...Object.fromEntries(sanctionTypes.map(({ value, label }) => [value, label])), LOGIN_SUSPEND: '禁止登录' }
const sanctionReasonLabels = Object.fromEntries(sanctionReasonOptions.map(({ value, label }) => [value, label]))
const sanctionStatusMeta: Record<string, { label: string; type: 'default' | 'info' | 'success' | 'warning' | 'error' }> = {
  ACTIVE: { label: '生效中', type: 'warning' },
  REVOKED: { label: '已撤销', type: 'default' },
  EXPIRED: { label: '已到期', type: 'info' }
}
const sanctionTab = ref('sanction-issue')
const sanctionUserId = ref('')
const sanctionType = ref('WARNING')
const sanctionReasonCode = ref('POLICY_VIOLATION')
const sanctionReasonNote = ref('')
const sanctionSourceReportId = ref('')
const sanctionExpiresAt = ref<number | null>(null)
const sanctionRows = ref<CommunitySanction[]>([])
const sanctionLoading = ref(false)
const issuing = ref(false)
const isTimed = computed(() => timedTypes.has(sanctionType.value))
const formatSanctionDate = (value: number) => new Date(value).toISOString().slice(0, 19).replace('T', ' ')

function onSanctionUserSearch(value: string) {
  sanctionUserId.value = value
  if (value.trim()) {
    form.userId = value.trim()
  }
  searchUsers(value)
}

async function loadSanctions() {
  const userId = (sanctionUserId.value ?? '').trim()
  if (!userId) {
    error.value = '请先输入用户 ID'
    return
  }
  sanctionLoading.value = true
  error.value = ''
  try {
    sanctionRows.value = await communityApi.sanctions(userId)
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    sanctionLoading.value = false
  }
}

async function issue() {
  const userId = (sanctionUserId.value ?? '').trim()
  if (!userId) {
    error.value = '请先输入用户 ID'
    return
  }
  if (isTimed.value && !sanctionExpiresAt.value) {
    error.value = '限时处置必须填写到期时间'
    return
  }
  issuing.value = true
  error.value = ''
  try {
    await communityApi.issueSanction({
      targetUserId: userId,
      sanctionType: sanctionType.value,
      reasonCode: sanctionReasonCode.value,
      reasonNote: sanctionReasonNote.value.trim() || undefined,
      sourceReportId: (sanctionSourceReportId.value ?? '').trim() || undefined,
      expiresAt: isTimed.value && sanctionExpiresAt.value ? formatSanctionDate(sanctionExpiresAt.value) : undefined
    })
    message.success('社区处置已发起')
    sanctionExpiresAt.value = null
    await loadSanctions()
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    issuing.value = false
  }
}

function revokeSanction(row: CommunitySanction) {
  dialog.warning({
    title: '撤销社区处置',
    content: `确认撤销 ${sanctionLabels[row.type] || row.type} 吗？`,
    positiveText: '撤销',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await communityApi.revokeSanction(row.id, '运营撤销社区处置')
        message.success('处置已撤销')
        await loadSanctions()
      } catch (cause) {
        error.value = describeError(cause)
      }
    }
  })
}

const sanctionColumns: DataTableColumns<CommunitySanction> = [
  { title: '处置类型', key: 'type', minWidth: 150, render: row => sanctionLabels[row.type] || row.type },
  { title: '原因', key: 'reasonCode', minWidth: 160, render: row => sanctionReasonLabels[row.reasonCode] || row.reasonCode },
  { title: '说明', key: 'reasonNote', minWidth: 220, render: row => row.reasonNote || '-' },
  {
    title: '状态', key: 'status', minWidth: 100,
    render: row => {
      const meta = sanctionStatusMeta[row.status]
      return h(NTag, { size: 'small', type: meta?.type ?? 'default' }, { default: () => meta?.label ?? row.status })
    }
  },
  { title: '到期时间', key: 'expiresAt', minWidth: 180, render: row => row.expiresAt ? row.expiresAt.replace('T', ' ') : '永久' },
  { title: '操作', key: 'actions', width: 100, render: row => h(NButton, { size: 'small', disabled: row.status !== 'ACTIVE', onClick: () => revokeSanction(row) }, { default: () => '撤销' }) }
]

/* ---------- 账号安全 / 冻结管理 ---------- */
const tab = ref('security')
const queryUserId = ref('')
const loading = ref(false)
const submitting = ref(false)
const rows = ref<CommunityAccountEnforcementCase[]>([])
const formRef = ref<FormInst | null>(null)
const showTemporaryPassword = ref(false)
const temporaryPassword = ref('')

const titles: Record<string, string> = {
  security: '账号安全',
  freeze: '冻结管理'
}

const form = reactive({
  userId: '',
  reasonCode: 'SECURITY_RISK',
  userVisibleReason: '',
  internalReason: '',
  evidence: '',
  evidenceAttachments: [] as EvidenceAttachment[],
  expiresAt: null as number | null
})

const rules: FormRules = {
  userId: {
    required: true,
    pattern: /^\d+$/,
    message: '请输入纯数字用户 ID',
    trigger: ['input', 'blur']
  },
  reasonCode: {
    required: true,
    message: '请选择或输入标准原因',
    trigger: ['input', 'blur']
  },
  userVisibleReason: {
    required: true,
    message: '用户可见理由为必填',
    trigger: ['input', 'blur']
  },
  internalReason: {
    required: true,
    message: '内部说明为必填',
    trigger: ['input', 'blur']
  },
  evidence: {
    required: true,
    message: '证据说明为必填',
    trigger: ['input', 'blur']
  }
}

const reasonOptions = [
  { value: 'POLICY_VIOLATION', label: '违反社区规范' },
  { value: 'SPAM_OR_BOT', label: '垃圾内容或机器人' },
  { value: 'HARASSMENT', label: '骚扰或攻击行为' },
  { value: 'COPYRIGHT', label: '侵权' },
  { value: 'FRAUD', label: '欺诈风险' },
  { value: 'SECURITY_RISK', label: '安全风险' },
  { value: 'OTHER', label: '其他原因' }
]
const reasonLabels = Object.fromEntries(reasonOptions.map(({ value, label }) => [value, label]))

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

const pagination = {
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 条`
}

function formatExpiresAt(value: number | null): string | undefined {
  if (!value) return undefined
  return new Date(value).toISOString().slice(0, 19)
}

function payload() {
  return {
    reasonCode: form.reasonCode.trim(),
    userVisibleReason: form.userVisibleReason.trim(),
    internalReason: form.internalReason.trim(),
    evidenceSnapshot: JSON.stringify({ description: form.evidence.trim(), attachments: form.evidenceAttachments }),
    expiresAt: formatExpiresAt(form.expiresAt)
  }
}

async function validate(): Promise<boolean> {
  if (!formRef.value) return true
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

function confirmAction(settings: {
  title: string
  content: string
  positiveText: string
  positiveType?: 'warning' | 'error'
  action: () => Promise<unknown>
}) {
  const open = settings.positiveType === 'error' ? dialog.error : dialog.warning
  open({
    title: settings.title,
    content: settings.content,
    positiveText: settings.positiveText,
    negativeText: '取消',
    onPositiveClick: async () => {
      await settings.action()
    }
  })
}

async function run(action: () => Promise<unknown>, successText: string) {
  submitting.value = true
  error.value = ''
  try {
    await action()
    message.success(successText)
    await load()
  } catch (cause) {
    const detail = describeError(cause)
    error.value = detail
    if (!(cause as { handled?: boolean })?.handled) message.error(detail)
  } finally {
    submitting.value = false
  }
}

function requireExpiresAt(): boolean {
  if (!form.expiresAt) {
    message.warning('请先选择到期时间')
    return false
  }
  if (form.expiresAt <= Date.now()) {
    message.warning('到期时间必须晚于当前时间')
    return false
  }
  return true
}

async function security(action: 'logout' | 'reset' | 'unlock') {
  if (!(await validate())) return
  const userId = (form.userId ?? '').trim()
  const plans: Record<string, { title: string; content: string; positiveText: string; positiveType?: 'warning' | 'error'; call: () => Promise<unknown> }> = {
    logout: {
      title: '确认强制下线',
      content: `将强制下线用户 #${userId} 的全部会话，并记录本次操作。`,
      positiveText: '强制下线',
      call: async () => communityApi.forceLogout(userId, payload())
    },
    reset: {
      title: '确认重置登录凭证',
      content: `将重置用户 #${userId} 的登录凭证，并生成一次性临时密码。`,
      positiveText: '重置凭证',
      positiveType: 'warning',
      call: async () => {
        const result = await communityApi.forcePasswordReset(userId, payload())
        temporaryPassword.value = result.temporaryPassword
        showTemporaryPassword.value = true
      }
    },
    unlock: {
      title: '确认解除锁定',
      content: `将解除用户 #${userId} 因登录失败产生的锁定，并记录本次操作。`,
      positiveText: '解除锁定',
      call: async () => communityApi.unlockAccount(userId, payload())
    }
  }
  const plan = plans[action]
  confirmAction({
    title: plan.title,
    content: plan.content,
    positiveText: plan.positiveText,
    positiveType: plan.positiveType,
    action: () => run(plan.call, action === 'reset' ? '登录凭证已重置' : '操作成功')
  })
}

async function freeze(action: 'freeze' | 'extend' | 'release') {
  if (!(await validate())) return
  if (action !== 'release' && !requireExpiresAt()) return
  const userId = (form.userId ?? '').trim()
  const expiresText = form.expiresAt ? formatDateTime(new Date(form.expiresAt).toISOString()) : ''
  const plans: Record<string, { title: string; content: string; positiveText: string; call: () => Promise<unknown> }> = {
    freeze: {
      title: '确认冻结账号',
      content: `将冻结用户 #${userId} 的账号，冻结期间无法登录，到期时间：${expiresText}。`,
      positiveText: '冻结',
      call: async () => communityApi.freezeAccount(userId, payload())
    },
    extend: {
      title: '确认延长冻结',
      content: `将用户 #${userId} 的冻结期限延长至 ${expiresText}。`,
      positiveText: '延长',
      call: async () => communityApi.extendAccountFreeze(userId, payload())
    },
    release: {
      title: '确认解除冻结',
      content: `将提前解除用户 #${userId} 的账号冻结，恢复其正常登录。`,
      positiveText: '解除冻结',
      call: async () => communityApi.releaseAccountFreeze(userId, payload())
    }
  }
  const plan = plans[action]
  confirmAction({
    title: plan.title,
    content: plan.content,
    positiveText: plan.positiveText,
    action: () => run(plan.call, '操作成功')
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const userId = (queryUserId.value ?? '').trim() || (form.userId ?? '').trim() || undefined
    rows.value = await communityApi.accountEnforcements(userId)
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryUserId.value = ''
  void load()
}

async function copyTemporaryPassword() {
  try {
    await navigator.clipboard.writeText(temporaryPassword.value)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

const columns: DataTableColumns<CommunityAccountEnforcementCase> = [
  {
    title: 'ID',
    key: 'id',
    width: 150,
    render: row => h(NButton, { text: true, type: 'primary', onClick: () => openRecordDetail(row) }, { default: () => row.id })
  },
  {
    title: '用户',
    key: 'targetUserId',
    width: 100,
    render: row => h(NButton, { text: true, type: 'primary', onClick: () => openCommunityUserDetail(row.targetUserId) }, { default: () => row.targetUserId })
  },
  {
    title: '操作',
    key: 'measureType',
    width: 140,
    render: row => h(NTag, { size: 'small' }, { default: () => measureLabels[row.measureType] || row.measureType })
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: row => {
      const meta = statusMeta[row.status]
      return h(NTag, { size: 'small', type: meta?.type ?? 'default' }, { default: () => meta?.label ?? row.status })
    }
  },
  { title: '时间', key: 'requestedAt', width: 170, render: row => formatDateTime(row.requestedAt) },
  {
    title: '申请人',
    key: 'requestedByAdminId',
    width: 110,
    render: row => row.requestedByAdminId
      ? h(NButton, { text: true, type: 'primary', onClick: () => openAdminDetail(row.requestedByAdminId as string) }, { default: () => `管理员 ${row.requestedByAdminId}` })
      : '-'
  }
]

const showRecordDetail = ref(false)
const recordDetail = ref<CommunityAccountEnforcementCase | null>(null)
const showCommunityUserDetail = ref(false)
const communityUserDetail = ref<CommunityUser | null>(null)
const communityUserDetailLoading = ref(false)
const showAdminDetail = ref(false)
const adminDetail = ref<UserDetailResult | null>(null)
const adminDetailLoading = ref(false)
const roleNames = ref<Map<number, string>>(new Map())


function openRecordDetail(row: CommunityAccountEnforcementCase) {
  recordDetail.value = row
  showRecordDetail.value = true
}

async function openCommunityUserDetail(id: string) {
  if (communityUserDetailLoading.value) return
  showCommunityUserDetail.value = true
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

async function openAdminDetail(id: string) {
  if (adminDetailLoading.value) return
  showAdminDetail.value = true
  adminDetailLoading.value = true
  adminDetail.value = null
  try {
    const [detailResult, roles] = await Promise.all([userApi.detail(Number(id)), roleApi.list()])
    adminDetail.value = detailResult
    roleNames.value = new Map(roles.filter((role: SysRole) => role.id != null).map((role: SysRole) => [role.id as number, role.name]))
  } catch (cause) {
    if (!(cause as { handled?: boolean })?.handled) message.error(describeError(cause))
  } finally {
    adminDetailLoading.value = false
  }
}

function adminIdentity(roleIds: number[]): string {
  return roleIds.map(id => roleNames.value.get(id) || `角色 ${id}`).join('、') || '-'
}

/* ---------- 目标用户联想 ---------- */
type UserOption = { label: string; value: string; disabled?: boolean }

const userOptions = ref<UserOption[]>([])
const userSearching = ref(false)
const applyUserOptions = ref<UserOption[]>([])
const applyUserSearching = ref(false)
let userSearchTimer: ReturnType<typeof setTimeout> | undefined
let applyUserSearchTimer: ReturnType<typeof setTimeout> | undefined

function userOptionLabel(user: CommunityUser, markFrozen = false): string {
  const name = user.displayName || user.username
  const frozen = user.status === 'FROZEN'
  return `${name}（@${user.username}） #${user.id}${user.email ? ` · ${user.email}` : ''}${markFrozen && frozen ? ' · 冻结中' : ''}`
}

function userOption(user: CommunityUser, disableFrozen = false): UserOption {
  return { label: userOptionLabel(user, disableFrozen), value: user.id, disabled: disableFrozen && user.status === 'FROZEN' }
}

async function lookupUsers(keyword: string, disableFrozen = false): Promise<UserOption[]> {
  const result = await communityApi.users({ keyword, pageNum: 1, pageSize: 10 })
  return result.list.map(user => userOption(user, disableFrozen))
}

// 按字母/数字排序（显示名优先）
function sortUserOptions(options: UserOption[]): UserOption[] {
  return options.sort((a, b) => a.label.localeCompare(b.label, 'zh-Hans-CN', { numeric: true }))
}

// 聚焦时默认加载现有用户列表（无需先输入关键词）
async function loadDefaultUsers() {
  if (userOptions.value.length > 0 && applyUserOptions.value.length > 0) return
  try {
    const result = await communityApi.users({ pageNum: 1, pageSize: 50 })
    if (userOptions.value.length === 0) userOptions.value = sortUserOptions(result.list.map(user => userOption(user)))
    if (applyUserOptions.value.length === 0) applyUserOptions.value = sortUserOptions(result.list.map(user => userOption(user, true)))
  } catch {
    // 忽略默认加载失败，输入关键词仍可搜索
  }
}

function onUserSelectFocus() {
  void loadDefaultUsers()
}

function searchUsers(value: string) {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  const keyword = value.trim()
  if (!keyword) {
    void loadDefaultUsers()
    return
  }
  userSearchTimer = setTimeout(async () => {
    userSearching.value = true
    try {
      userOptions.value = await lookupUsers(keyword)
    } catch {
      userOptions.value = []
    } finally {
      userSearching.value = false
    }
  }, 300)
}

function searchApplyUsers(value: string) {
  if (applyUserSearchTimer) clearTimeout(applyUserSearchTimer)
  const keyword = value.trim()
  if (!keyword) {
    void loadDefaultUsers()
    return
  }
  applyUserSearchTimer = setTimeout(async () => {
    applyUserSearching.value = true
    try {
      applyUserOptions.value = await lookupUsers(keyword, true)
    } catch {
      applyUserOptions.value = []
    } finally {
      applyUserSearching.value = false
    }
  }, 300)
}

/* ---------- 关联举报联想 ---------- */
const reportOptions = ref<Array<{ label: string; value: string }>>([])
const reportSearching = ref(false)
let reportSearchTimer: ReturnType<typeof setTimeout> | undefined

function reportOption(report: CommunityReport) {
  return {
    label: `举报 #${report.id} · ${report.targetType} #${report.targetId}${report.reasonCode ? ` · ${report.reasonCode}` : ''}`,
    value: report.id
  }
}

function searchReports(value: string) {
  if (reportSearchTimer) clearTimeout(reportSearchTimer)
  const keyword = value.trim()
  if (!keyword) {
    reportOptions.value = []
    return
  }
  reportSearchTimer = setTimeout(async () => {
    reportSearching.value = true
    try {
      const reports = await communityApi.reportSearch(keyword)
      reportOptions.value = reports.map(reportOption)
    } catch {
      reportOptions.value = []
    } finally {
      reportSearching.value = false
    }
  }, 300)
}

/* ---------- 发起申请 ---------- */
const showApplyModal = ref(false)
const applying = ref(false)
const applyFormRef = ref<FormInst | null>(null)

const applyForm = reactive({
  targetUserId: '',
  measureType: 'ACCOUNT_DELETE',
  reasonCode: 'POLICY_VIOLATION',
  userVisibleReason: '',
  internalReason: '',
  evidence: '',
  evidenceAttachments: [] as EvidenceAttachment[],
  cleanupScope: [] as string[],
  sourceReportId: '',
  expiresAt: null as number | null
})

const applyRules: FormRules = {
  targetUserId: {
    required: true,
    pattern: /^\d+$/,
    message: '请输入纯数字用户 ID',
    trigger: ['input', 'blur']
  },
  reasonCode: {
    required: true,
    message: '请选择或输入标准原因',
    trigger: ['input', 'blur']
  },
  userVisibleReason: {
    required: true,
    message: '用户可见理由为必填',
    trigger: ['input', 'blur']
  },
  internalReason: {
    required: true,
    message: '内部说明为必填',
    trigger: ['input', 'blur']
  },
  evidence: {
    required: true,
    message: '证据说明为必填',
    trigger: ['input', 'blur']
  },
  expiresAt: {
    required: true,
    type: 'number',
    message: '长期冻结必须选择到期时间',
    trigger: ['change', 'blur']
  },
  cleanupScope: {
    required: true,
    type: 'array',
    message: '请选择至少一项清理范围',
    trigger: ['change', 'blur']
  }
}

const measureOptions = [
  { value: 'LONG_FREEZE', label: '长期冻结' },
  { value: 'DATA_CLEANUP', label: '数据清理' },
  { value: 'ACCOUNT_DELETE', label: '账号删除' }
]

const cleanupScopeOptions = [
  { value: 'PROFILE', label: '个人资料' },
  { value: 'ARTICLES', label: '文章' },
  { value: 'MOMENTS', label: '动态' },
  { value: 'COMMENTS', label: '评论' },
  { value: 'CHAT_MESSAGES', label: '私信' },
  { value: 'FILE_REFERENCES', label: '文件引用' }
]

function resetApplyForm() {
  applyForm.targetUserId = form.userId || queryUserId.value
  applyForm.measureType = 'ACCOUNT_DELETE'
  applyForm.reasonCode = 'POLICY_VIOLATION'
  applyForm.userVisibleReason = ''
  applyForm.internalReason = ''
  applyForm.evidence = ''
  applyForm.cleanupScope = []
  applyForm.sourceReportId = ''
  applyForm.expiresAt = null
}

function openSubmit() {
  resetApplyForm()
  void loadDefaultUsers()
  showApplyModal.value = true
}

async function submitApplication() {
  if (!applyFormRef.value) return
  try {
    await applyFormRef.value.validate()
  } catch {
    return
  }
  if (applyForm.measureType === 'LONG_FREEZE' && (!applyForm.expiresAt || applyForm.expiresAt <= Date.now())) {
    message.warning('长期冻结的到期时间必须晚于当前时间')
    return
  }
  applying.value = true
  error.value = ''
  try {
    await communityApi.submitAccountEnforcement({
      targetUserId: (applyForm.targetUserId ?? '').trim(),
      measureType: applyForm.measureType,
      reasonCode: applyForm.reasonCode.trim(),
      userVisibleReason: applyForm.userVisibleReason.trim(),
      internalReason: applyForm.internalReason.trim(),
      evidenceSnapshot: JSON.stringify({ description: applyForm.evidence.trim(), attachments: applyForm.evidenceAttachments }),
      cleanupScope: applyForm.measureType === 'DATA_CLEANUP' ? JSON.stringify(applyForm.cleanupScope) : undefined,
      sourceReportId: (applyForm.sourceReportId ?? '').trim() || undefined,
      expiresAt: applyForm.measureType === 'LONG_FREEZE' ? formatExpiresAt(applyForm.expiresAt) : undefined
    })
    message.success('申请已提交，等待审核')
    showApplyModal.value = false
  } catch (cause) {
    const detail = describeError(cause)
    if (!(cause as { handled?: boolean })?.handled) message.error(detail)
  } finally {
    applying.value = false
  }
}

onMounted(() => {
  const value = route.query.userId
  if (typeof value === 'string' && /^\d+$/.test(value)) {
    form.userId = value
    queryUserId.value = value
    sanctionUserId.value = value
  }
  void load()
})
</script>

<style scoped>
.community-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.heading-content h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 4px 0 8px;
  color: var(--n-text-color-1, #111827);
  letter-spacing: -0.5px;
}

.heading-content p {
  margin: 0;
  color: var(--n-text-color-2, #374151);
  font-size: 14px;
  line-height: 1.5;
}

.page-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: var(--n-primary-color, #2563eb);
  text-transform: uppercase;
}

.eyebrow-icon {
  display: inline-flex;
  align-items: center;
}

.apply-btn {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.main-tabs {
  margin-bottom: 20px;
}

.governance-sub-tabs {
  margin-bottom: 24px;
}

.form-card,
.history-card,
.governance-card,
.records-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  background-color: var(--n-card-color, #ffffff);
  border: 1px solid var(--n-border-color, #e5e7eb);
  transition: box-shadow 0.2s ease;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--n-text-color-1, #111827);
}

.header-icon {
  font-size: 18px;
  color: var(--n-primary-color);
}

.notice-alert {
  margin-bottom: 24px;
  border-radius: 8px;
}

.form-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--n-text-color-1, #111827);
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px dashed var(--n-border-color, #cbd5e1);
}

.form-grid-3 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0 20px;
}

.form-actions-bar {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--n-border-color, #e5e7eb);
  display: flex;
  justify-content: flex-end;
}

.filter-header {
  margin-bottom: 20px;
}

/* 账号安全与冻结管理 - 双栏/分块布局 */
.split-form-layout {
  width: 100%;
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(380px, 1.2fr);
  gap: 24px;
}

@media (max-width: 900px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

.grid-panel {
  background-color: var(--n-color-embedded, #f8fafc);
  border: 1px solid var(--n-border-color, #e2e8f0);
  border-radius: 10px;
  padding: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--n-text-color-1, #0f172a);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--n-border-color, #e2e8f0);
}

.panel-subtitle {
  font-size: 13px;
  font-weight: 700;
  color: var(--n-text-color-1, #1e293b);
  margin-bottom: 12px;
}

.action-card-box {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed var(--n-border-color, #cbd5e1);
}

.action-buttons-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  justify-content: flex-start;
  height: 38px;
  font-weight: 600;
}

/* 深度选择器强化字体颜色与对比度 */
:deep(.n-form-item-label__text) {
  font-weight: 600 !important;
  color: var(--n-text-color-1, #1e293b) !important;
}

:deep(.n-form-item-label) {
  color: var(--n-text-color-1, #1e293b) !important;
}

:deep(.n-tabs-tab__label) {
  font-weight: 600;
  color: var(--n-text-color-1, #1e293b);
}

:deep(.n-tabs-tab--active .n-tabs-tab__label) {
  color: var(--n-primary-color) !important;
}

:deep(.n-input__placeholder),
:deep(.n-base-selection-placeholder) {
  color: var(--n-text-color-3, #94a3b8);
}

.apply-form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0 20px;
}

.apply-form-grid :deep(.span-2) {
  grid-column: 1 / -1;
}

.target-user-select {
  width: 100%;
}

.target-user-select small {
  display: block;
  min-height: 18px;
  margin-top: 6px;
  color: var(--n-text-color-3, #64748b);
  font-size: 12px;
  line-height: 1.5;
}

.target-user-select .field-helper-spacer {
  visibility: hidden;
}

.evidence-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
  width: 100%;
}
.evidence-desc {
  flex: 1 1 auto;
}
.evidence-upload {
  flex: 0 0 300px;
}

.temp-password-hint {
  margin: 0 0 12px;
  color: var(--n-text-color-2, #334155);
  font-size: 14px;
}

.error-alert {
  margin-top: 20px;
  border-radius: 8px;
}

.custom-table {
  border-radius: 8px;
  overflow: hidden;
}
</style>
