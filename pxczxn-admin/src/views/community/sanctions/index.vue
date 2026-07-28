<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">SANCTIONS</div>
        <h1>处罚体系</h1>
        <p>发放、查询和撤销社区用户处罚，所有变更均保留不可修改的处理历史。</p>
      </div>
    </header>

    <n-card title="用户处罚">
      <n-form class="sanction-form" label-placement="top">
        <n-form-item label="用户 ID" required>
          <n-input-number v-model:value="userId" :min="1" :show-button="false" placeholder="输入社区用户 ID" />
        </n-form-item>
        <n-form-item label="处罚类型" required>
          <n-select v-model:value="sanctionType" :options="types" />
        </n-form-item>
        <n-form-item label="原因代码" required>
          <n-input v-model:value="reasonCode" placeholder="例如 POLICY_VIOLATION" />
        </n-form-item>
        <n-form-item v-if="isTimed" label="到期时间" required>
          <n-date-picker v-model:value="expiresAt" type="datetime" clearable />
        </n-form-item>
        <n-form-item label="关联举报 ID">
          <n-input-number v-model:value="sourceReportId" :min="1" :show-button="false" placeholder="可选" />
        </n-form-item>
        <n-form-item class="form-actions" label=" ">
          <n-space>
            <n-button type="primary" :loading="issuing" @click="issue">发放处罚</n-button>
            <n-button :loading="loading" @click="load">查询记录</n-button>
          </n-space>
        </n-form-item>
      </n-form>

      <n-form-item label="处理备注">
        <n-input v-model:value="reasonNote" type="textarea" :maxlength="1000" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="向用户说明处罚原因（可选）" />
      </n-form-item>

      <n-alert v-if="error" type="error" closable style="margin-bottom: 16px" @close="error = ''">
        {{ error }}
      </n-alert>

      <n-data-table
        :data="rows"
        :columns="columns"
        :loading="loading"
        :row-key="(row: CommunitySanction) => row.id"
        :bordered="false"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import {
  NButton,
  NDatePicker,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  type DataTableColumns,
  useDialog,
  useMessage
} from 'naive-ui'
import { communityApi, type CommunitySanction } from '@/api/community'

const timedTypes = new Set([
  'RATE_LIMIT',
  'COMMENT_BAN',
  'MOMENT_BAN',
  'SUBMISSION_BAN',
  'PUBLISH_SUSPEND',
  'LOGIN_SUSPEND'
])
const types = [
  'WARNING',
  'RATE_LIMIT',
  'COMMENT_BAN',
  'MOMENT_BAN',
  'SUBMISSION_BAN',
  'PUBLISH_SUSPEND',
  'LOGIN_SUSPEND',
  'PERMANENT_BAN'
].map((value) => ({ label: value, value }))

const userId = ref<number | null>(null)
const sanctionType = ref('WARNING')
const reasonCode = ref('POLICY_VIOLATION')
const reasonNote = ref('')
const sourceReportId = ref<number | null>(null)
const expiresAt = ref<number | null>(null)
const rows = ref<CommunitySanction[]>([])
const loading = ref(false)
const issuing = ref(false)
const error = ref('')
const message = useMessage()
const dialog = useDialog()
const isTimed = computed(() => timedTypes.has(sanctionType.value))

function asLocalDateTime(value: number) {
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}`
}

function describeError(value: unknown) {
  return value instanceof Error ? value.message : '请求失败，请稍后重试'
}

async function load() {
  if (!userId.value) {
    error.value = '请先输入用户 ID'
    return
  }
  loading.value = true
  error.value = ''
  try {
    rows.value = await communityApi.sanctions(String(userId.value))
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    loading.value = false
  }
}

async function issue() {
  if (!userId.value) {
    error.value = '请先输入用户 ID'
    return
  }
  if (!reasonCode.value.trim()) {
    error.value = '请填写原因代码'
    return
  }
  if (isTimed.value && !expiresAt.value) {
    error.value = '限时处罚必须填写到期时间'
    return
  }
  issuing.value = true
  error.value = ''
  try {
    await communityApi.issueSanction({
      targetUserId: userId.value,
      sanctionType: sanctionType.value,
      reasonCode: reasonCode.value.trim(),
      reasonNote: reasonNote.value.trim() || undefined,
      sourceReportId: sourceReportId.value || undefined,
      expiresAt: isTimed.value && expiresAt.value ? asLocalDateTime(expiresAt.value) : undefined
    })
    message.success('处罚已发放')
    expiresAt.value = null
    await load()
  } catch (cause) {
    error.value = describeError(cause)
  } finally {
    issuing.value = false
  }
}

function revoke(row: CommunitySanction) {
  dialog.warning({
    title: '撤销处罚',
    content: `确认撤销 ${row.type} 处罚吗？`,
    positiveText: '撤销',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        await communityApi.revokeSanction(row.id, '运营撤销处罚')
        message.success('处罚已撤销')
        await load()
      } catch (cause) {
        error.value = describeError(cause)
      }
    }
  })
}

const columns: DataTableColumns<CommunitySanction> = [
  { title: '处罚类型', key: 'type', minWidth: 150 },
  { title: '原因', key: 'reasonCode', minWidth: 160 },
  {
    title: '说明',
    key: 'reasonNote',
    minWidth: 220,
    render: (row) => row.reasonNote || '-'
  },
  { title: '状态', key: 'status', minWidth: 100 },
  {
    title: '到期时间',
    key: 'expiresAt',
    minWidth: 180,
    render: (row) => row.expiresAt ? row.expiresAt.replace('T', ' ') : '永久'
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => h(
      NButton,
      {
        size: 'small',
        disabled: row.status !== 'ACTIVE',
        onClick: () => revoke(row)
      },
      { default: () => '撤销' }
    )
  }
]
</script>

<style scoped>
.community-page { padding: 24px; }
.page-heading { margin-bottom: 24px; }
.page-eyebrow { color: var(--n-text-color-3); font-size: 12px; }
.page-heading h1 { margin: 8px 0; }
.page-heading p { color: var(--n-text-color-3); margin: 0; }
.sanction-form { display: grid; gap: 4px 16px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }
.form-actions { align-self: end; }
</style>
