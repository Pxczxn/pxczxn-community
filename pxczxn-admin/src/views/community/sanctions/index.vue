<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">COMMUNITY GOVERNANCE</div>
        <h1>社区处置</h1>
        <p>仅处理社区违规后的警告和内容能力限制；账号安全、账号控制及账号清理由“账号操作中心”统一处理。</p>
      </div>
    </header>

    <n-tabs v-model:value="activeTab" type="line" class="governance-tabs">
      <n-tab-pane name="sanction" tab="发起社区处置">
        <n-card title="社区能力限制">
          <n-alert type="info" :show-icon="true" style="margin-bottom: 16px">
            社区处置不会冻结账号或禁止登录。需要控制账号登录状态，请转到“账号操作中心 → 账号控制”。
          </n-alert>
          <n-form class="sanction-form" label-placement="top">
            <n-form-item label="用户 ID" required><n-input v-model:value="userId" inputmode="numeric" placeholder="输入社区用户 ID" /></n-form-item>
            <n-form-item label="处置类型" required><n-select v-model:value="sanctionType" :options="types" /></n-form-item>
            <n-form-item label="处置原因" required><n-select v-model:value="reasonCode" :options="reasonOptions" /></n-form-item>
            <n-form-item v-if="isTimed" label="到期时间" required><n-date-picker v-model:value="expiresAt" type="datetime" clearable /></n-form-item>
            <n-form-item label="关联举报 ID"><n-input-number v-model:value="sourceReportId" :min="1" :show-button="false" placeholder="可选" /></n-form-item>
            <n-form-item class="form-actions" label=" "><n-space><n-button type="primary" :loading="issuing" @click="issue">发起处置</n-button><n-button :loading="loading" @click="load">查询记录</n-button></n-space></n-form-item>
          </n-form>
          <n-form-item label="处理备注"><n-input v-model:value="reasonNote" type="textarea" :maxlength="1000" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="向用户说明社区能力限制的原因（可选）" /></n-form-item>
        </n-card>
      </n-tab-pane>

      <n-tab-pane name="history" tab="处置记录">
        <n-card title="社区处置记录">
          <n-space align="center" style="margin-bottom: 16px"><n-input v-model:value="userId" inputmode="numeric" placeholder="输入社区用户 ID" style="width: 260px" /><n-button :loading="loading" @click="load">查询记录</n-button></n-space>
          <n-data-table :data="rows" :columns="columns" :loading="loading" :row-key="(row: CommunitySanction) => row.id" :bordered="false" />
        </n-card>
      </n-tab-pane>
    </n-tabs>
    <n-alert v-if="error" type="error" closable style="margin-top: 16px" @close="error = ''">{{ error }}</n-alert>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { NButton, type DataTableColumns, useDialog, useMessage } from 'naive-ui'
import { communityApi, type CommunitySanction } from '@/api/community'

