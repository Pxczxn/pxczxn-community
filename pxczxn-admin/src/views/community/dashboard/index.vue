<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">COMMUNITY OPERATIONS</div>
        <h1>运营总览</h1>
        <p>集中查看用户增长、内容生产、审核队列与发布运行状态。</p>
      </div>
      <n-button :loading="loading" @click="loadDashboard">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新数据
      </n-button>
    </header>

    <n-alert v-if="errorMessage" type="error" closable @close="errorMessage = ''">
      {{ errorMessage }}
      <template #action>
        <n-button size="small" @click="loadDashboard">重新加载</n-button>
      </template>
    </n-alert>

    <n-spin :show="loading">
      <template v-if="dashboard">
        <section class="metric-grid">
          <n-card
            v-for="metric in headlineMetrics"
            :key="metric.label"
            size="small"
            class="metric-card"
          >
            <div class="metric-card__top">
              <span>{{ metric.label }}</span>
              <n-icon :color="metric.color" size="20">
                <component :is="metric.icon" />
              </n-icon>
            </div>
            <strong>{{ compactNumber(metric.value) }}</strong>
            <small>{{ metric.hint }}</small>
          </n-card>
        </section>

        <n-grid :cols="12" :x-gap="16" :y-gap="16" responsive="screen">
          <n-grid-item :span="12" :l-span="8">
            <n-card title="近 7 日社区趋势" class="dashboard-panel">
              <template #header-extra>
                <n-space size="large">
                  <span class="chart-legend chart-legend--blue">新增用户</span>
                  <span class="chart-legend chart-legend--cyan">新增文章</span>
                </n-space>
              </template>
              <div ref="trendChartRef" class="trend-chart" aria-label="近七日新增用户与新增文章趋势图" />
            </n-card>
          </n-grid-item>

          <n-grid-item :span="12" :l-span="4">
            <n-card title="审核与发布" class="dashboard-panel">
              <div class="health-list">
                <button type="button" @click="go('/community/reviews')">
                  <span><i class="health-dot health-dot--warning" />待处理审核</span>
                  <strong>{{ dashboard.pendingReviewCount }}</strong>
                </button>
                <button type="button" @click="go('/community/articles')">
                  <span><i class="health-dot health-dot--info" />定时发布</span>
                  <strong>{{ dashboard.scheduledArticleCount }}</strong>
                </button>
                <button type="button" @click="go('/community/articles')">
                  <span><i class="health-dot health-dot--error" />发布失败</span>
                  <strong>{{ dashboard.publishFailedCount }}</strong>
                </button>
              </div>
              <n-divider />
              <div class="publish-rate">
                <div>
                  <span>内容发布率</span>
                  <strong>{{ publishRate }}%</strong>
                </div>
                <n-progress
                  type="line"
                  :percentage="publishRate"
                  :show-indicator="false"
                  :height="8"
                  border-radius="4"
                />
              </div>
            </n-card>
          </n-grid-item>

          <n-grid-item :span="12" :l-span="7">
            <n-card title="治理风险趋势" class="dashboard-panel">
              <template #header-extra>近 7 日</template>
              <div ref="governanceChartRef" class="trend-chart" aria-label="举报、处罚与反滥用拒绝趋势图" />
            </n-card>
          </n-grid-item>
          <n-grid-item :span="12" :l-span="5">
            <n-card title="治理待办" class="dashboard-panel">
              <div class="health-list">
                <button type="button" @click="go('/community/reports')"><span><i class="health-dot health-dot--warning" />待处理举报</span><strong>{{ dashboard.pendingReportCount }}</strong></button>
                <button type="button" @click="go('/community/appeals')"><span><i class="health-dot health-dot--info" />待处理申诉</span><strong>{{ dashboard.pendingAppealCount }}</strong></button>
                <button type="button" @click="go('/community/account-enforcements')"><span><i class="health-dot health-dot--error" />有效处罚</span><strong>{{ dashboard.activeSanctionCount }}</strong></button>
              </div>
              <n-divider />
              <div class="governance-summary"><span>近 7 日风险拒绝</span><strong>{{ dashboard.rejectedAbuseCount }}</strong><span>平均举报处置</span><strong>{{ dashboard.averageReportResolutionMinutes }} 分钟</strong></div>
            </n-card>
          </n-grid-item>

          <n-grid-item :span="12">
            <n-card title="最近治理审计" class="dashboard-panel">
              <n-empty v-if="!dashboard.recentGovernanceAudits.length" size="small" description="暂无治理审计事件" />
              <n-table v-else :single-line="false" size="small">
                <thead><tr><th>来源</th><th>动作</th><th>操作者</th><th>关联 ID</th><th>发生时间</th></tr></thead>
                <tbody><tr v-for="event in dashboard.recentGovernanceAudits" :key="`${event.source}-${event.referenceId}-${event.occurredAt}`"><td>{{ event.source }}</td><td>{{ event.eventType }}</td><td>{{ event.actorType }}{{ event.actorId ? ` #${event.actorId}` : '' }}</td><td>{{ event.referenceId || '-' }}</td><td>{{ event.occurredAt }}</td></tr></tbody>
              </n-table>
            </n-card>
          </n-grid-item>

          <n-grid-item :span="12">
            <n-card title="快捷入口">
              <div class="quick-grid">
                <button
                  v-for="item in quickLinks"
                  :key="item.path"
                  type="button"
                  class="quick-card"
                  @click="go(item.path)"
                >
                  <span class="quick-card__icon">
                    <n-icon size="22"><component :is="item.icon" /></n-icon>
                  </span>
                  <span>
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.description }}</small>
                  </span>
                  <n-icon><ChevronForwardOutline /></n-icon>
                </button>
              </div>
            </n-card>
          </n-grid-item>
        </n-grid>
      </template>

      <n-empty
        v-else-if="!loading && !errorMessage"
        description="暂无社区统计数据"
        class="page-empty"
      />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  AlbumsOutline,
  ChevronForwardOutline,
  DocumentTextOutline,
  PeopleOutline,
  RefreshOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
