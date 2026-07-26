<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">INTERACTION INVENTORY</div>
        <h1>互动查询</h1>
        <p>只读检索点赞、收藏和关注关系；收藏夹名称与结构不会进入运营查询结果。</p>
      </div>
      <n-button :loading="loading" @click="loadData">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新
      </n-button>
    </header>

    <div class="interaction-overview">
      <n-card v-for="item in overviewCards" :key="item.value" size="small">
        <button
          type="button"
          class="overview-button"
          :class="{ active: filters.interactionType === item.value }"
          @click="switchType(item.value)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ filters.interactionType === item.value ? compactNumber(pagination.itemCount) : '查询' }}</strong>
          <small>{{ item.description }}</small>
        </button>
      </n-card>
    </div>

    <n-card>
      <n-alert type="info" class="privacy-alert" :bordered="false">
        本页展示关系本身和公开目标摘要，不展示用户私密收藏夹、喜欢列表可见性或其他个人偏好结构。
      </n-alert>

      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="互动类型">
          <n-select
            v-model:value="filters.interactionType"
            :options="interactionOptions"
            style="width: 140px"
            @update:value="search"
          />
        </n-form-item>
        <n-form-item label="目标类型">
          <n-select
            v-model:value="filters.targetType"
            :options="targetOptions"
            clearable
            filterable
            placeholder="全部类型"
            style="width: 150px"
          />
        </n-form-item>
        <n-form-item label="目标 ID">
          <n-input v-model:value="filters.targetId" clearable placeholder="Snowflake ID" />
        </n-form-item>
        <n-form-item label="用户 ID">
          <n-input v-model:value="filters.actorUserId" clearable placeholder="行为用户 ID" />
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
        :scroll-x="1320"
        @update:page="changePage"
        @update:page-size="changePageSize"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import { RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import {
  communityApi,
  type CommunityInteraction
} from '@/api/community'
import {
  compactNumber,
  formatDateTime,
  statusLabel,
  statusTone
} from '@/utils/community'

type InteractionType = CommunityInteraction['interactionType']

const rows = ref<CommunityInteraction[]>([])
const loading = ref(false)
const errorMessage = ref('')
const filters = reactive({
  interactionType: 'LIKE' as InteractionType,
  targetType: null as string | null,
  targetId: '',
  actorUserId: ''
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const interactionOptions = [
  { label: '点赞', value: 'LIKE' },
  { label: '收藏', value: 'FAVORITE' },
  { label: '关注', value: 'FOLLOW' }
]
const overviewCards = [
  { label: '点赞关系', value: 'LIKE' as InteractionType, description: '文章、动态与评论' },
  { label: '收藏关系', value: 'FAVORITE' as InteractionType, description: '不暴露收藏夹结构' },
  { label: '关注关系', value: 'FOLLOW' as InteractionType, description: '通知级别与特别关注' }
]
const targetOptions = computed(() => filters.interactionType === 'FOLLOW'
  ? [
      { label: '博客', value: 'BLOG' },
      { label: '平台标签', value: 'PLATFORM_TAG' },
      { label: '系列', value: 'SERIES' }
    ]
  : [
      { label: '文章', value: 'ARTICLE' },
      { label: '动态', value: 'MOMENT' },
      ...(filters.interactionType === 'LIKE'
        ? [{ label: '评论', value: 'COMMENT' }]
        : [])
    ]
)

const columns: DataTableColumns<CommunityInteraction> = [
  {
    title: '行为用户',
    key: 'actorUsername',
    minWidth: 230,
    fixed: 'left',
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.actorDisplayName || row.actorUsername || '社区用户'),
      h('span', row.actorUsername ? `@${row.actorUsername} · #${row.actorUserId}` : `#${row.actorUserId}`)
    ])
  },
  {
    title: '互动',
    key: 'interactionType',
    width: 100,
    render: row => h(NTag, {
      size: 'small',
      bordered: false,
      type: statusTone(row.interactionType)
    }, { default: () => statusLabel(row.interactionType) })
  },
  {
    title: '目标',
    key: 'targetTitle',
    minWidth: 300,
    ellipsis: { tooltip: true },
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.targetTitle || `${statusLabel(row.targetType)} #${row.targetId}`),
      h('span', `${statusLabel(row.targetType)} · #${row.targetId}`)
    ])
  },
  {
    title: '通知级别',
    key: 'notificationLevel',
    width: 130,
    render: row => row.interactionType === 'FOLLOW'
      ? statusLabel(row.notificationLevel)
      : '—'
  },
  {
    title: '特别关注',
    key: 'specialFollow',
    width: 110,
    render: row => row.interactionType === 'FOLLOW'
      ? h(NTag, {
          size: 'small',
          bordered: false,
          type: row.specialFollow ? 'warning' : 'default'
        }, { default: () => row.specialFollow ? '是' : '否' })
      : '—'
  },
  {
    title: '关系 ID',
    key: 'id',
    width: 180
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 170,
    render: row => formatDateTime(row.createdAt)
  },
  {
    title: '更新时间',
    key: 'updatedAt',
    width: 170,
    render: row => formatDateTime(row.updatedAt)
  }
]

function rowKey(row: CommunityInteraction) {
  return row.id
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await communityApi.interactions({
      interactionType: filters.interactionType,
      targetType: filters.targetType || undefined,
      targetId: filters.targetId || undefined,
      actorUserId: filters.actorUserId || undefined,
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    rows.value = result.list
    pagination.itemCount = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '互动关系加载失败'
  } finally {
    loading.value = false
  }
}

function switchType(value: InteractionType) {
  filters.interactionType = value
  filters.targetType = null
  pagination.page = 1
  loadData()
}

function search() {
  pagination.page = 1
  loadData()
}

function reset() {
  filters.targetType = null
  filters.targetId = ''
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
.interaction-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.overview-button {
  display: grid;
  width: 100%;
  padding: 4px;
  border: 0;
  background: transparent;
  color: var(--community-muted);
  cursor: pointer;
  text-align: left;

  span {
    font-size: 12px;
    font-weight: 600;
  }

  strong {
    margin-top: 8px;
    color: var(--community-text);
    font-size: 24px;
  }

  small {
    margin-top: 3px;
    font-size: 11px;
  }

  &.active span,
  &:hover span {
    color: var(--primary-color);
  }
}

.privacy-alert {
  margin-bottom: 16px;
}

@media (max-width: 760px) {
  .interaction-overview {
    grid-template-columns: 1fr;
  }
}
</style>
