<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">REVIEW QUEUE</div>
        <h1>文章审核</h1>
        <p>按风险级别处理固定内容版本，审核动作受任务锁与 RBAC 双重约束。</p>
      </div>
      <n-button :loading="loading" @click="loadData">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新队列
      </n-button>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="任务状态">
          <n-select
            v-model:value="filters.status"
            :options="statusOptions"
            clearable
            placeholder="全部状态"
            style="width: 170px"
          />
        </n-form-item>
        <n-form-item label="风险级别">
          <n-select
            v-model:value="filters.riskLevel"
            :options="riskOptions"
            clearable
            placeholder="全部级别"
            class="filter-select"
          />
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

      <n-alert v-if="errorMessage" type="error" class="table-alert">
        {{ errorMessage }}
        <template #action><n-button size="small" @click="loadData">重试</n-button></template>
      </n-alert>

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :scroll-x="1380"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="780" placement="right">
      <n-drawer-content
        :title="detail?.articleTitle || '审核详情'"
        closable
        body-content-style="padding: 0"
      >
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <div class="review-summary">
              <div class="detail-tags">
                <n-tag :type="statusTone(detail.taskStatus)" :bordered="false">
                  {{ statusLabel(detail.taskStatus) }}
                </n-tag>
                <n-tag :type="statusTone(detail.riskLevel)" :bordered="false">
                  {{ statusLabel(detail.riskLevel) }}
                </n-tag>
                <n-tag :bordered="false">{{ detail.reviewType }}</n-tag>
              </div>
              <p>{{ detail.articleSummary || '暂无摘要' }}</p>
              <div class="review-summary__line">
                <span>{{ detail.blogName }}</span>
                <span>{{ detail.authorDisplayName }} (@{{ detail.authorUsername }})</span>
                <span>固定版本 v{{ detail.content.versionNo }}</span>
              </div>
            </div>

            <n-descriptions :column="2" bordered size="small" class="detail-descriptions">
              <n-descriptions-item label="任务 ID">{{ detail.taskId }}</n-descriptions-item>
              <n-descriptions-item label="文章 ID">{{ detail.articleId }}</n-descriptions-item>
              <n-descriptions-item label="任务锁">{{ detail.taskLockVersion }}</n-descriptions-item>
              <n-descriptions-item label="文章锁">{{ detail.articleLockVersion }}</n-descriptions-item>
              <n-descriptions-item label="提交时间">{{ formatDateTime(detail.submittedAt) }}</n-descriptions-item>
              <n-descriptions-item label="领取时间">{{ formatDateTime(detail.claimedAt) }}</n-descriptions-item>
              <n-descriptions-item v-if="detail.resultCode" label="结果代码">
                {{ detail.resultCode }}
              </n-descriptions-item>
              <n-descriptions-item v-if="detail.resultReason" label="结果原因">
                {{ detail.resultReason }}
              </n-descriptions-item>
            </n-descriptions>

            <section class="review-content">
              <div class="review-content__heading">
                <div>
                  <h3>固定内容快照</h3>
                  <p>{{ detail.content.wordCount }} 字 · 约 {{ detail.content.readingTimeMinutes }} 分钟阅读</p>
                </div>
                <n-tag size="small">{{ detail.content.contentMode }}</n-tag>
              </div>
              <article
                v-if="detail.content.renderedHtml"
                class="safe-html"
                v-html="detail.content.renderedHtml"
              />
              <n-empty v-else description="该版本暂无安全渲染内容" />
            </section>
          </template>
          <n-result
            v-else-if="detailError"
            status="error"
            title="审核详情加载失败"
            :description="detailError"
          >
            <template #footer>
              <n-button @click="retryDetail">重新加载</n-button>
            </template>
          </n-result>
        </n-spin>

        <template v-if="detail && canOperate(detail)" #footer>
          <n-space justify="end">
            <n-button
              v-if="detail.taskStatus === 'QUEUED' && userStore.hasPermission('community:review:claim')"
              type="primary"
              :loading="actionLoading"
              @click="claim"
            >
              领取审核
            </n-button>
            <template v-if="detail.taskStatus === 'MANUAL_REVIEWING'">
              <n-button
                v-if="userStore.hasPermission('community:review:revision')"
                :loading="actionLoading"
                @click="openDecision('revision')"
              >
                要求修改
              </n-button>
              <n-button
                v-if="userStore.hasPermission('community:review:reject')"
                type="error"
                secondary
                :loading="actionLoading"
                @click="openDecision('reject')"
              >
                驳回
              </n-button>
              <n-button
                v-if="userStore.hasPermission('community:review:approve')"
                type="primary"
                :loading="actionLoading"
                @click="openDecision('approve')"
              >
                审核通过
              </n-button>
            </template>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="decisionVisible"
      preset="card"
      :title="decisionTitle"
      class="dialog-form-sm"
    >
      <n-alert :type="decisionAction === 'approve' ? 'success' : 'warning'">
        本次操作仅作用于当前固定版本，并校验任务锁版本。
      </n-alert>
      <n-form label-placement="top" class="decision-form">
        <n-form-item
          label="审核说明"
          :required="decisionAction !== 'approve'"
        >
          <n-input
            v-model:value="decisionReason"
            type="textarea"
            :maxlength="500"
            show-count
            :placeholder="decisionAction === 'approve' ? '可选：填写通过说明' : '请填写清晰、可执行的原因'"
            :autosize="{ minRows: 4, maxRows: 7 }"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="decisionVisible = false">取消</n-button>
          <n-button
            :type="decisionAction === 'reject' ? 'error' : 'primary'"
            :loading="actionLoading"
            @click="submitDecision"
          >
            确认{{ decisionTitle }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { NButton, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { EyeOutline, RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import {
  communityApi,
  type ArticleReviewDetail,
  type ArticleReviewListItem
} from '@/api/community'
import { formatDateTime, statusLabel, statusTone } from '@/utils/community'
import { useUserStore } from '@/stores/user'

type DecisionAction = 'approve' | 'revision' | 'reject'

const message = useMessage()
const userStore = useUserStore()
const rows = ref<ArticleReviewListItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<ArticleReviewDetail | null>(null)
const selectedTaskId = ref('')
const actionLoading = ref(false)
const decisionVisible = ref(false)
const decisionAction = ref<DecisionAction>('approve')
const decisionTitle = ref('审核通过')
const decisionReason = ref('')

const filters = reactive({
  status: null as string | null,
  riskLevel: null as string | null
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const statusOptions = [
  { label: '待领取', value: 'QUEUED' },
  { label: '自动审核中', value: 'AUTO_REVIEWING' },
  { label: '人工审核中', value: 'MANUAL_REVIEWING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]
const riskOptions = [
  { label: '低风险', value: 'LOW' },
  { label: '中风险', value: 'MEDIUM' },
  { label: '高风险', value: 'HIGH' },
  { label: '严重风险', value: 'CRITICAL' }
]

const columns: DataTableColumns<ArticleReviewListItem> = [
  {
    title: '待审文章',
    key: 'articleTitle',
    minWidth: 300,
    fixed: 'left',
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.articleTitle),
      h('span', `${row.blogName} · ${row.authorDisplayName}`)
    ])
  },
  {
    title: '任务状态',
    key: 'status',
    width: 120,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.status)
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: '风险',
    key: 'riskLevel',
    width: 100,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.riskLevel)
    }, { default: () => statusLabel(row.riskLevel) })
  },
  { title: '审核类型', key: 'reviewType', width: 130 },
  {
    title: '处理人',
    key: 'assigneeAdminId',
    width: 110,
    render: row => row.assigneeAdminId ? `#${row.assigneeAdminId}` : '未领取'
  },
  {
    title: '提交时间',
    key: 'submittedAt',
    width: 170,
    render: row => formatDateTime(row.submittedAt)
  },
  {
    title: '完成时间',
    key: 'completedAt',
    width: 170,
    render: row => formatDateTime(row.completedAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 170,
    fixed: 'right',
    render: row => h(NSpace, { size: 4 }, {
      default: () => [
        userStore.hasPermission('community:review:query')
          ? h(NButton, {
              size: 'small',
              quaternary: true,
              type: 'primary',
              onClick: () => openDetail(row.taskId)
            }, { icon: () => h(EyeOutline), default: () => '详情' })
          : null,
        row.status === 'QUEUED' && userStore.hasPermission('community:review:claim')
          ? h(NButton, {
              size: 'small',
              quaternary: true,
              onClick: () => claimFromRow(row)
            }, { default: () => '领取' })
          : null
      ]
    })
  }
]

const rowKey = (row: ArticleReviewListItem) => row.taskId

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.reviews({
      status: filters.status || undefined,
      riskLevel: filters.riskLevel || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '审核队列加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(taskId: string) {
  selectedTaskId.value = taskId
  detail.value = null
  detailError.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await communityApi.review(taskId)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '审核详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function retryDetail() {
  if (selectedTaskId.value) openDetail(selectedTaskId.value)
}

async function claimFromRow(row: ArticleReviewListItem) {
  actionLoading.value = true
  try {
    await communityApi.claimReview(row.taskId, row.taskLockVersion)
    message.success('审核任务领取成功')
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

async function claim() {
  if (!detail.value) return
  actionLoading.value = true
  try {
    detail.value = await communityApi.claimReview(
      detail.value.taskId,
      detail.value.taskLockVersion
    )
    message.success('审核任务领取成功')
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

function openDecision(action: DecisionAction) {
  decisionAction.value = action
  decisionTitle.value = {
    approve: '审核通过',
    revision: '要求修改',
    reject: '驳回文章'
  }[action]
  decisionReason.value = ''
  decisionVisible.value = true
}

async function submitDecision() {
  if (!detail.value) return
  if (decisionAction.value !== 'approve' && !decisionReason.value.trim()) {
    message.warning('请填写审核原因')
    return
  }
  actionLoading.value = true
  try {
    detail.value = await communityApi.decideReview(
      detail.value.taskId,
      decisionAction.value,
      detail.value.taskLockVersion,
      decisionReason.value.trim() || undefined
    )
    decisionVisible.value = false
    message.success(`${decisionTitle.value}成功`)
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

function canOperate(current: ArticleReviewDetail) {
  return current.taskStatus === 'QUEUED' || current.taskStatus === 'MANUAL_REVIEWING'
}

function search() {
  pagination.page = 1
  loadData()
}

function reset() {
  filters.status = null
  filters.riskLevel = null
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
.review-summary {
  padding: 24px;
  border-bottom: 1px solid var(--community-border);
  background: var(--community-soft);

  p {
    margin-top: 12px;
    color: var(--community-muted);
    line-height: 1.7;
  }

  &__line {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 20px;
    margin-top: 14px;
    color: var(--community-muted);
    font-size: 12px;
  }
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-descriptions {
  margin: 20px 24px;
}

.review-content {
  padding: 4px 24px 32px;

  &__heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h3 { font-size: 16px; }
    p { margin-top: 4px; color: var(--community-muted); font-size: 12px; }
  }
}

.safe-html {
  color: var(--community-text);
  font-size: 15px;
  line-height: 1.8;

  :deep(img) { max-width: 100%; border-radius: 8px; }
  :deep(pre) {
    overflow: auto;
    padding: 14px;
    border-radius: 8px;
    background: #111827;
    color: #E5E7EB;
  }
}

.decision-form {
  margin-top: 16px;
}
</style>