import { communityApi, type CommunityDashboard } from '@/api/community'
import { compactNumber } from '@/utils/community'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const themeStore = useThemeStore()
const dashboard = ref<CommunityDashboard | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const trendChartRef = ref<HTMLElement | null>(null)
const governanceChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let governanceChart: echarts.ECharts | null = null

const headlineMetrics = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '社区用户', value: dashboard.value.userCount, hint: `${dashboard.value.activeUserCount} 位状态正常`, color: '#2E5CF6', icon: PeopleOutline },
    { label: '博客空间', value: dashboard.value.blogCount, hint: '个人与团队博客', color: '#7C83FF', icon: AlbumsOutline },
    { label: '文章总量', value: dashboard.value.articleCount, hint: `${dashboard.value.publishedArticleCount} 篇已发布`, color: '#14B8A6', icon: DocumentTextOutline },
    { label: '待审核', value: dashboard.value.pendingReviewCount, hint: '需要运营人员处理', color: '#F59E0B', icon: ShieldCheckmarkOutline }
  ]
})

const publishRate = computed(() => {
  if (!dashboard.value?.articleCount) return 0
  return Math.round(
    dashboard.value.publishedArticleCount / dashboard.value.articleCount * 100
  )
})

const quickLinks = [
  { title: '用户与博客', description: '查询社区账号、博客与认证状态', path: '/community/users', icon: PeopleOutline },
  { title: '内容管理', description: '查看博客空间、文章状态与版本快照', path: '/community/articles', icon: AlbumsOutline },
  { title: '文章管理', description: '检索文章状态与版本快照', path: '/community/articles', icon: DocumentTextOutline },
  { title: '审核中心', description: '领取并处理文章审核任务', path: '/community/reviews', icon: ShieldCheckmarkOutline }
]

function chartTextColor() {
  return themeStore.isDark ? '#8FA3C9' : '#6B7280'
}

