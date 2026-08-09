<template>
  <div class="community-page">
    <header class="page-heading">
      <div><h1>系列审核</h1><p>审核创作者提交的系列；通过后才会在社区公开展示。</p></div>
      <n-button :loading="loading" @click="loadData"><template #icon><n-icon><RefreshOutline /></n-icon></template>刷新队列</n-button>
    </header>
    <n-card>
      <n-alert v-if="errorMessage" type="error" class="table-alert">{{ errorMessage }}<template #action><n-button size="small" @click="loadData">重试</n-button></template></n-alert>
      <n-empty v-if="!loading && rows.length === 0" description="暂无待审核系列"><template #icon><n-icon><CheckmarkDoneOutline /></n-icon></template></n-empty>
      <n-data-table v-else :columns="columns" :data="rows" :loading="loading" :row-key="(row: CommunitySeries) => row.id" :scroll-x="1040" />
    </n-card>
    <n-modal v-model:show="visible" preset="card" :title="action === 'approve' ? '通过系列审核' : '驳回系列审核'" class="dialog-form-md">
      <template v-if="current"><n-descriptions :column="1" bordered size="small"><n-descriptions-item label="系列">{{ current.title }}</n-descriptions-item><n-descriptions-item label="所属博客">{{ current.blogName || '未命名博客' }} <small class="muted">（{{ blogTypeLabel(current.blogType) }}）</small></n-descriptions-item><n-descriptions-item label="章节">{{ current.chapters.length }} 篇</n-descriptions-item><n-descriptions-item label="连载状态">{{ statusLabel(current.serializationStatus) }}</n-descriptions-item></n-descriptions><n-form style="margin-top: 16px"><n-form-item label="审核说明"><n-input v-model:value="comment" type="textarea" :maxlength="1000" show-count :rows="4" placeholder="可选填写审核说明" /></n-form-item></n-form><n-alert v-if="action === 'approve'" type="info" :bordered="false">通过后，已发布章节会按设置的顺序在社区系列页公开。</n-alert></template>
      <template #footer><n-space justify="end"><n-button @click="visible = false">取消</n-button><n-button :loading="saving" :type="action === 'approve' ? 'primary' : 'error'" @click="decide">确认{{ action === 'approve' ? '通过' : '驳回' }}</n-button></n-space></template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { CheckmarkDoneOutline, RefreshOutline } from '@vicons/ionicons5'
import { NButton, NIcon, NSpace, NTag, useMessage } from 'naive-ui'
import { communityApi, type CommunitySeries } from '@/api/community'
import { formatDateTime } from '@/utils/community'

const message = useMessage(); const loading = ref(false); const saving = ref(false); const errorMessage = ref(''); const rows = ref<CommunitySeries[]>([]); const visible = ref(false); const current = ref<CommunitySeries | null>(null); const action = ref<'approve' | 'reject'>('approve'); const comment = ref('')
const statusLabel = (value: CommunitySeries['serializationStatus']) => ({ ONGOING: '连载中', COMPLETED: '已完结', PAUSED: '暂缓更新' }[value])
const blogTypeLabel = (value: string) => ({ PERSONAL: '个人博客', TEAM: '团队博客' }[value] || value)
const columns = [{ title: '系列', key: 'title', minWidth: 220, render: (row: CommunitySeries) => h('div', [h('strong', row.title), h('small', { class: 'muted' }, ` ${row.slug}`)]) }, { title: '博客', key: 'blogName', width: 180, render: (row: CommunitySeries) => h('span', [h('strong', row.blogName || '未命名博客'), h('small', { class: 'muted' }, ` ${blogTypeLabel(row.blogType)}`)]) }, { title: '章节', key: 'chapters', width: 100, render: (row: CommunitySeries) => `${row.chapters.length} 篇` }, { title: '连载状态', key: 'serializationStatus', width: 120, render: (row: CommunitySeries) => h(NTag, { size: 'small', type: row.serializationStatus === 'COMPLETED' ? 'success' : row.serializationStatus === 'PAUSED' ? 'warning' : 'info' }, { default: () => statusLabel(row.serializationStatus) }) }, { title: '提交时间', key: 'updatedAt', width: 175, render: (row: CommunitySeries) => formatDateTime(row.updatedAt) }, { title: '操作', key: 'actions', width: 180, fixed: 'right' as const, render: (row: CommunitySeries) => h(NSpace, { size: 'small' }, { default: () => [h(NButton, { size: 'small', type: 'success', onClick: () => open(row, 'approve') }, { default: () => '通过' }), h(NButton, { size: 'small', type: 'error', onClick: () => open(row, 'reject') }, { default: () => '驳回' })] }) }]
async function loadData() { loading.value = true; errorMessage.value = ''; try { rows.value = await communityApi.series() } catch (error: unknown) { errorMessage.value = error instanceof Error ? error.message : '加载失败' } finally { loading.value = false } }
function open(row: CommunitySeries, next: 'approve' | 'reject') { current.value = row; action.value = next; comment.value = ''; visible.value = true }
async function decide() { if (!current.value) return; saving.value = true; try { await communityApi.decideSeries(current.value.id, action.value, current.value.lockVersion, comment.value || undefined); message.success('审核结果已保存'); visible.value = false; await loadData() } catch (error: unknown) { message.error(error instanceof Error ? error.message : '审核失败') } finally { saving.value = false } }
onMounted(() => { void loadData() })
</script>
