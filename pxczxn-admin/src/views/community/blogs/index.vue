<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">BLOG SPACES</div>
        <h1>博客管理</h1>
        <p>查看个人博客与团队博客的主体、内容规模和运营状态。</p>
      </div>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="博客名、标识或所有者"
            @keyup.enter="search"
          />
        </n-form-item>
        <n-form-item label="博客类型">
          <n-select
            v-model:value="filters.blogType"
            :options="typeOptions"
            clearable
            placeholder="全部类型"
            class="filter-select"
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

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :scroll-x="1160"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { communityApi, type CommunityBlog } from '@/api/community'
import { compactNumber, formatDateTime, statusLabel, statusTone } from '@/utils/community'

const rows = ref<CommunityBlog[]>([])
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({
  keyword: '',
  blogType: null as string | null,
  status: null as string | null
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const typeOptions = [
  { label: '个人博客', value: 'PERSONAL' },
  { label: '团队博客', value: 'TEAM' }
]
const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' }
]

const columns: DataTableColumns<CommunityBlog> = [
  {
    title: '博客',
    key: 'name',
    minWidth: 230,
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.name),
      h('span', `/${row.slug}`)
    ])
  },
  {
    title: '类型',
    key: 'blogType',
    width: 110,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: row.blogType === 'TEAM' ? 'info' : 'default'
    }, { default: () => statusLabel(row.blogType) })
  },
  {
    title: '所有者',
    key: 'ownerUsername',
    width: 150,
    render: row => row.ownerUsername ? `@${row.ownerUsername}` : '团队主体'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.status)
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: '文章',
    key: 'articleCount',
    width: 100,
    render: row => compactNumber(row.articleCount)
  },
  {
    title: '关注者',
    key: 'followerCount',
    width: 100,
    render: row => compactNumber(row.followerCount)
  },
  {
    title: '简介',
    key: 'summary',
    minWidth: 240,
    ellipsis: { tooltip: true },
    render: row => row.summary || '—'
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 170,
    render: row => formatDateTime(row.createdAt)
  }
]

const rowKey = (row: CommunityBlog) => row.id

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.blogs({
      keyword: filters.keyword || undefined,
      blogType: filters.blogType || undefined,
      status: filters.status || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '博客列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.page = 1
  loadData()
}

function reset() {
  filters.keyword = ''
  filters.blogType = null
  filters.status = null
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
