<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">CONTENT POLICY</div>
        <h1>内容规则</h1>
        <p>维护关键词、内容范围、风险等级与自动处理动作。</p>
      </div>
      <n-button type="primary" @click="openCreate">新增规则</n-button>
    </header>

    <n-card>
      <n-space style="margin-bottom: 16px">
        <n-select
          v-model:value="status"
          :options="statusOptions"
          clearable
          placeholder="全部状态"
          class="filter-select"
          @update:value="load"
        />
        <n-button @click="load">刷新</n-button>
      </n-space>
      <n-data-table
        :data="rows"
        :columns="columns"
        :loading="loading"
        :row-key="(row: ContentRule) => row.id"
      />
    </n-card>

    <n-modal
      v-model:show="visible"
      preset="card"
      :title="editing ? '编辑规则' : '新增规则'"
      class="dialog-form-md"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="top">
        <n-form-item label="关键词" path="keyword">
          <n-input v-model:value="form.keyword" :disabled="!!editing" />
        </n-form-item>
        <n-form-item label="内容范围" path="scopes">
          <n-checkbox-group v-model:value="form.scopes">
            <n-space>
              <n-checkbox value="ARTICLE" label="文章" />
              <n-checkbox value="COMMENT" label="评论" />
              <n-checkbox value="MOMENT" label="动态" />
            </n-space>
          </n-checkbox-group>
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="风险等级" path="riskLevel">
            <n-select v-model:value="form.riskLevel" :options="riskOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="命中动作" path="hitAction">
            <n-select v-model:value="form.hitAction" :options="actionOptions" />
          </n-form-item-gi>
        </n-grid>
        <n-form-item label="说明">
          <n-input v-model:value="form.description" type="textarea" :maxlength="500" />
        </n-form-item>
        <n-space justify="end">
          <n-button @click="visible = false">取消</n-button>
          <n-button type="primary" @click="save">保存</n-button>
        </n-space>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NTag,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  useDialog,
  useMessage
} from 'naive-ui'
import { communityApi, type ContentRule } from '@/api/community'

interface ContentRuleForm {
  keyword: string
  scopes: string[]
  riskLevel: string
  hitAction: string
  description: string
}

const rows = ref<ContentRule[]>([])
const loading = ref(false)
const visible = ref(false)
const editing = ref<ContentRule | null>(null)
const status = ref<string | null>(null)
const formRef = ref<FormInst | null>(null)
const message = useMessage()
const dialog = useDialog()

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
]
const riskOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
  .map((value) => ({ label: value, value }))
const actionOptions = ['WARN', 'MANUAL_REVIEW', 'BLOCK']
  .map((value) => ({ label: value, value }))

const form = reactive<ContentRuleForm>({
  keyword: '',
  scopes: ['ARTICLE', 'COMMENT', 'MOMENT'],
  riskLevel: 'MEDIUM',
  hitAction: 'WARN',
  description: ''
})
const formRules: FormRules = {
  keyword: [{ required: true, message: '请填写关键词', trigger: ['input', 'blur'] }],
  scopes: [{ type: 'array', required: true, message: '请选择至少一个内容范围', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  hitAction: [{ required: true, message: '请选择命中动作', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    rows.value = await communityApi.contentRules(status.value || undefined)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    keyword: '',
    scopes: ['ARTICLE', 'COMMENT', 'MOMENT'],
    riskLevel: 'MEDIUM',
    hitAction: 'WARN',
    description: ''
  })
}

function openCreate() {
  editing.value = null
  resetForm()
  visible.value = true
}

function edit(row: ContentRule) {
  editing.value = row
  Object.assign(form, {
    keyword: row.keyword,
    scopes: row.contentScopes.split(','),
    riskLevel: row.riskLevel,
    hitAction: row.hitAction,
    description: row.description || ''
  })
  visible.value = true
}

async function save() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const payload = {
    keyword: form.keyword.trim(),
    contentScopes: form.scopes.join(','),
    riskLevel: form.riskLevel,
    hitAction: form.hitAction,
    description: form.description.trim() || null
  }
  if (editing.value) {
    await communityApi.updateContentRule(editing.value.id, payload)
  } else {
    await communityApi.createContentRule({ ...payload, status: 'ACTIVE', sortOrder: 0 })
  }
  visible.value = false
  await load()
  message.success('已保存')
}

function remove(row: ContentRule) {
  dialog.warning({
    title: '删除规则',
    content: `确认删除“${row.keyword}”吗？`,
    positiveText: '删除',
    negativeText: '取消',
    async onPositiveClick() {
      await communityApi.deleteContentRule(row.id)
      await load()
    }
  })
}

const columns: DataTableColumns<ContentRule> = [
  { title: '关键词', key: 'keyword' },
  { title: '范围', key: 'contentScopes' },
  { title: '风险', key: 'riskLevel' },
  { title: '动作', key: 'hitAction' },
  {
    title: '状态',
    key: 'status',
    render: (row) => h(NTag, { type: row.status === 'ACTIVE' ? 'success' : 'default' }, {
      default: () => row.status
    })
  },
  {
    title: '操作',
    key: 'actions',
    render: (row) => h('div', { class: 'actions' }, [
      h(NButton, { size: 'small', onClick: () => edit(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', onClick: () => remove(row) }, { default: () => '删除' })
    ])
  }
]

onMounted(load)
</script>

<style scoped>
.actions {
  display: flex;
  gap: 8px;
}
</style>
