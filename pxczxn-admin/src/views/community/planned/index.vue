<template>
  <div class="planned-page">
    <n-result status="info" :title="title" :description="description">
      <template #footer>
        <n-button type="primary" @click="router.push('/community/dashboard')">
          返回运营总览
        </n-button>
      </template>
    </n-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const content = {
  '/community/teams': {
    title: '团队管理正在随 M3 交付',
    description: '团队申请、成员关系、投稿审计和团队治理会在团队博客能力完成后开放。此入口不展示虚构团队数据。'
  },
  '/community/series': {
    title: '系列管理正在随 M3 交付',
    description: '系列创建、章节排序、连载状态与平台审核会和团队协作创作一起交付。'
  },
  '/community/analytics': {
    title: '社区数据分析正在随 M5 交付',
    description: '创作者和运营数据分析将在有可解释的数据口径后开放，不展示伪造指标。'
  }
} as const

const current = computed(() => content[route.path as keyof typeof content])
const title = computed(() => current.value?.title ?? '功能规划中')
const description = computed(() => current.value?.description ?? '该入口已纳入正式信息架构，功能将在对应里程碑交付。')
</script>

<style scoped>
.planned-page {
  min-height: min(520px, calc(100vh - 180px));
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: var(--n-color);
}
</style>
