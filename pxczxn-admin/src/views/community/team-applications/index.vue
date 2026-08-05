<template>
  <div class="community-page">
    <header class="page-heading">
      <div>
        <div class="page-eyebrow">TEAM APPLICATION REVIEW</div>
        <h1>团队申请审核</h1>
        <p>审核用户提交的团队创建申请，通过后自动创建团队博客与成员关系。</p>
      </div>
      <n-button :loading="loading" @click="loadData">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新列表
      </n-button>
    </header>

    <n-card>
      <n-alert v-if="errorMessage" type="error" class="table-alert">
        {{ errorMessage }}
        <template #action><n-button size="small" @click="loadData">重试</n-button></template>
      </n-alert>

      <n-empty v-if="!loading && rows.length === 0" description="暂无待审申请">
        <template #icon>
          <n-icon><CheckmarkDoneOutline /></n-icon>
        </template>
      </n-empty>

      <n-data-table
        v-else
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="rowKey"
        :scroll-x="1200"
      />
    </n-card>

    <!-- Review Modal -->
    <n-modal
      v-model:show="reviewVisible"
      preset="card"
      title="团队申请审核"
      class="dialog-form-lg"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <n-spin :show="reviewLoading">
        <div v-if="currentApplication" class="review-split">
          <div class="review-pane">
            <div class="review-pane-title">申请信息</div>
            <n-descriptions :column="1" bordered size="small" label-placement="left">
              <n-descriptions-item label="申请 ID">{{ currentApplication.id }}</n-descriptions-item>
              <n-descriptions-item label="团队名称">{{ currentApplication.teamName }}</n-descriptions-item>
              <n-descriptions-item label="团队 Slug">{{ currentApplication.teamSlug }}</n-descriptions-item>
              <n-descriptions-item label="申请人 ID">
                <span
                  class="clickable-cell user-id-cell applicant-link"
                  @click="openApplicant(currentApplication.applicantUserId)"
                >
                  {{ currentApplication.applicantUserId }}
                </span>
              </n-descriptions-item>
              <n-descriptions-item label="申请时间">
                {{ formatDateTime(currentApplication.createdAt) }}
              </n-descriptions-item>
              <n-descriptions-item v-if="currentApplication.description" label="申请说明">
                <div class="description-text">{{ currentApplication.description }}</div>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="review-pane">
            <div class="review-pane-title">审核操作</div>
            <n-radio-group v-model:value="reviewAction" class="review-action-radio">
              <n-space size="medium">
                <n-radio-button value="approve">通过</n-radio-button>
                <n-radio-button value="reject">拒绝</n-radio-button>
              </n-space>
            </n-radio-group>

            <n-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules">
              <n-form-item
                label="审核说明"
                path="reviewComment"
                :required="reviewAction === 'reject'"
              >
                <n-input
                  v-model:value="reviewForm.reviewComment"
                  type="textarea"
                  :placeholder="
                    reviewAction === 'approve'
                      ? '可选：审核通过的说明'
                      : '必填：请说明拒绝原因'
                  "
                  :rows="5"
                  :maxlength="500"
                  show-count
                />
              </n-form-item>
            </n-form>

            <n-alert
              v-if="reviewAction === 'approve'"
              type="info"
              :bordered="false"
            >
              审核通过后将自动创建：团队博客、团队记录、Owner 成员、默认设置、默认分类，并发送通知。
            </n-alert>

            <n-alert
              v-if="reviewAction === 'reject'"
              type="warning"
              :bordered="false"
            >
              拒绝后申请人可重新提交申请，请务必说明拒绝原因。
            </n-alert>
          </div>
        </div>
      </n-spin>

      <template #footer>
        <n-space justify="end">
          <n-button @click="reviewVisible = false">取消</n-button>
          <n-button
            :type="reviewAction === 'approve' ? 'primary' : 'error'"
            :loading="reviewLoading"
            @click="handleReviewSubmit"
          >
            {{ reviewAction === 'approve' ? '确认通过' : '确认拒绝' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 申请说明详情 -->
    <n-modal
      v-model:show="descVisible"
      preset="card"
      title="申请说明"
      class="dialog-form-md"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <n-descriptions
        v-if="hasParsedDescription"
        :column="1"
        bordered
        label-placement="left"
        size="small"
      >
        <n-descriptions-item v-if="parsedDescription.category" label="团队分类">
          <n-space size="small" :wrap="true">
            <n-tag
              v-for="tag in parsedDescription.category.split('、')"
              :key="tag"
              size="small"
              type="info"
            >
              {{ tag }}
            </n-tag>
          </n-space>
        </n-descriptions-item>
        <n-descriptions-item v-if="parsedDescription.scale" label="团队规模">
          {{ parsedDescription.scale }}
        </n-descriptions-item>
        <n-descriptions-item v-if="parsedDescription.goal" label="团队目标">
          <div class="description-text">{{ parsedDescription.goal }}</div>
        </n-descriptions-item>
        <n-descriptions-item v-if="parsedDescription.content" label="内容方向">
          <div class="description-text">{{ parsedDescription.content }}</div>
        </n-descriptions-item>
      </n-descriptions>
      <n-card
        v-else
        class="description-card"
        :bordered="true"
      >
        {{ currentDescription }}
      </n-card>
      <template #footer>
        <n-space justify="end">
          <n-button @click="descVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 申请人详情 -->
    <n-modal
      v-model:show="applicantVisible"
      preset="card"
      title="申请人详情"
      class="dialog-form-md"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <n-spin :show="applicantLoading">
        <template v-if="currentApplicant">
          <n-descriptions :column="1" bordered size="small" label-placement="left">
            <n-descriptions-item label="用户 ID">{{ currentApplicant.id }}</n-descriptions-item>
            <n-descriptions-item label="用户名">{{ currentApplicant.username }}</n-descriptions-item>
            <n-descriptions-item label="显示名称">{{ currentApplicant.displayName }}</n-descriptions-item>
            <n-descriptions-item label="邮箱">{{ currentApplicant.email }}</n-descriptions-item>
            <n-descriptions-item label="账号状态">{{ statusLabel(currentApplicant.status) }}</n-descriptions-item>
            <n-descriptions-item label="认证状态">{{ statusLabel(currentApplicant.verificationStatus) }}</n-descriptions-item>
            <n-descriptions-item v-if="currentApplicant.personalBlogName" label="个人博客">
              {{ currentApplicant.personalBlogName }}
            </n-descriptions-item>
            <n-descriptions-item v-if="currentApplicant.lastLoginAt" label="最后登录">
              {{ formatDateTime(currentApplicant.lastLoginAt) }}
            </n-descriptions-item>
            <n-descriptions-item label="注册时间">
              {{ formatDateTime(currentApplicant.createdAt) }}
            </n-descriptions-item>
          </n-descriptions>
        </template>
        <n-empty v-else-if="!applicantLoading" description="未找到申请人信息" />
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="applicantVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, h, computed, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, NRadioGroup, NRadioButton, useMessage } from 'naive-ui'
import { RefreshOutline, CheckmarkDoneOutline } from '@vicons/ionicons5'
import { communityApi, type TeamApplication, type CommunityUser } from '@/api/community'
import { formatDateTime, statusLabel } from '@/utils/community'

const message = useMessage()

const loading = ref(false)
const errorMessage = ref('')
const rows = ref<TeamApplication[]>([])

const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewAction = ref<'approve' | 'reject'>('approve')
const currentApplication = ref<TeamApplication | null>(null)
const reviewFormRef = ref()

const reviewForm = ref({
  reviewComment: ''
})

const descVisible = ref(false)
const currentDescription = ref('')

interface ParsedDescription {
  category?: string
  scale?: string
  goal?: string
  content?: string
}

const parsedDescription = ref<ParsedDescription>({})

const hasParsedDescription = computed(
  () =>
    !!(
      parsedDescription.value.category ||
      parsedDescription.value.scale ||
      parsedDescription.value.goal ||
      parsedDescription.value.content
    )
)

const applicantVisible = ref(false)
const applicantLoading = ref(false)
const currentApplicant = ref<CommunityUser | null>(null)

const reviewRules = {
  reviewComment: [
    {
      required: true,
      trigger: ['input', 'blur'],
      validator: (_rule: unknown, value: string) => {
        if (reviewAction.value === 'reject' && (!value || value.trim() === '')) {
          return new Error('拒绝申请时必须填写说明')
        }
        return true
      }
    }
  ]
}

const rowKey = (row: TeamApplication) => row.id

const columns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '团队名称',
    key: 'teamName',
    width: 180,
    ellipsis: { tooltip: true }
  },
  {
    title: '团队 Slug',
    key: 'teamSlug',
    width: 160,
    ellipsis: { tooltip: true },
    render: (row: TeamApplication) => {
      return h('code', { style: { fontSize: '13px' } }, row.teamSlug)
    }
  },
  {
    title: '申请说明',
    key: 'description',
    width: 220,
    ellipsis: { tooltip: true },
    render: (row: TeamApplication) => {
      const text = row.description || '无'
      return h(
        'span',
        {
          class: 'clickable-cell',
          onClick: () => openDescription(row.description)
        },
        text
      )
    }
  },
  {
    title: '申请人',
    key: 'applicantUserId',
    width: 100,
    render: (row: TeamApplication) => {
      return h(
        'span',
        {
          class: 'clickable-cell user-id-cell',
          onClick: () => openApplicant(row.applicantUserId)
        },
        row.applicantUserId
      )
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row: TeamApplication) => {
      const statusMap = {
        PENDING: { text: '待审核', type: 'warning' as const },
        APPROVED: { text: '已通过', type: 'success' as const },
        REJECTED: { text: '已拒绝', type: 'error' as const },
        CANCELLED: { text: '已取消', type: 'default' as const }
      }
      const status = statusMap[row.status]
      return h(NTag, { type: status.type, size: 'small' }, { default: () => status.text })
    }
  },
  {
    title: '申请时间',
    key: 'createdAt',
    width: 160,
    render: (row: TeamApplication) => formatDateTime(row.createdAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    fixed: 'right' as const,
    render: (row: TeamApplication) => {
      if (row.status !== 'PENDING') {
        return h('span', { style: { color: 'var(--community-muted)' } }, '-')
      }
      return h(
        NButton,
        {
          size: 'small',
          type: 'primary',
          secondary: true,
          onClick: () => openReview(row)
        },
        { default: () => '审核' }
      )
    }
  }
]

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await communityApi.getTeamApplications()
    rows.value = data
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '加载失败'
    message.error('加载团队申请列表失败')
  } finally {
    loading.value = false
  }
}

