<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">CONTENT INVENTORY</div>
        <h1>文章管理</h1>
        <p>检索文章发布、审核与可见性状态，并查看固定版本的安全渲染快照。</p>
      </div>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="标题、标识或作者"
            @keyup.enter="search"
          />
        </n-form-item>
        <n-form-item label="发布状态">
          <n-select
            v-model:value="filters.publishStatus"
            :options="publishOptions"
            clearable
            placeholder="全部状态"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="审核状态">
          <n-select
            v-model:value="filters.reviewStatus"
            :options="reviewOptions"
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
        <n-form-item>
          <n-space>
            <n-button type="primary" @click="search">
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              查询
            </n-button>
            <n-button @click="reset">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              重置
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>

      <n-alert v-if="errorMessage" type="error" class="table-alert">
        {{ errorMessage }}
        <template #action><n-button size="small" @click="loadData">重试</n-button></template>
      </n-alert>

      <div class="article-table-scroll">
        <n-data-table
          remote
          class="article-table"
          :columns="columns"
          :data="rows"
          :loading="loading"
          :pagination="pagination"
          :row-key="rowKey"
          :scroll-x="1480"
          @update:page="changePage"
          @update:page-size="changePageSize"
        />
      </div>
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="760" placement="right">
      <n-drawer-content
        :title="detail?.title || '文章详情'"
        closable
        body-content-style="padding: 0"
      >
        <n-spin :show="detailLoading">
          <template v-if="detail">
            <div class="detail-hero">
              <div class="detail-tags">
                <n-tag :type="statusTone(detail.publishStatus)" :bordered="false">
                  {{ statusLabel(detail.publishStatus) }}
                </n-tag>
                <n-tag :type="statusTone(detail.reviewStatus)" :bordered="false">
                  {{ statusLabel(detail.reviewStatus) }}
                </n-tag>
                <n-tag :bordered="false">{{ statusLabel(detail.visibility) }}</n-tag>
              </div>
              <p>{{ detail.summary || '暂无摘要' }}</p>
              <div class="detail-meta">
                <span>@{{ detail.authorUsername }}</span>
                <span>{{ detail.blogName }}</span>
                <span>版本 v{{ detail.currentVersionNo || '—' }}</span>
                <span>{{ detail.wordCount || 0 }} 字</span>
                <span>{{ detail.readingTimeMinutes || 0 }} 分钟阅读</span>
              </div>
            </div>

            <n-descriptions
              label-placement="left"
              :column="2"
              bordered
              size="small"
              class="detail-descriptions"
            >
              <n-descriptions-item label="文章 ID">{{ detail.id }}</n-descriptions-item>
              <n-descriptions-item label="博客 ID">{{ detail.blogId }}</n-descriptions-item>
              <n-descriptions-item label="发布方式">{{ statusLabel(detail.publishMethod) }}</n-descriptions-item>
              <n-descriptions-item label="锁版本">{{ detail.lockVersion }}</n-descriptions-item>
              <n-descriptions-item label="计划发布时间">{{ formatDateTime(detail.scheduledPublishAt) }}</n-descriptions-item>
              <n-descriptions-item label="实际发布时间">{{ formatDateTime(detail.publishedAt) }}</n-descriptions-item>
              <n-descriptions-item label="当前版本">{{ detail.currentVersionId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="公开版本">{{ detail.publishedVersionId || '—' }}</n-descriptions-item>
              <n-descriptions-item label="Canonical" :span="2">
                <code>{{ detail.canonicalPath || '—' }}</code>
              </n-descriptions-item>
            </n-descriptions>

            <section class="content-preview">
              <div class="content-preview__heading">
                <h3>安全渲染预览</h3>
                <n-space>
                  <n-tag v-for="tag in detail.tags" :key="tag" size="small">{{ tag }}</n-tag>
                </n-space>
              </div>
              <article
                v-if="detail.renderedHtml"
                class="safe-html"
                v-html="detail.renderedHtml"
              />
              <n-empty v-else description="该版本暂无可预览正文" />
            </section>
          </template>
          <n-result
            v-else-if="detailError"
            status="error"
            title="详情加载失败"
            :description="detailError"
          >
            <template #footer>
              <n-button @click="retryDetail">重新加载</n-button>
            </template>
          </n-result>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { NButton, NTag, type DataTableColumns } from 'naive-ui'
import { EyeOutline, RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { communityApi, type CommunityArticle } from '@/api/community'
import { compactNumber, formatDateTime, statusLabel, statusTone } from '@/utils/community'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const rows = ref<CommunityArticle[]>([])
const loading = ref(false)
const errorMessage = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<CommunityArticle | null>(null)
const selectedArticleId = ref('')

const filters = reactive({
  keyword: '',
  publishStatus: null as string | null,
  reviewStatus: null as string | null,
  visibility: null as string | null
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const publishOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '定时发布', value: 'SCHEDULED' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '发布失败', value: 'PUBLISH_FAILED' }
]
const reviewOptions = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '待修改', value: 'REVISION_REQUIRED' },
  { label: '已驳回', value: 'REJECTED' }
]
const visibilityOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '不列出', value: 'UNLISTED' },
  { label: '私密', value: 'PRIVATE' }
]

