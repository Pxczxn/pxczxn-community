<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">MOMENT GOVERNANCE</div>
        <h1>动态治理</h1>
        <p>检索所有公开与受限动态，处理待审、下架和恢复，并核对传播与互动计数。</p>
      </div>
      <n-button :loading="loading" @click="loadData">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新
      </n-button>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="正文或链接"
            @keyup.enter="search"
          />
        </n-form-item>
        <n-form-item label="状态">
          <n-select
            v-model:value="filters.status"
            :options="statusOptions"
            clearable
            placeholder="全部状态"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="可见性">
          <n-select
            v-model:value="filters.visibility"
            :options="visibilityOptions"
            clearable
            placeholder="全部范围"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="类型">
          <n-select
            v-model:value="filters.momentType"
            :options="momentTypeOptions"
            clearable
            filterable
            placeholder="全部类型"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="博客 ID">
          <n-input v-model:value="filters.blogId" clearable placeholder="博客 ID" />
        </n-form-item>
        <n-form-item label="发布者 ID">
          <n-input v-model:value="filters.actorUserId" clearable placeholder="用户 ID" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" @click="search">
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              查询
            </n-button>
            <n-button @click="reset">重置</n-button>
          </n-space>
        </n-form-item>
      </n-form>

      <div v-if="userStore.hasPermission('community:moment:batch')" class="governance-toolbar">
        <div>
          <strong>批量治理</strong>
          <span>已选择 {{ checkedRowKeys.length }} 条；批次内任一锁冲突会整体回滚。</span>
        </div>
        <n-space>
          <n-select
            v-model:value="batchAction"
            :options="availableActionOptions"
            placeholder="选择动作"
            style="width: 160px"
          />
          <n-button
            type="primary"
            :disabled="!batchAction || checkedRowKeys.length === 0"
            @click="openBatchAction"
          >
            执行批量治理
          </n-button>
        </n-space>
      </div>

      <n-alert v-if="errorMessage" type="error" class="table-alert">
        {{ errorMessage }}
        <template #action><n-button size="small" @click="loadData">重试</n-button></template>
      </n-alert>

      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :scroll-x="1780"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="820" placement="right">
      <n-drawer-content
        :title="detail?.moment.blogName || '动态详情'"
        closable
        body-content-style="padding: 0"
      >
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <section class="governance-summary">
              <div class="detail-tags">
                <n-tag :type="statusTone(detail.moment.status)" :bordered="false">
                  {{ statusLabel(detail.moment.status) }}
                </n-tag>
                <n-tag :bordered="false">{{ statusLabel(detail.moment.momentType) }}</n-tag>
                <n-tag :bordered="false">{{ statusLabel(detail.moment.visibility) }}</n-tag>
              </div>
              <article
                v-if="detail.moment.renderedHtml"
                class="governance-content safe-html"
                v-html="detail.moment.renderedHtml"
              />
              <p v-else class="governance-content">
                {{ detail.moment.textContent || '该动态没有文字正文' }}
              </p>
              <a
                v-if="detail.moment.linkUrl"
                :href="detail.moment.linkUrl"
                target="_blank"
                rel="noopener noreferrer"
                class="moment-link"
              >
                {{ detail.moment.linkUrl }}
              </a>
              <div class="summary-meta">
                <span>{{ actorLabel(detail.moment) }}</span>
                <span>{{ formatDateTime(detail.moment.createdAt) }}</span>
                <span>赞 {{ compactNumber(detail.moment.likeCount) }}</span>
                <span>藏 {{ compactNumber(detail.moment.favoriteCount) }}</span>
                <span>评 {{ compactNumber(detail.moment.commentCount) }}</span>
                <span>转 {{ compactNumber(detail.moment.repostCount) }}</span>
              </div>
            </section>

            <n-descriptions :column="2" bordered size="small" class="detail-descriptions">
              <n-descriptions-item label="动态 ID">{{ detail.moment.id }}</n-descriptions-item>
              <n-descriptions-item label="发布者 ID">{{ detail.moment.actorUserId }}</n-descriptions-item>
              <n-descriptions-item label="博客 ID">{{ detail.moment.blogId }}</n-descriptions-item>
              <n-descriptions-item label="锁版本">{{ detail.moment.lockVersion }}</n-descriptions-item>
              <n-descriptions-item label="关联文章">{{ detail.moment.articleId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="转发源">{{ detail.moment.repostMomentId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="更新时间">{{ formatDateTime(detail.moment.updatedAt) }}</n-descriptions-item>
              <n-descriptions-item label="删除时间">{{ formatDateTime(detail.moment.deletedAt) }}</n-descriptions-item>
            </n-descriptions>

            <section class="governance-events">
              <div class="section-heading">
                <div>
                  <h3>不可变治理事件</h3>
                  <p>仅平台管理员可产生事件，事件不提供修改和删除入口。</p>
                </div>
                <n-tag size="small">{{ detail.events.length }} 条</n-tag>
              </div>
              <n-timeline v-if="detail.events.length">
                <n-timeline-item
                  v-for="event in detail.events"
                  :key="event.id"
                  :type="statusTone(event.newStatus)"
                  :title="statusLabel(event.action)"
                  :time="formatDateTime(event.createdAt)"
                >
                  <div class="event-body">
                    <span>{{ statusLabel(event.previousStatus) }} → {{ statusLabel(event.newStatus) }}</span>
                    <span>管理员 #{{ event.actorAdminId || '—' }}</span>
                    <p v-if="event.reason">{{ event.reason }}</p>
                  </div>
                </n-timeline-item>
              </n-timeline>
              <n-empty v-else description="暂无平台治理事件" />
            </section>
          </template>
          <n-result
            v-else-if="detailError"
            status="error"
            title="动态详情加载失败"
            :description="detailError"
          >
            <template #footer><n-button @click="retryDetail">重新加载</n-button></template>
          </n-result>
        </n-spin>

        <template v-if="detail && availableActions(detail.moment).length" #footer>
          <n-space justify="end">
            <n-button
              v-for="action in availableActions(detail.moment)"
              :key="action.value"
              :type="action.value === 'reject' || action.value === 'take-down' ? 'error' : 'primary'"
              :secondary="action.value === 'reject' || action.value === 'take-down'"
              :loading="actionLoading"
              @click="openSingleAction(detail.moment, action.value)"
            >
              {{ action.label }}
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="actionVisible"
      preset="card"
      :title="actionTitle"
      class="dialog-form-sm"
    >
      <n-alert :type="isDestructiveAction ? 'warning' : 'info'">
        {{ actionScopeText }}；恢复转发动态时会重新校验转发源，计数与状态同事务提交。
      </n-alert>
      <n-form label-placement="top" class="decision-form">
        <n-form-item label="治理原因" :required="isDestructiveAction">
          <n-input
            v-model:value="actionReason"
            type="textarea"
            :maxlength="500"
            show-count
            :placeholder="isDestructiveAction ? '请填写明确、可审计的治理原因' : '可选：填写审核说明'"
            :autosize="{ minRows: 4, maxRows: 7 }"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="actionVisible = false">取消</n-button>
          <n-button
            :type="isDestructiveAction ? 'error' : 'primary'"
            :loading="actionLoading"
            @click="submitAction"
          >
            确认{{ actionTitle }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
  type DataTableRowKey
} from 'naive-ui'
import { EyeOutline, RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import {
  communityApi,
  type CommunityMoment,
  type CommunityMomentDetail,
  type GovernanceAction
} from '@/api/community'
import {
  compactNumber,
  formatDateTime,
  statusLabel,
  statusTone
} from '@/utils/community'
import { useUserStore } from '@/stores/user'

interface ActionOption {
  label: string
  value: GovernanceAction
}

const message = useMessage()
const userStore = useUserStore()
const rows = ref<CommunityMoment[]>([])
const loading = ref(false)
const errorMessage = ref('')
const checkedRowKeys = ref<DataTableRowKey[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<CommunityMomentDetail | null>(null)
const selectedId = ref('')
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionReason = ref('')
const actionTarget = ref<CommunityMoment | null>(null)
const actionMode = ref<'single' | 'batch'>('single')
const selectedAction = ref<GovernanceAction>('approve')
const batchAction = ref<GovernanceAction | null>(null)

const filters = reactive({
  keyword: '',
  status: null as string | null,
  visibility: null as string | null,
  momentType: null as string | null,
  blogId: '',
  actorUserId: ''
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const statusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已隐藏', value: 'HIDDEN' },
  { label: '平台下架', value: 'TAKEN_DOWN' },
  { label: '已删除', value: 'DELETED' }
]
const visibilityOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '仅关注者', value: 'FOLLOWERS_ONLY' },
  { label: '私密', value: 'PRIVATE' },
  { label: '不列出', value: 'UNLISTED' }
]
const momentTypeOptions = [
  'TEXT', 'IMAGE', 'LINK', 'ARTICLE_SHARE', 'PROJECT_UPDATE',
  'CODE', 'POLL', 'TEAM_NOTICE', 'REPOST', 'QUOTE', 'VIDEO_LINK'
].map(value => ({ label: statusLabel(value), value }))
const allActionOptions: ActionOption[] = [
  { label: '审核通过', value: 'approve' },
  { label: '审核驳回', value: 'reject' },
  { label: '平台下架', value: 'take-down' },
  { label: '恢复发布', value: 'restore' }
]
const permissionMap: Record<GovernanceAction, string> = {
  approve: 'community:moment:approve',
  reject: 'community:moment:reject',
  'take-down': 'community:moment:takeDown',
  restore: 'community:moment:restore'
}
const statusActionMap: Record<string, GovernanceAction[]> = {
  PENDING_REVIEW: ['approve', 'reject'],
  PUBLISHED: ['take-down'],
  TAKEN_DOWN: ['restore']
}

const availableActionOptions = computed(() =>
  allActionOptions.filter(option => userStore.hasPermission(permissionMap[option.value]))
)
const actionTitle = computed(() =>
  allActionOptions.find(option => option.value === selectedAction.value)?.label || '治理'
)
const isDestructiveAction = computed(() =>
  selectedAction.value === 'reject' || selectedAction.value === 'take-down'
)
const actionScopeText = computed(() =>
  actionMode.value === 'batch'
    ? `将对选中的 ${checkedRowKeys.value.length} 条动态执行批量治理`
    : '将对当前动态执行治理'
)

const columns: DataTableColumns<CommunityMoment> = [
  { type: 'selection', fixed: 'left', width: 44 },
  {
    title: '动态内容',
    key: 'textContent',
    minWidth: 320,
    fixed: 'left',
    ellipsis: { tooltip: true },
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.textContent || row.linkUrl || statusLabel(row.momentType)),
      h('span', `${row.blogName || `博客 #${row.blogId}`} · ${statusLabel(row.momentType)}`)
    ])
  },
  {
    title: '状态',
    key: 'status',
    width: 115,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.status)
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: '可见性',
    key: 'visibility',
    width: 110,
    render: row => statusLabel(row.visibility)
  },
  {
    title: '发布者',
    key: 'actorUsername',
    width: 180,
    render: row => actorLabel(row)
  },
  {
    title: '互动数据',
    key: 'likeCount',
    width: 245,
    render: row => `赞 ${compactNumber(row.likeCount)} · 藏 ${compactNumber(row.favoriteCount)} · 评 ${compactNumber(row.commentCount)} · 转 ${compactNumber(row.repostCount)}`
  },
  {
    title: '锁 / 事件',
    key: 'lockVersion',
    width: 120,
    render: row => `v${row.lockVersion} · ${row.eventCount} 条`
  },
  {
    title: '发布时间',
    key: 'createdAt',
    width: 170,
    render: row => formatDateTime(row.createdAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    fixed: 'right',
    render: row => h(NSpace, { size: 4 }, {
      default: () => [
        userStore.hasPermission('community:moment:query')
          ? h(NButton, {
              size: 'small',
              quaternary: true,
              type: 'primary',
              onClick: () => openDetail(row.id)
            }, { icon: () => h(EyeOutline), default: () => '详情' })
          : null,
        ...availableActions(row).slice(0, 2).map(action =>
          h(NButton, {
            size: 'small',
            quaternary: true,
            type: action.value === 'reject' || action.value === 'take-down' ? 'error' : 'primary',
            onClick: () => openSingleAction(row, action.value)
          }, { default: () => action.label })
        )
      ]
    })
  }
]

function rowKey(row: CommunityMoment) {
  return row.id
}

function actorLabel(row: CommunityMoment) {
  const name = row.actorDisplayName || row.actorUsername || '社区用户'
  return row.actorUsername ? `${name} (@${row.actorUsername})` : `${name} #${row.actorUserId}`
}

function availableActions(row: CommunityMoment): ActionOption[] {
  const allowed = statusActionMap[row.status] || []
  return allActionOptions.filter(option =>
    allowed.includes(option.value) && userStore.hasPermission(permissionMap[option.value])
  )
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.moments({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      visibility: filters.visibility || undefined,
      momentType: filters.momentType || undefined,
      blogId: filters.blogId || undefined,
      actorUserId: filters.actorUserId || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
    checkedRowKeys.value = []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '动态列表加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(id: string) {
  selectedId.value = id
  detail.value = null
  detailError.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await communityApi.moment(id)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '动态详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function retryDetail() {
  if (selectedId.value) openDetail(selectedId.value)
}

function openSingleAction(row: CommunityMoment, action: GovernanceAction) {
  actionMode.value = 'single'
  actionTarget.value = row
  selectedAction.value = action
  actionReason.value = ''
  actionVisible.value = true
}

function openBatchAction() {
  if (!batchAction.value) return
  const selected = rows.value.filter(row => checkedRowKeys.value.includes(row.id))
  if (!selected.length) {
    message.warning('请选择动态')
    return
  }
  const invalid = selected.find(row => !(statusActionMap[row.status] || []).includes(batchAction.value!))
  if (invalid) {
    message.warning(`动态 #${invalid.id} 当前状态不能执行该动作`)
    return
  }
  actionMode.value = 'batch'
  actionTarget.value = null
  selectedAction.value = batchAction.value
  actionReason.value = ''
  actionVisible.value = true
}

async function submitAction() {
  if (isDestructiveAction.value && !actionReason.value.trim()) {
    message.warning('请填写治理原因')
    return
  }
  actionLoading.value = true
  try {
    if (actionMode.value === 'single' && actionTarget.value) {
      const result = await communityApi.governMoment(
        actionTarget.value.id,
        selectedAction.value,
        actionTarget.value.lockVersion,
        actionReason.value.trim() || undefined
      )
      message.success(result.replay ? '该治理动作已执行，无需重复处理' : `${actionTitle.value}成功`)
      actionVisible.value = false
      await loadData()
      if (detailVisible.value) await openDetail(result.id)
    } else {
      const selected = rows.value.filter(row => checkedRowKeys.value.includes(row.id))
      const result = await communityApi.batchGovernMoments(
        selectedAction.value,
        selected.map(row => ({ id: row.id, expectedLockVersion: row.lockVersion })),
        actionReason.value.trim() || undefined
      )
      message.success(`已完成 ${result.length} 条动态治理`)
      actionVisible.value = false
      await loadData()
    }
  } catch (error) {
    message.error(error instanceof Error ? error.message : '治理操作失败')
  } finally {
    actionLoading.value = false
  }
}

function search() {
  pagination.page = 1
  loadData()
}

function reset() {
  filters.keyword = ''
  filters.status = null
  filters.visibility = null
  filters.momentType = null
  filters.blogId = ''
  filters.actorUserId = ''
  search()
}

function changePage(page: number) {
  pagination.page = page
  loadData()
}

function changePageSize(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.governance-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--community-border);
  border-radius: 8px;
  background: var(--community-primary-soft);

  strong,
  span {
    display: block;
  }

  strong {
    color: var(--community-text);
  }

  span {
    margin-top: 3px;
    color: var(--community-muted);
    font-size: 12px;
  }
}

.governance-summary {
  padding: 22px 24px;
  border-bottom: 1px solid var(--community-border);
}

.detail-tags,
.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.governance-content {
  margin: 18px 0;
  color: var(--community-text);
  font-size: 15px;
  line-height: 1.8;
}

.moment-link {
  display: block;
  overflow: hidden;
  margin: -6px 0 16px;
  color: var(--primary-color);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-meta {
  color: var(--community-muted);
  font-size: 12px;
}

.detail-descriptions {
  padding: 22px 24px 0;
}

.governance-events {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;

  h3,
  p {
    margin: 0;
  }

  h3 {
    color: var(--community-text);
    font-size: 16px;
  }

  p {
    margin-top: 4px;
    color: var(--community-muted);
    font-size: 12px;
  }
}

.event-body {
  display: grid;
  gap: 4px;
  color: var(--community-muted);
  font-size: 12px;

  p {
    margin: 2px 0 0;
    color: var(--community-text);
  }
}

.decision-form {
  margin-top: 18px;
}

@media (max-width: 760px) {
  .governance-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