function renderChart() {
  if (!dashboard.value || !trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const metrics = dashboard.value.dailyMetrics
  trendChart.setOption({
    animationDuration: 500,
    color: ['#2E5CF6', '#38BDF8'],
    tooltip: { trigger: 'axis' },
    grid: { left: 12, right: 12, top: 20, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: metrics.map(item => item.date.slice(5)),
      axisLine: { lineStyle: { color: themeStore.isDark ? '#2A365D' : '#E5E7EB' } },
      axisLabel: { color: chartTextColor() }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: themeStore.isDark ? '#1E2A50' : '#EEF1F6' } },
      axisLabel: { color: chartTextColor() }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: metrics.map(item => item.userCount),
        areaStyle: { color: 'rgba(46, 92, 246, .10)' }
      },
      {
        name: '新增文章',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: metrics.map(item => item.articleCount)
      }
    ]
  }, true)

  if (!governanceChartRef.value) return
  if (!governanceChart) governanceChart = echarts.init(governanceChartRef.value)
  const governance = dashboard.value.governanceDailyMetrics
  governanceChart.setOption({
    animationDuration: 500, color: ['#F59E0B', '#EF4444', '#7C3AED'], tooltip: { trigger: 'axis' }, grid: { left: 12, right: 12, top: 20, bottom: 4, containLabel: true },
    xAxis: { type: 'category', data: governance.map(item => item.date.slice(5)), axisLabel: { color: chartTextColor() } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: chartTextColor() } },
    series: [
      { name: '举报', type: 'bar', data: governance.map(item => item.reportCount) },
      { name: '处罚', type: 'bar', data: governance.map(item => item.sanctionCount) },
      { name: '风险拒绝', type: 'line', smooth: true, data: governance.map(item => item.abuseRejectCount) }
    ]
  }, true)
}

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await communityApi.dashboard()
    await nextTick()
    renderChart()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '社区统计加载失败'
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  router.push(path)
}

function resizeChart() {
  trendChart?.resize()
  governanceChart?.resize()
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  trendChart?.dispose()
  governanceChart?.dispose()
  trendChart = null
  governanceChart = null
})
</script>

<style scoped lang="scss">
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  border-top: 3px solid var(--primary-color);

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: var(--community-muted);
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 12px;
    color: var(--community-text);
    font-size: 30px;
    line-height: 1;
  }

  small {
    display: block;
    margin-top: 10px;
    color: var(--community-muted);
  }
}

.dashboard-panel {
  height: 100%;
}

.governance-summary {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px 12px;
  color: var(--community-muted);

  strong {
    color: var(--community-text);
  }
}

.trend-chart {
  height: 292px;
}

.chart-legend {
  color: var(--community-muted);
  font-size: 12px;

  &::before {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 6px;
    border-radius: 50%;
    content: '';
  }

  &--blue::before { background: #2E5CF6; }
  &--cyan::before { background: #38BDF8; }
}

.health-list {
  display: grid;
  gap: 8px;

  button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 13px 12px;
    border: 1px solid var(--community-border);
    border-radius: 8px;
    background: var(--community-soft);
    color: var(--community-text);
    cursor: pointer;
    transition: border-color .2s, transform .2s;

    &:hover {
      border-color: var(--primary-color);
      transform: translateY(-1px);
    }

    span {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94A3B8;

  &--warning { background: #F59E0B; }
  &--info { background: #2E5CF6; }
  &--error { background: #EF4444; }
}

.publish-rate {
  > div {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    color: var(--community-muted);
  }

  strong { color: var(--community-text); }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--community-border);
  border-radius: 10px;
  background: transparent;
  color: var(--community-text);
  text-align: left;
  cursor: pointer;

  &:hover {
    border-color: var(--primary-color);
    background: var(--community-soft);
  }

  &__icon {
    display: grid;
    width: 42px;
    height: 42px;
    place-items: center;
    border-radius: 9px;
    background: var(--community-primary-soft);
    color: var(--primary-color);
  }

  strong, small { display: block; }
  small { margin-top: 4px; color: var(--community-muted); }
}

@media (max-width: 1100px) {
  .metric-grid, .quick-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 680px) {
  .metric-grid, .quick-grid { grid-template-columns: 1fr; }
}
</style>