function openReview(application: TeamApplication) {
  currentApplication.value = application
  reviewAction.value = 'approve'
  reviewForm.value.reviewComment = ''
  reviewVisible.value = true
}

function parseDescription(raw?: string): ParsedDescription {
  if (!raw) return {}
  const result: ParsedDescription = {}
  const regex = /【([^】]+)】\s*([^\n【]+)/g
  let match: RegExpExecArray | null
  while ((match = regex.exec(raw)) !== null) {
    const label = match[1].trim()
    const value = match[2].trim()
    if (label === '团队分类') result.category = value
    else if (label === '团队规模') result.scale = value
    else if (label === '团队目标') result.goal = value
    else if (label === '内容方向') result.content = value
  }
  return result
}

function openDescription(description?: string) {
  currentDescription.value = description || '无申请说明'
  parsedDescription.value = parseDescription(description)
  descVisible.value = true
}

async function openApplicant(userId: string) {
  currentApplicant.value = null
  applicantVisible.value = true
  applicantLoading.value = true
  try {
    const user = await communityApi.user(userId)
    currentApplicant.value = user
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '加载申请人信息失败')
  } finally {
    applicantLoading.value = false
  }
}

async function handleReviewSubmit() {
  if (reviewAction.value === 'reject') {
    try {
      await reviewFormRef.value?.validate()
    } catch {
      return
    }
  }

  if (!currentApplication.value) return

  reviewLoading.value = true
  try {
    if (reviewAction.value === 'approve') {
      const result = await communityApi.approveTeamApplication(
        currentApplication.value.id,
        { reviewComment: reviewForm.value.reviewComment || undefined }
      )
      message.success(`审核通过！团队 ID: ${result.teamId}`)
    } else {
      await communityApi.rejectTeamApplication(
        currentApplication.value.id,
        { reviewComment: reviewForm.value.reviewComment }
      )
      message.success('已拒绝申请')
    }

    reviewVisible.value = false
    loadData()
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '审核操作失败')
  } finally {
    reviewLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.community-page {
  padding: 24px;
}

.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-eyebrow {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--community-muted);
  margin-bottom: 8px;
}

.page-heading h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}

.page-heading p {
  margin: 0;
  color: var(--community-muted);
  font-size: 14px;
}

.table-alert {
  margin-bottom: 16px;
}

.clickable-cell {
  cursor: pointer;
  color: var(--primary-color, #1677ff);
  transition: color 0.2s;
}

.clickable-cell:hover {
  color: var(--primary-color, #4096ff);
  text-decoration: underline;
}

.user-id-cell {
  word-break: break-all;
}

.description-card {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.7;
}

.description-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  color: var(--text-primary);
}

.review-split {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.review-pane {
  flex: 1 1 320px;
  min-width: 0;
}

.review-pane-title {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--community-muted);
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid var(--community-accent, #1677ff);
}

.review-action-radio {
  margin-bottom: 16px;
}

.applicant-link,
.applicant-link:hover {
  text-decoration: none;
}
</style>