const columns: DataTableColumns<CommunityArticle> = [
  {
    title: '文章',
    key: 'title',
    minWidth: 300,
    fixed: 'left',
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.title),
      h('span', `${row.blogName} · @${row.authorUsername}`)
    ])
  },
  {
    title: '发布状态',
    key: 'publishStatus',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.publishStatus)
    }, { default: () => statusLabel(row.publishStatus) })
  },
  {
    title: '审核状态',
    key: 'reviewStatus',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.reviewStatus)
    }, { default: () => statusLabel(row.reviewStatus) })
  },
  {
    title: '可见性',
    key: 'visibility',
    width: 90,
    render: row => statusLabel(row.visibility)
  },
  {
    title: '版本',
    key: 'currentVersionNo',
    width: 80,
    render: row => row.currentVersionNo ? `v${row.currentVersionNo}` : '—'
  },
  {
    title: '数据',
    key: 'viewCount',
    width: 180,
    render: row => `浏览 ${compactNumber(row.viewCount)} · 赞 ${compactNumber(row.likeCount)}`
  },
  {
    title: '发布时间',
    key: 'publishedAt',
    width: 170,
    render: row => formatDateTime(row.publishedAt || row.scheduledPublishAt)
  },
  {
    title: '更新时间',
    key: 'updatedAt',
    width: 170,
    render: row => formatDateTime(row.updatedAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 96,
    fixed: 'right',
    render: row => userStore.hasPermission('community:article:query')
      ? h(NButton, {
          size: 'small',
          quaternary: true,
          type: 'primary',
          onClick: () => openDetail(row.id)
        }, {
          icon: () => h(EyeOutline),
          default: () => '详情'
        })
      : null
  }
]

const rowKey = (row: CommunityArticle) => row.id

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.articles({
      keyword: filters.keyword || undefined,
      publishStatus: filters.publishStatus || undefined,
      reviewStatus: filters.reviewStatus || undefined,
      visibility: filters.visibility || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '文章列表加载失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(articleId: string) {
  selectedArticleId.value = articleId
  detail.value = null
  detailError.value = ''
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await communityApi.article(articleId)
  } catch (error) {
    detailError.value = error instanceof Error ? error.message : '文章详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

function retryDetail() {
  if (selectedArticleId.value) openDetail(selectedArticleId.value)
}

function search() {
  pagination.page = 1
  loadData()
}

function reset() {
  filters.keyword = ''
  filters.publishStatus = null
  filters.reviewStatus = null
  filters.visibility = null
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
.article-table-scroll {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
  overscroll-behavior-inline: contain;

  .article-table {
    width: 100%;
    min-width: 0;

    :deep(.n-data-table-wrapper) {
      width: 100%;
      min-width: 0;
    }
  }
}

@media (max-width: 720px) {
  :deep(.n-card__content) {
    padding: 14px;
  }

  :deep(.filter-form) {
    display: grid;
    grid-template-columns: 1fr;
    gap: 0;

    .n-form-item {
      margin-bottom: 12px;
    }

    .n-form-item-blank,
    .n-input,
    .n-base-selection {
      width: 100%;
    }
  }

  .detail-descriptions {
    margin: 14px;
  }

  .content-preview,
  .detail-hero {
    padding-inline: 14px;
  }
}

.detail-hero {
  padding: 24px;
  border-bottom: 1px solid var(--community-border);
  background: var(--community-soft);

  p {
    margin-top: 12px;
    color: var(--community-muted);
    line-height: 1.7;
  }
}

.detail-tags, .detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-meta {
  margin-top: 14px;
  color: var(--community-muted);
  font-size: 12px;

  span + span::before {
    margin-right: 8px;
    color: var(--community-border);
    content: '•';
  }
}

.detail-descriptions {
  margin: 20px 24px;
}

.content-preview {
  padding: 4px 24px 32px;

  &__heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;

    h3 { font-size: 16px; }
  }
}

.safe-html {
  overflow-wrap: anywhere;
  color: var(--community-text);
  font-size: 15px;
  line-height: 1.8;

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
  }

  :deep(pre) {
    overflow: auto;
    padding: 14px;
    border-radius: 8px;
    background: #111827;
    color: #E5E7EB;
  }
}
</style>
