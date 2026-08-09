<template>
  <div class="community-page">
    <header class="page-heading">
      <div><h1>外部投稿审核</h1><p>平台审核固定的个人文章版本。通过后会新建归属目标团队的文章，原文章不会被转移。</p></div>
      <n-button :loading="loading" @click="loadData"><template #icon><n-icon><RefreshOutline /></n-icon></template>刷新队列</n-button>
    </header>
    <n-card><n-alert v-if="errorMessage" type="error" class="table-alert">{{ errorMessage }}<template #action><n-button size="small" @click="loadData">重试</n-button></template></n-alert><n-empty v-if="!loading && rows.length === 0" description="暂无待平台审核的外部投稿"><template #icon><n-icon><CheckmarkDoneOutline /></n-icon></template></n-empty><n-data-table v-else :columns="columns" :data="rows" :loading="loading" :row-key="(row: TeamSubmission) => row.id" :scroll-x="1240" /></n-card>
    <n-modal v-model:show="visible" preset="card" :title="actionTitle" class="dialog-form-md" :segmented="{ content: 'soft', footer: 'soft' }"><template v-if="current"><n-descriptions :column="1" bordered size="small"><n-descriptions-item label="投稿 ID">{{ current.id }}</n-descriptions-item><n-descriptions-item label="原文章">{{ current.sourceArticleTitle }} #{{ current.sourceArticleId }}</n-descriptions-item><n-descriptions-item label="固定版本">#{{ current.fixedSourceVersionId }}</n-descriptions-item><n-descriptions-item label="目标团队">#{{ current.targetTeamId }}</n-descriptions-item><n-descriptions-item label="提交者">#{{ current.submittedByUserId }}</n-descriptions-item></n-descriptions><n-form style="margin-top: 16px"><n-form-item label="审核说明"><n-input v-model:value="comment" type="textarea" :maxlength="1000" show-count :rows="4" placeholder="可填写审核反馈" /></n-form-item></n-form><n-alert v-if="action === 'approve'" type="info" :bordered="false">确认通过后将从固定快照创建独立团队文章，并保留原作者署名。</n-alert></template><template #footer><n-space justify="end"><n-button @click="visible = false">取消</n-button><n-button :loading="saving" :type="action === 'approve' ? 'primary' : action === 'reject' ? 'error' : 'warning'" @click="decide">确认{{ actionTitle }}</n-button></n-space></template></n-modal>
  </div>
</template>
<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { CheckmarkDoneOutline, RefreshOutline } from '@vicons/ionicons5'
import { NButton, NIcon, NSpace, NTag, useMessage } from 'naive-ui'
import { communityApi, type TeamSubmission } from '@/api/community'
import { formatDateTime } from '@/utils/community'
const message = useMessage(); const loading = ref(false); const saving = ref(false); const errorMessage = ref(''); const rows = ref<TeamSubmission[]>([]); const visible = ref(false); const current = ref<TeamSubmission | null>(null); const action = ref<'approve' | 'revision' | 'reject'>('approve'); const comment = ref('')
const labels: Record<string, string> = { PLATFORM_PENDING: '待平台审核', PLATFORM_PUBLISHING: '发布处理中' }
const actionTitle = computed(() => ({ approve: '通过', revision: '要求修改', reject: '拒绝' })[action.value])
const columns = [{ title: '投稿 ID', key: 'id', width: 150 }, { title: '原文章', key: 'sourceArticleTitle', minWidth: 180, render: (row: TeamSubmission) => h('div', [row.sourceArticleTitle, h('small', { class: 'muted' }, ` #${row.sourceArticleId}`)]) }, { title: '目标团队', key: 'targetTeamId', width: 140 }, { title: '固定版本', key: 'fixedSourceVersionId', width: 150 }, { title: '状态', key: 'status', width: 140, render: (row: TeamSubmission) => h(NTag, { type: 'warning', size: 'small' }, { default: () => labels[row.status] || row.status }) }, { title: '提交时间', key: 'createdAt', width: 175, render: (row: TeamSubmission) => formatDateTime(row.createdAt) }, { title: '操作', key: 'actions', width: 240, fixed: 'right' as const, render: (row: TeamSubmission) => h(NSpace, { size: 'small' }, { default: () => ['approve', 'revision', 'reject'].map((next) => h(NButton, { size: 'small', type: next === 'approve' ? 'success' : next === 'reject' ? 'error' : 'warning', onClick: () => open(row, next as 'approve' | 'revision' | 'reject') }, { default: () => next === 'approve' ? '通过' : next === 'revision' ? '修改' : '拒绝' })) }) }]
async function loadData() { loading.value = true; errorMessage.value = ''; try { rows.value = await communityApi.teamSubmissions() } catch (error: unknown) { errorMessage.value = error instanceof Error ? error.message : '加载失败' } finally { loading.value = false } }
function open(row: TeamSubmission, next: 'approve' | 'revision' | 'reject') { current.value = row; action.value = next; comment.value = ''; visible.value = true }
async function decide() { if (!current.value) return; saving.value = true; try { await communityApi.decideTeamSubmission(current.value.id, action.value, current.value.lockVersion, comment.value || undefined); message.success('审核结果已保存'); visible.value = false; await loadData() } catch (error: unknown) { message.error(error instanceof Error ? error.message : '审核失败') } finally { saving.value = false } }
onMounted(() => { void loadData() })
</script>
