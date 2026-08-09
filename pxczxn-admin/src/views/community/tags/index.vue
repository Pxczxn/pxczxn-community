<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <h1>平台标签</h1>
        <p>维护全站可用的规范标签、URL 标识和启停状态。</p>
      </div>
      <n-button
        v-if="userStore.hasPermission('community:tag:add')"
        type="primary"
        @click="openCreate"
      >
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新建标签
      </n-button>
    </header>

    <n-card>
      <n-form inline :model="filters" class="filter-form" label-placement="left">
        <n-form-item label="关键词">
          <n-input
            v-model:value="filters.keyword"
            clearable
            placeholder="标签名称或标识"
            @keyup.enter="loadData"
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
            <n-button type="primary" @click="loadData">
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
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="rowKey"
        :scroll-x="920"
      />
      <n-empty
        v-if="!loading && !rows.length && !errorMessage"
        description="暂无匹配的平台标签"
        class="inline-empty"
      />
    </n-card>

    <n-modal
      v-model:show="modalVisible"
      preset="card"
      :title="editingId ? '编辑标签' : '新建标签'"
      class="dialog-form-sm"
    >
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
      >
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="标签名称" path="name">
            <n-input v-model:value="form.name" maxlength="32" placeholder="例如：人工智能" />
          </n-form-item-gi>
          <n-form-item-gi label="URL 标识" path="slug">
            <n-input v-model:value="form.slug" maxlength="48" placeholder="例如：artificial-intelligence" />
          </n-form-item-gi>
        </n-grid>
        <n-form-item v-if="editingId" label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio-button value="ACTIVE">启用</n-radio-button>
            <n-radio-button value="HIDDEN">隐藏</n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="说明" path="description">
          <n-input
            v-model:value="form.description"
            type="textarea"
            maxlength="200"
            show-count
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="说明标签的适用内容范围"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submit">
            保存标签
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NSpace,
  NTag,
  useDialog,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  AddOutline,
  CreateOutline,
  RefreshOutline,
  SearchOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { communityApi, type PlatformTag } from '@/api/community'
import { statusLabel, statusTone } from '@/utils/community'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const message = useMessage()
const dialog = useDialog()
const formRef = ref<FormInst | null>(null)
const rows = ref<PlatformTag[]>([])
const loading = ref(false)
const errorMessage = ref('')
const modalVisible = ref(false)
const submitting = ref(false)
const editingId = ref('')
const filters = reactive({
  keyword: '',
  status: null as string | null
})
const form = reactive({
  name: '',
  slug: '',
  description: '',
  status: 'ACTIVE'
})

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '隐藏', value: 'HIDDEN' }
]
const rules: FormRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: ['blur', 'input'] },
    { min: 1, max: 32, message: '标签名称最多 32 个字符', trigger: ['blur', 'input'] }
  ],
  slug: [
    { required: true, message: '请输入 URL 标识', trigger: ['blur', 'input'] },
    {
      pattern: /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
      message: '仅支持小写字母、数字与中划线',
      trigger: ['blur', 'input']
    }
  ]
}

const columns: DataTableColumns<PlatformTag> = [
  {
    title: '标签',
    key: 'name',
    minWidth: 180,
    render: row => h('div', { class: 'identity-cell' }, [
      h('strong', row.name),
      h('span', row.slug)
    ])
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
  { title: '使用次数', key: 'usageCount', width: 110 },
  {
    title: '说明',
    key: 'description',
    minWidth: 280,
    ellipsis: { tooltip: true },
    render: row => row.description || '—'
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render: row => h(NSpace, { size: 4 }, {
      default: () => [
        userStore.hasPermission('community:tag:edit')
          ? h(NButton, {
              size: 'small',
              quaternary: true,
              type: 'primary',
              onClick: () => openEdit(row)
            }, { icon: () => h(CreateOutline), default: () => '编辑' })
          : null,
        userStore.hasPermission('community:tag:delete')
          ? h(NButton, {
              size: 'small',
              quaternary: true,
              type: 'error',
              disabled: row.usageCount > 0,
              onClick: () => remove(row)
            }, { icon: () => h(TrashOutline), default: () => '删除' })
          : null
      ]
    })
  }
]

const rowKey = (row: PlatformTag) => row.tagId

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    rows.value = await communityApi.tags({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '标签列表加载失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  filters.keyword = ''
  filters.status = null
  loadData()
}

function resetForm() {
  editingId.value = ''
  form.name = ''
  form.slug = ''
  form.description = ''
  form.status = 'ACTIVE'
}

function openCreate() {
  resetForm()
  modalVisible.value = true
}

function openEdit(tag: PlatformTag) {
  editingId.value = tag.tagId
  form.name = tag.name
  form.slug = tag.slug
  form.description = tag.description || ''
  form.status = tag.status
  modalVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingId.value) {
      await communityApi.updateTag(editingId.value, { ...form })
      message.success('标签更新成功')
    } else {
      await communityApi.createTag({
        name: form.name,
        slug: form.slug,
        description: form.description
      })
      message.success('标签创建成功')
    }
    modalVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function remove(tag: PlatformTag) {
  dialog.warning({
    title: '删除标签',
    content: `确认删除“${tag.name}”吗？该操作不可撤销。`,
    positiveText: '确认删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await communityApi.deleteTag(tag.tagId)
      message.success('标签已删除')
      await loadData()
    }
  })
}

onMounted(loadData)
</script>
