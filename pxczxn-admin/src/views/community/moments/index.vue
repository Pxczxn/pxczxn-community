<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <h1>动态管理</h1>
        <p>查看社区动态、检索内容、理解传播和互动状态、处理待审核内容、核查举报关联，并在必要时执行平台治理。</p>
      </div>
      <n-button :loading="loading" @click="loadData">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新
      </n-button>
    </header>

    <div class="overview-stats">
      <div class="stat-card">
        <span class="stat-label">动态总数</span>
        <strong class="stat-value">{{ compactNumber(overview.totalMoments) }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">今日新增</span>
        <strong class="stat-value">{{ compactNumber(overview.todayNew) }}</strong>
      </div>
      <div class="stat-card stat-pending">
        <span class="stat-label">待审核</span>
        <strong class="stat-value">{{ compactNumber(overview.pendingReview) }}</strong>
      </div>
      <div class="stat-card stat-takedown">
        <span class="stat-label">平台下架</span>
        <strong class="stat-value">{{ compactNumber(overview.takenDown) }}</strong>
      </div>
      <div class="stat-card">
        <span class="stat-label">今日互动</span>
        <strong class="stat-value">{{ compactNumber(overview.todayInteractions) }}</strong>
      </div>
    </div>

    <n-card>
      <n-tabs v-model:value="activeTab" type="line" animated @update:value="onTabChange">
        <n-tab-pane name="all" tab="全部动态" />
        <n-tab-pane name="PENDING_REVIEW" tab="待审核" />
        <n-tab-pane name="PUBLISHED" tab="已发布" />
        <n-tab-pane name="HIDDEN" tab="已隐藏" />
        <n-tab-pane name="TAKEN_DOWN" tab="已下架" />
        <n-tab-pane name="DELETED" tab="已删除" />
      </n-tabs>

      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="正文或链接"
            @keyup.enter="search"
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
          <strong>批量处理</strong>
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
            执行批量处理
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
        :scroll-x="1960"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="880" placement="right">
      <n-drawer-content
        :title="detail?.moment.blogName || '动态详情'"
        closable
        body-content-style="padding: 0"
      >
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <section class="detail-section governance-summary">
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
            </section>

            <section class="detail-section">
              <h3 class="section-title">发布信息</h3>
              <n-descriptions :column="2" bordered size="small">
                <n-descriptions-item label="发布者">
                  <n-button text type="primary" @click="goUser(detail.moment.actorUserId)">
                    {{ actorLabel(detail.moment) }}
                  </n-button>
                </n-descriptions-item>
                <n-descriptions-item label="来源博客">
                  <n-button text type="primary" @click="goBlog(detail.moment.blogId)">
                    {{ detail.moment.blogName || `博客 #${detail.moment.blogId}` }}
                  </n-button>
                </n-descriptions-item>
                <n-descriptions-item label="动态类型">{{ statusLabel(detail.moment.momentType) }}</n-descriptions-item>
                <n-descriptions-item label="可见范围">{{ statusLabel(detail.moment.visibility) }}</n-descriptions-item>
                <n-descriptions-item label="当前状态">
                  <n-tag size="small" :type="statusTone(detail.moment.status)" :bordered="false">
                    {{ statusLabel(detail.moment.status) }}
                  </n-tag>
                </n-descriptions-item>
                <n-descriptions-item label="发布时间">{{ formatDateTime(detail.moment.createdAt) }}</n-descriptions-item>
                <n-descriptions-item label="动态 ID">{{ detail.moment.id }}</n-descriptions-item>
                <n-descriptions-item label="发布者 ID">{{ detail.moment.actorUserId }}</n-descriptions-item>
                <n-descriptions-item label="博客 ID">{{ detail.moment.blogId }}</n-descriptions-item>
                <n-descriptions-item label="锁版本">v{{ detail.moment.lockVersion }}</n-descriptions-item>
                <n-descriptions-item label="更新时间">{{ formatDateTime(detail.moment.updatedAt) }}</n-descriptions-item>
                <n-descriptions-item label="删除时间">{{ formatDateTime(detail.moment.deletedAt) }}</n-descriptions-item>
              </n-descriptions>
            </section>

            <section v-if="relatedContentVisible" class="detail-section">
              <h3 class="section-title">关联内容</h3>
              <div v-if="detail.moment.momentType === 'ARTICLE_SHARE' && detail.moment.articleId" class="related-item">
                <span>文章 ID：{{ detail.moment.articleId }}</span>
                <n-button text type="primary" @click="goArticle(detail.moment.articleId)">查看文章</n-button>
              </div>
              <div v-else-if="(detail.moment.momentType === 'REPOST' || detail.moment.momentType === 'QUOTE') && detail.moment.repostMomentId" class="related-item">
                <span>原动态 ID：{{ detail.moment.repostMomentId }}</span>
                <n-button text type="primary" @click="openDetail(detail.moment.repostMomentId!)">查看原动态</n-button>
              </div>
              <div v-else-if="(detail.moment.momentType === 'LINK' || detail.moment.momentType === 'VIDEO_LINK') && detail.moment.linkUrl" class="related-item">
                <span>域名：{{ linkDomain(detail.moment.linkUrl) }}</span>
                <n-button text type="primary" @click="openLink(detail.moment.linkUrl)">打开链接</n-button>
              </div>
            </section>

            <section class="detail-section">
              <h3 class="section-title">互动情况</h3>
              <div class="interaction-grid">
                <div class="interaction-item">
                  <span>点赞 {{ compactNumber(detail.moment.likeCount) }}</span>
                  <n-button text size="small" type="primary" @click="goInteractions(detail.moment.id, 'LIKE')">查看点赞记录</n-button>
                </div>
                <div class="interaction-item">
                  <span>收藏 {{ compactNumber(detail.moment.favoriteCount) }}</span>
                  <n-button text size="small" type="primary" @click="goInteractions(detail.moment.id, 'FAVORITE')">查看收藏记录</n-button>
                </div>
                <div class="interaction-item">
                  <span>评论 {{ compactNumber(detail.moment.commentCount) }}</span>
                  <n-button text size="small" type="primary" @click="goComments(detail.moment.id)">查看评论</n-button>
                </div>
                <div class="interaction-item">
                  <span>转发 {{ compactNumber(detail.moment.repostCount) }}</span>
                  <n-button text size="small" type="primary" @click="goInteractions(detail.moment.id, 'FOLLOW')">查看关注关系</n-button>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <h3 class="section-title">举报关联</h3>
              <div class="related-item">
                <span>相关举报 {{ detail.moment.reportCount }} 条</span>
                <n-button text type="primary" @click="goReports(detail.moment.id)">查看全部举报</n-button>
              </div>
              <p v-if="detail.moment.reportCount === 0" class="muted-note">该动态暂无举报记录。</p>
            </section>

            <section class="detail-section governance-events">
              <div class="section-heading">
                <div>
                  <h3 class="section-title">治理记录</h3>
                  <p class="muted-note">仅平台管理员可产生事件，事件不提供修改和删除入口。</p>
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
        <n-form-item :label="actionReasonLabel" :required="isDestructiveAction">
          <n-input
            v-model:value="actionReason"
            type="textarea"
            :maxlength="500"
            show-count
            :placeholder="actionReasonPlaceholder"
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
import { useRouter } from 'vue-router'
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
  type CommunityMomentOverview,
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
const router = useRouter()
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
const activeTab = ref('all')
const overview = ref<CommunityMomentOverview>({
  totalMoments: 0,
  todayNew: 0,
  pendingReview: 0,
  takenDown: 0,
  todayInteractions: 0
})

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

const visibilityOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '仅关注者', value: 'FOLLOWERS_ONLY' },
  { label: '私密', value: 'PRIVATE' },
  { label: '不列出', value: 'UNLISTED' }
]
const momentTypeOptions = [
  'TEXT', 'LINK', 'ARTICLE_SHARE', 'PROJECT_UPDATE',
  'CODE', 'REPOST', 'QUOTE', 'VIDEO_LINK'
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
const actionReasonPlaceholder = computed(() => {
  const map: Record<GovernanceAction, string> = {
    approve: '可选：填写审核说明',
    reject: '请填写明确、可审计的驳回原因',
    'take-down': '请填写明确、可审计的下架原因',
    restore: '请填写恢复说明'
  }
  return map[selectedAction.value] || '请填写处理原因'
})
const actionReasonLabel = computed(() => {
  const map: Record<GovernanceAction, string> = {
    approve: '审核说明',
    reject: '驳回原因',
    'take-down': '下架原因',
    restore: '恢复说明'
  }
  return map[selectedAction.value] || '处理原因'
})
const isDestructiveAction = computed(() =>
  selectedAction.value === 'reject' || selectedAction.value === 'take-down'
)
const actionScopeText = computed(() =>
  actionMode.value === 'batch'
    ? `将对选中的 ${checkedRowKeys.value.length} 条动态执行批量治理`
    : '将对当前动态执行治理'
)
const relatedContentVisible = computed(() => {
  if (!detail.value) return false
  const m = detail.value.moment
  if (m.momentType === 'ARTICLE_SHARE' && m.articleId) return true
  if ((m.momentType === 'REPOST' || m.momentType === 'QUOTE') && m.repostMomentId) return true
  if ((m.momentType === 'LINK' || m.momentType === 'VIDEO_LINK') && m.linkUrl) return true
  return false
})

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
    title: '发布者',
    key: 'actorUsername',
    width: 180,
    render: row => h(NButton, {
      text: true,
      type: 'primary',
      onClick: () => goUser(row.actorUserId)
    }, { default: () => actorLabel(row) })
  },
  {
    title: '来源博客',
    key: 'blogName',
    width: 160,
    ellipsis: { tooltip: true },
    render: row => h(NButton, {
      text: true,
      type: 'primary',
      onClick: () => goBlog(row.blogId)
    }, { default: () => row.blogName || `博客 #${row.blogId}` })
  },
  {
    title: '类型',
    key: 'momentType',
    width: 110,
    render: row => h(NTag, { size: 'small', bordered: false }, { default: () => statusLabel(row.momentType) })
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
    title: '互动',
    key: 'likeCount',
    width: 245,
    render: row => `赞 ${compactNumber(row.likeCount)} · 评 ${compactNumber(row.commentCount)} · 藏 ${compactNumber(row.favoriteCount)} · 转 ${compactNumber(row.repostCount)}`
  },
  {
    title: '举报',
    key: 'reportCount',
    width: 110,
    render: row => row.reportCount > 0
      ? h(NButton, {
          text: true,
          type: 'warning',
          onClick: () => goReports(row.id)
        }, { default: () => `${row.reportCount} 条举报` })
      : '—'
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

function linkDomain(url: string): string {
  try {
    return new URL(url).hostname
  } catch {
    return url
  }
}

function goUser(userId: string) {
  router.push({ path: '/community/users', query: { keyword: userId } })
}

function goBlog(blogId: string) {
  router.push({ path: '/community/blogs', query: { keyword: blogId } })
}

function goArticle(articleId: string) {
  router.push({ path: '/community/articles', query: { keyword: articleId } })
}

function goReports(momentId: string) {
  router.push({ path: '/community/reports', query: { targetType: 'MOMENT', targetId: momentId } })
}

function goComments(momentId: string) {
  router.push({ path: '/community/comments', query: { targetType: 'MOMENT', targetId: momentId } })
}

function goInteractions(momentId: string, interactionType: string) {
  router.push({ path: '/community/interactions', query: { targetType: 'MOMENT', targetId: momentId, interactionType } })
}

function openLink(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

function onTabChange(name: string) {
  filters.status = name === 'all' ? null : name
  search()
}

async function loadOverview() {
  try {
    overview.value = await communityApi.momentOverview()
  } catch {
    // 概览失败不阻塞列表
  }
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
      await loadOverview()
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
      await loadOverview()
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
  activeTab.value = 'all'
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

onMounted(() => {
  loadOverview()
  loadData()
})
</script>

<style scoped lang="scss">
.overview-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border: 1px solid var(--community-border);
  border-radius: 10px;
  background: var(--community-primary-soft);
}

.stat-label {
  color: var(--community-muted);
  font-size: 12px;
}

.stat-value {
  color: var(--community-text);
  font-size: 24px;
  font-weight: 700;
}

.stat-pending .stat-value {
  color: #f0a020;
}

.stat-takedown .stat-value {
  color: #d03050;
}

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

.detail-section {
  padding: 20px 24px;
  border-bottom: 1px solid var(--community-border);

  &:last-child {
    border-bottom: none;
  }
}

.governance-summary {
  padding-top: 24px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}

.governance-content {
  margin: 14px 0;
  color: var(--community-text);
  font-size: 15px;
  line-height: 1.8;
}

.moment-link {
  display: block;
  overflow: hidden;
  margin: -6px 0 0;
  color: var(--primary-color);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-title {
  margin: 0 0 14px;
  color: var(--community-text);
  font-size: 15px;
  font-weight: 600;
}

.muted-note {
  margin: 6px 0 0;
  color: var(--community-muted);
  font-size: 12px;
}

.related-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--community-text);
  font-size: 13px;
}

.interaction-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.interaction-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--community-border);
  border-radius: 8px;
  color: var(--community-text);
  font-size: 13px;
}

.governance-events {
  .section-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;

    .section-title {
      margin: 0;
    }
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

@media (max-width: 1200px) {
  .overview-stats {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 760px) {
  .overview-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .governance-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .interaction-grid {
    grid-template-columns: 1fr;
  }
}
</style>
