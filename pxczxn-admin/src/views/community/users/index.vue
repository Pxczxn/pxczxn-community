<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">MEMBERS</div>
        <h1>社区用户</h1>
        <p>查询社区账号、认证状态、个人博客和最近登录情况。</p>
      </div>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="用户名、昵称或邮箱"
            @keyup.enter="search"
          />
        </n-form-item>
        <n-form-item label="账号状态">
          <n-select
            v-model:value="filters.status"
            :options="statusOptions"
            clearable
            placeholder="全部状态"
            class="filter-select"
          />
        </n-form-item>
        <n-form-item label="认证状态">
          <n-select
            v-model:value="filters.verificationStatus"
            :options="verificationOptions"
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
        :scroll-x="1050"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, type DataTableColumns } from 'naive-ui'
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import { communityApi, type CommunityUser } from '@/api/community'
import { formatDateTime, statusLabel, statusTone } from '@/utils/community'

const rows = ref<CommunityUser[]>([])
const loading = ref(false)
const errorMessage = ref('')
const router = useRouter()
const filters = reactive({
  keyword: '',
  status: null as string | null,
  verificationStatus: null as string | null
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const statusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '禁用', value: 'DISABLED' },
  { label: '封禁', value: 'BANNED' }
]
const verificationOptions = [
  { label: '已认证', value: 'VERIFIED' },
  { label: '待认证', value: 'PENDING' },
  { label: '未认证', value: 'UNVERIFIED' }
]

function maskedValue(value: string, visiblePrefix = 2) {
  if (value.length <= visiblePrefix + 1) return `${value.slice(0, 1)}***`
  return `${value.slice(0, visiblePrefix)}***${value.slice(-1)}`
}

function maskedEmail(email: string) {
  const [local = '', domain = ''] = email.split('@')
  return `${maskedValue(local)}@${maskedValue(domain, 1)}`
}

function sensitiveValue(masked: string, full: string) {
  return h('span', { class: 'sensitive-reveal', title: '悬停查看完整信息' }, [
    h('span', { class: 'sensitive-reveal__masked' }, masked),
    h('span', { class: 'sensitive-reveal__full' }, full)
  ])
}

const columns: DataTableColumns<CommunityUser> = [
  {
    title: '用户',
    key: 'displayName',
    width: 160,
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.displayName || row.username),
      sensitiveValue(`@${maskedValue(row.username)}`, `@${row.username}`)
    ])
  },
  {
    title: '邮箱', key: 'email', width: 175,
    render: row => sensitiveValue(maskedEmail(row.email), row.email)
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.status)
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: '认证',
    key: 'verificationStatus',
    width: 80,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.verificationStatus)
    }, { default: () => statusLabel(row.verificationStatus) })
  },
  {
    title: '关联博客',
    key: 'personalBlogName',
    minWidth: 150,
    render: row => row.personalBlogName || '—'
  },
  {
    title: '最近登录',
    key: 'lastLoginAt',
    width: 150,
    render: row => formatDateTime(row.lastLoginAt)
  },
  {
    title: '注册时间',
    key: 'createdAt',
    width: 150,
    render: row => formatDateTime(row.createdAt)
  },
  {
    title: '账号操作', key: 'actions', width: 100, fixed: 'right',
    render: row => h(NButton, {
      size: 'small',
      onClick: () => router.push({ path: '/community/account-enforcements', query: { userId: row.id } })
    }, { default: () => '账号操作' })
  }
]

const rowKey = (row: CommunityUser) => row.id

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.users({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      verificationStatus: filters.verificationStatus || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '用户列表加载失败'
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
  filters.status = null
  filters.verificationStatus = null
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
