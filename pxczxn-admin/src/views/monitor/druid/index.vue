<template>
  <div class="page-container">
    <header class="page-heading">
      <div><div class="page-eyebrow">DATABASE MONITOR</div><h1>SQL 监控</h1><p>查看数据库连接池与驱动状态，不暴露数据库凭据。</p></div>
      <n-button :loading="loading" @click="load"><template #icon><n-icon><RefreshOutline /></n-icon></template>刷新</n-button>
    </header>
    <n-alert v-if="error" type="error" class="table-alert">{{ error }}</n-alert>
    <n-grid :cols="2" :x-gap="16" :y-gap="16">
      <n-gi><n-card title="数据库信息"><n-descriptions :column="1" label-placement="left"><n-descriptions-item label="产品">{{ info.databaseProduct || '-' }}</n-descriptions-item><n-descriptions-item label="版本">{{ info.databaseVersion || '-' }}</n-descriptions-item><n-descriptions-item label="驱动">{{ info.driverName || '-' }}</n-descriptions-item><n-descriptions-item label="连接地址">{{ info.jdbcUrl || '-' }}</n-descriptions-item></n-descriptions></n-card></n-gi>
      <n-gi><n-card title="连接池状态"><n-statistic label="活跃连接" :value="info.activeConnections ?? 0" /><n-statistic label="空闲连接" :value="info.idleConnections ?? 0" style="margin-top:16px" /><n-statistic label="连接总数" :value="info.totalConnections ?? 0" style="margin-top:16px" /><n-statistic label="等待连接线程" :value="info.threadsAwaitingConnection ?? 0" style="margin-top:16px" /></n-card></n-gi>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NButton, NIcon } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import { databaseMonitorApi, type DatabaseMonitorInfo } from '@/api/monitor'

const loading = ref(false); const error = ref(''); const info = ref<Partial<DatabaseMonitorInfo>>({})
async function load() { loading.value = true; error.value = ''; try { info.value = await databaseMonitorApi.info() } catch (e) { error.value = e instanceof Error ? e.message : '加载 SQL 监控失败' } finally { loading.value = false } }
onMounted(() => { void load() })
</script>