const timedTypes = new Set(['RATE_LIMIT', 'COMMENT_BAN', 'MOMENT_BAN', 'SUBMISSION_BAN', 'PUBLISH_SUSPEND', 'MESSAGE_BAN'])
const types = [
  ['WARNING', '警告'], ['RATE_LIMIT', '限制互动'], ['COMMENT_BAN', '禁止评论'], ['MOMENT_BAN', '禁止发布动态'],
  ['SUBMISSION_BAN', '禁止投稿'], ['PUBLISH_SUSPEND', '禁止发布文章'], ['MESSAGE_BAN', '禁止私信']
].map(([value, label]) => ({ value, label }))
const reasonOptions = [
  ['POLICY_VIOLATION', '违反社区规范'], ['SPAM_OR_BOT', '垃圾内容或机器人'], ['HARASSMENT', '骚扰或攻击行为'],
  ['COPYRIGHT', '侵权'], ['FRAUD', '欺诈风险'], ['OTHER', '其他原因']
].map(([value, label]) => ({ value, label }))
const sanctionLabels = Object.fromEntries(types.map(({ value, label }) => [value, label]))
const reasonLabels = Object.fromEntries(reasonOptions.map(({ value, label }) => [value, label]))
const route = useRoute(), message = useMessage(), dialog = useDialog()
const activeTab = ref('sanction'), userId = ref(''), sanctionType = ref('WARNING'), reasonCode = ref('POLICY_VIOLATION'), reasonNote = ref(''), sourceReportId = ref<number | null>(null), expiresAt = ref<number | null>(null), rows = ref<CommunitySanction[]>([]), loading = ref(false), issuing = ref(false), error = ref('')
const isTimed = computed(() => timedTypes.has(sanctionType.value))
const formatDate = (value: number) => new Date(value).toISOString().slice(0, 19).replace('T', ' ')
const describeError = (value: unknown) => value instanceof Error ? value.message : '请求失败，请稍后重试'
async function load() { if (!userId.value) { error.value = '请先输入用户 ID'; return }; loading.value = true; error.value = ''; try { rows.value = await communityApi.sanctions(userId.value) } catch (cause) { error.value = describeError(cause) } finally { loading.value = false } }
async function issue() {
  if (!userId.value) { error.value = '请先输入用户 ID'; return }
  if (isTimed.value && !expiresAt.value) { error.value = '限时处置必须填写到期时间'; return }
  issuing.value = true; error.value = ''
  try { await communityApi.issueSanction({ targetUserId: userId.value, sanctionType: sanctionType.value, reasonCode: reasonCode.value, reasonNote: reasonNote.value.trim() || undefined, sourceReportId: sourceReportId.value || undefined, expiresAt: isTimed.value && expiresAt.value ? formatDate(expiresAt.value) : undefined }); message.success('社区处置已发起'); expiresAt.value = null; await load() } catch (cause) { error.value = describeError(cause) } finally { issuing.value = false }
}
function revoke(row: CommunitySanction) { dialog.warning({ title: '撤销社区处置', content: `确认撤销 ${sanctionLabels[row.type] || row.type} 吗？`, positiveText: '撤销', negativeText: '取消', onPositiveClick: async () => { try { await communityApi.revokeSanction(row.id, '运营撤销社区处置'); message.success('处置已撤销'); await load() } catch (cause) { error.value = describeError(cause) } } }) }
onMounted(() => { const targetUserId = route.query.userId; if (typeof targetUserId === 'string' && /^\d+$/.test(targetUserId)) { userId.value = targetUserId; activeTab.value = 'sanction'; void load() } })
const columns: DataTableColumns<CommunitySanction> = [
  { title: '处置类型', key: 'type', minWidth: 150, render: row => sanctionLabels[row.type] || row.type }, { title: '原因', key: 'reasonCode', minWidth: 160, render: row => reasonLabels[row.reasonCode] || row.reasonCode }, { title: '说明', key: 'reasonNote', minWidth: 220, render: row => row.reasonNote || '-' }, { title: '状态', key: 'status', minWidth: 100 }, { title: '到期时间', key: 'expiresAt', minWidth: 180, render: row => row.expiresAt ? row.expiresAt.replace('T', ' ') : '永久' }, { title: '操作', key: 'actions', width: 100, render: row => h(NButton, { size: 'small', disabled: row.status !== 'ACTIVE', onClick: () => revoke(row) }, { default: () => '撤销' }) }
]
</script>

<style scoped>
.community-page { padding: 24px; }.page-heading { margin-bottom: 24px; }.page-eyebrow { color: var(--n-text-color-3); font-size: 12px; }.page-heading h1 { margin: 8px 0; }.page-heading p { color: var(--n-text-color-3); margin: 0; }.sanction-form { display: grid; gap: 4px 16px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); align-items: end; }.form-actions { align-self: end; }.governance-tabs :deep(.n-tabs-nav) { margin-bottom: 16px; }
</style>
