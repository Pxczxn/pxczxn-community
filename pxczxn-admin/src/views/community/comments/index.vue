<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">COMMENT GOVERNANCE</div>
        <h1>评论治理</h1>
        <p>审核关键词转人工评论，执行线程级下架与恢复，并追溯不可变治理事件。</p>
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
            placeholder="评论正文"
            @keyup.enter="search"
          />
        </n-form-item>
        <n-form-item label="状态">
          <n-select
            v-model:value="filters.status"
            :options="statusOptions"
            clearable
            placeholder="全部状态"
            style="width: 160px"
          />
        </n-form-item>
        <n-form-item label="目标">
          <n-select
            v-model:value="filters.targetType"
            :options="targetOptions"
            clearable
            placeholder="文章 / 动态"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="目标 ID">
          <n-input v-model:value="filters.targetId" clearable placeholder="Snowflake ID" />
        </n-form-item>
        <n-form-item label="作者 ID">
          <n-input v-model:value="filters.authorUserId" clearable placeholder="用户 ID" />
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

      <div v-if="userStore.hasPermission('community:comment:batch')" class="governance-toolbar">
        <div>
          <strong>批量治理</strong>
          <span>已选择 {{ checkedRowKeys.length }} 条；整批使用事务与逐条乐观锁。</span>
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
        :scroll-x="1640"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="780" placement="right">
      <n-drawer-content
        :title="detail?.comment.targetTitle || '评论详情'"
        closable
        body-content-style="padding: 0"
      >
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <section class="governance-summary">
              <div class="detail-tags">
                <n-tag :type="statusTone(detail.comment.status)" :bordered="false">
                  {{ statusLabel(detail.comment.status) }}
                </n-tag>
                <n-tag :bordered="false">{{ statusLabel(detail.comment.targetType) }}</n-tag>
                <n-tag v-if="detail.comment.rootCommentId" :bordered="false">回复</n-tag>
                <n-tag v-else :bordered="false">一级评论</n-tag>
              </div>
              <article
                v-if="detail.comment.renderedHtml"
                class="governance-content safe-html"
                v-html="detail.comment.renderedHtml"
              />
              <p v-else class="governance-content">{{ detail.comment.contentText }}</p>
              <div class="summary-meta">
                <span>{{ authorLabel(detail.comment) }}</span>
                <span>{{ formatDateTime(detail.comment.createdAt) }}</span>
                <span>赞 {{ compactNumber(detail.comment.likeCount) }}</span>
              </div>
            </section>

            <n-descriptions :column="2" bordered size="small" class="detail-descriptions">
              <n-descriptions-item label="评论 ID">{{ detail.comment.id }}</n-descriptions-item>
              <n-descriptions-item label="作者 ID">{{ detail.comment.authorUserId }}</n-descriptions-item>
              <n-descriptions-item label="目标 ID">{{ detail.comment.targetId }}</n-descriptions-item>
              <n-descriptions-item label="锁版本">{{ detail.comment.lockVersion }}</n-descriptions-item>
              <n-descriptions-item label="根评论">{{ detail.comment.rootCommentId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="父评论">{{ detail.comment.parentCommentId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="更新时间">{{ formatDateTime(detail.comment.updatedAt) }}</n-descriptions-item>
              <n-descriptions-item label="删除时间">{{ formatDateTime(detail.comment.deletedAt) }}</n-descriptions-item>
            </n-descriptions>

            <section class="governance-events">
              <div class="section-heading">
                <div>
                  <h3>不可变治理事件</h3>
                  <p>自动审核、用户动作和平台动作按时间倒序保留。</p>
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
                    <span>{{ actorLabel(event) }}</span>
                    <p v-if="event.reason">{{ event.reason }}</p>
                  </div>
                </n-timeline-item>
              </n-timeline>
              <n-empty v-else description="暂无治理事件" />
            </section>
          </template>
          <n-result
            v-else-if="detailError"
            status="error"
            title="评论详情加载失败"
            :description="detailError"
          >
            <template #footer><n-button @click="retryDetail">重新加载</n-button></template>
          </n-result>
        </n-spin>

        <template v-if="detail && availableActions(detail.comment).length" #footer>
          <n-space justify="end">
            <n-button
              v-for="action in availableActions(detail.comment)"
              :key="action.value"
              :type="action.value === 'reject' || action.value === 'take-down' ? 'error' : 'primary'"
              :secondary="action.value === 'reject' || action.value === 'take-down'"
              :loading="actionLoading"
              @click="openSingleAction(detail.comment, action.value)"
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
        {{ actionScopeText }}；提交时会校验当前锁版本并写入不可变治理事件。
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
  type CommunityComment,
  type CommunityCommentDetail,
  type GovernanceAction,
  type GovernanceEvent
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
const rows = ref<CommunityComment[]>([])
const loading = ref(false)
const errorMessage = ref('')
const checkedRowKeys = ref<DataTableRowKey[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<CommunityCommentDetail | null>(null)
const selectedId = ref('')
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionReason = ref('')
const actionTarget = ref<CommunityComment | null>(null)
const actionMode = ref<'single' | 'batch'>('single')
const selectedAction = ref<GovernanceAction>('approve')
const batchAction = ref<GovernanceAction | null>(null)

const filters = reactive({
  keyword: '',
  status: null as string | null,
  targetType: null as string | null,
  targetId: '',
  authorUserId: ''
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
  { label: '作者隐藏', value: 'HIDDEN_BY_AUTHOR' },
  { label: '博客隐藏', value: 'HIDDEN_BY_BLOG' },
  { label: '用户删除', value: 'DELETED_BY_USER' },
  { label: '平台下架', value: 'TAKEN_DOWN' },
  { label: '垃圾内容', value: 'SPAM' }
]
const targetOptions = [
  { label: '文章', value: 'ARTICLE' },
  { label: '动态', value: 'MOMENT' }
]
const allActionOptions: ActionOption[] = [
  { label: '审核通过', value: 'approve' },
  { label: '审核驳回', value: 'reject' },
  { label: '平台下架', value: 'take-down' },
  { label: '恢复发布', value: 'restore' }
]
const permissionMap: Record<GovernanceAction, string> = {
  approve: 'community:comment:approve',
  reject: 'community:comment:reject',
  'take-down': 'community:comment:takeDown',
  restore: 'community:comment:restore'
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
    ? `将对选中的 ${checkedRowKeys.value.length} 条评论执行批量治理`
    : '将对当前评论执行治理'
)

const columns: DataTableColumns<CommunityComment> = [
  { type: 'selection', fixed: 'left', width: 44 },
  {
    title: '评论内容',
    key: 'contentText',
    minWidth: 320,
    fixed: 'left',
    ellipsis: { tooltip: true },
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.contentText),
      h('span', `${row.targetTitle || statusLabel(row.targetType)} · ${row.rootCommentId ? '回复' : '一级评论'}`)
    ])
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.status)
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: '作者',
    key: 'authorUsername',
    width: 180,
    render: row => authorLabel(row)
  },
  {
    title: '目标',
    key: 'targetType',
    width: 170,
    render: row => `${statusLabel(row.targetType)} #${row.targetId}`
  },
  {
    title: '数据',
    key: 'likeCount',
    width: 130,
    render: row => `赞 ${compactNumber(row.likeCount)} · 事件 ${row.eventCount}`
  },
  {
    title: '锁版本',
    key: 'lockVersion',
    width: 90,
    render: row => `v${row.lockVersion}`
  },
  {
    title: '提交时间',
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
        userStore.hasPermission('community:comment:query')
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

function rowKey(row: CommunityComment) {
  return row.id
}

function authorLabel(row: CommunityComment) {
  const name = row.authorDisplayName || row.authorUsername || '社区用户'
  return row.authorUsername ? `${name} (@${row.authorUsername})` : `${name} #${row.authorUserId}`
}

function actorLabel(event: GovernanceEvent) {
  if (event.actorType === 'ADMIN') return `管理员 #${event.actorAdminId || '—'}`
  if (event.actorType === 'USER') return `社区用户 #${event.actorUserId || '—'}`
  return '系统自动审核'
}

function availableActions(row: CommunityComment): ActionOption[] {
  const allowed = statusActionMap[row.status] || []
  return allActionOptions.filter(option =>
    allowed.includes(option.value) && userStore.hasPermission(permissionMap[option.value])
  )
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.comments({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      targetType: filters.targetType || undefined,
      targetId: filters.targetId || undefined,
      authorUserId: filters.authorUserId || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
    checkedRowKeys.value = []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '评论列表加载失败'
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
    detail.value = await communityApi.comment(id)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '评论详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function retryDetail() {
  if (selectedId.value) openDetail(selectedId.value)
}

function openSingleAction(row: CommunityComment, action: GovernanceAction) {
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
    message.warning('请选择评论')
    return
  }
  const invalid = selected.find(row => !(statusActionMap[row.status] || []).includes(batchAction.value!))
  if (invalid) {
    message.warning(`评论 #${invalid.id} 当前状态不能执行该动作`)
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
      const result = await communityApi.governComment(
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
      const result = await communityApi.batchGovernComments(
        selectedAction.value,
        selected.map(row => ({ id: row.id, expectedLockVersion: row.lockVersion })),
        actionReason.value.trim() || undefined
      )
      message.success(`已完成 ${result.length} 条评论治理`)
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
  filters.targetType = null
  filters.targetId = ''
  filters.authorUserId = ''
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
