<template>
  <div class="community-page">
    <header class="page-heading"><div class="page-eyebrow">ACCOUNT MANAGEMENT</div><h1>账号管理</h1></header>
    <n-tabs v-model:value="tab" type="line">
      <n-tab-pane name="security" tab="账号安全" />
      <n-tab-pane name="freeze" tab="冻结管理" />
      <n-tab-pane name="deletion" tab="删除申请" />
      <n-tab-pane name="records" tab="操作记录" />
    </n-tabs>
    <n-card v-if="tab !== 'records'" :title="titles[tab]">
      <n-form label-placement="top" class="form-grid">
        <n-form-item label="用户 ID"><n-input v-model:value="form.userId" /></n-form-item>
        <n-form-item label="标准原因"><n-input v-model:value="form.reasonCode" /></n-form-item>
        <n-form-item v-if="tab === 'freeze'" label="到期时间"><n-date-picker v-model:value="form.expiresAt" type="datetime" /></n-form-item>
      </n-form>
      <n-form-item label="用户可见理由"><n-input v-model:value="form.userVisibleReason" type="textarea" /></n-form-item>
      <n-form-item label="内部说明"><n-input v-model:value="form.internalReason" type="textarea" /></n-form-item>
      <n-form-item label="证据说明"><n-input v-model:value="form.evidence" type="textarea" /></n-form-item>
      <n-space v-if="tab === 'security'"><n-button @click="security('logout')">强制下线</n-button><n-button type="warning" @click="security('reset')">强制重置登录凭证</n-button><n-button @click="security('unlock')">解除登录失败锁定</n-button></n-space>
      <n-space v-else-if="tab === 'freeze'"><n-button type="primary" @click="freeze('freeze')">冻结账号</n-button><n-button @click="freeze('extend')">延长冻结</n-button><n-button @click="freeze('release')">提前解除冻结</n-button></n-space>
      <n-space v-else><n-button type="primary" @click="submitDeletion">提交删除申请</n-button></n-space>
    </n-card>
    <n-card v-else title="操作记录"><n-space><n-input v-model:value="queryUserId" placeholder="用户 ID"/><n-button @click="load">查询</n-button></n-space><n-data-table :columns="columns" :data="rows" :loading="loading" :row-key="(row: CommunityAccountEnforcementCase) => row.id" /></n-card>
    <n-alert v-if="error" type="error" style="margin-top:16px">{{ error }}</n-alert>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { NButton, type DataTableColumns, useDialog, useMessage } from 'naive-ui'
import { useRoute } from 'vue-router'
import { communityApi, type CommunityAccountEnforcementCase } from '@/api/community'
const route = useRoute(), message = useMessage(), dialog = useDialog()
const tab = ref('security'), queryUserId = ref(''), loading = ref(false), error = ref(''), rows = ref<CommunityAccountEnforcementCase[]>([])
const titles: Record<string, string> = { security: '账号安全', freeze: '冻结管理', deletion: '删除申请' }
const form = reactive({ userId: '', reasonCode: 'SECURITY_RISK', userVisibleReason: '', internalReason: '', evidence: '', expiresAt: null as number | null })
const payload = () => ({ reasonCode: form.reasonCode, userVisibleReason: form.userVisibleReason, internalReason: form.internalReason, evidenceSnapshot: JSON.stringify({ description: form.evidence, attachments: [] }), expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString().slice(0, 19) : undefined })
const userId = () => { const value = form.userId || queryUserId.value; if (!/^\d+$/.test(value)) throw new Error('请输入用户 ID'); return value }
async function security(action: 'logout' | 'reset' | 'unlock') { try { const id = userId(); if (action === 'logout') await communityApi.forceLogout(id, payload()); else if (action === 'reset') { const result = await communityApi.forcePasswordReset(id, payload()); dialog.success({ title: '一次性临时密码', content: result.temporaryPassword }) } else await communityApi.unlockAccount(id, payload()); message.success('操作成功'); await load() } catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败' } }
async function freeze(action: 'freeze' | 'extend' | 'release') { try { const id = userId(); if (action === 'freeze') await communityApi.freezeAccount(id, payload()); else if (action === 'extend') await communityApi.extendAccountFreeze(id, payload()); else await communityApi.releaseAccountFreeze(id, payload()); message.success('操作成功'); await load() } catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败' } }
async function submitDeletion() { try { await communityApi.submitAccountDeletion({ targetUserId: userId(), ...payload() }); message.success('删除申请已提交'); tab.value = 'records'; await load() } catch (cause) { error.value = cause instanceof Error ? cause.message : '提交失败' } }
async function load() { loading.value = true; try { rows.value = await communityApi.accountEnforcements(queryUserId.value || form.userId || undefined) } catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败' } finally { loading.value = false } }
const columns: DataTableColumns<CommunityAccountEnforcementCase> = [{ title: 'ID', key: 'id' }, { title: '用户', key: 'targetUserId' }, { title: '操作', key: 'measureType' }, { title: '状态', key: 'status' }, { title: '时间', key: 'createdAt' }, { title: '申请人', key: 'requestedByAdminId' }]
onMounted(() => { const value = route.query.userId; if (typeof value === 'string') { form.userId = value; queryUserId.value = value }; void load() })
</script>
<style scoped>.community-page{padding:24px}.page-heading{margin-bottom:24px}.page-eyebrow{font-size:12px;color:var(--n-text-color-3)}.form-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:0 16px}</style>
