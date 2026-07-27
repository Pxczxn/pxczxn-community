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
      :title="reviewAction === 'approve' ? '审核通过' : '审核拒绝'"
      style="width: 600px"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <n-spin :show="reviewLoading">
        <template v-if="currentApplication">
          <n-descriptions :column="1" bordered size="small" label-placement="left">
            <n-descriptions-item label="申请 ID">{{ currentApplication.id }}</n-descriptions-item>
            <n-descriptions-item label="团队名称">{{ currentApplication.teamName }}</n-descriptions-item>
            <n-descriptions-item label="团队 Slug">{{ currentApplication.teamSlug }}</n-descriptions-item>
            <n-descriptions-item label="申请人 ID">{{ currentApplication.applicantUserId }}</n-descriptions-item>
            <n-descriptions-item label="申请时间">
              {{ formatDateTime(currentApplication.createdAt) }}
            </n-descriptions-item>
            <n-descriptions-item v-if="currentApplication.description" label="申请说明">
              {{ currentApplication.description }}
            </n-descriptions-item>
          </n-descriptions>

          <n-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" style="margin-top: 16px">
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
                :rows="4"
                :maxlength="500"
                show-count
              />
            </n-form-item>
          </n-form>

          <n-alert
            v-if="reviewAction === 'approve'"
            type="info"
            style="margin-top: 16px"
            :bordered="false"
          >
            审核通过后将自动创建：团队博客、团队记录、Owner 成员、默认设置、默认分类，并发送通知。
          </n-alert>

          <n-alert
            v-if="reviewAction === 'reject'"
            type="warning"
            style="margin-top: 16px"
            :bordered="false"
          >
            拒绝后申请人可重新提交申请，请务必说明拒绝原因。
          </n-alert>
        </template>
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
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, useMessage } from 'naive-ui'
import { RefreshOutline, CheckmarkDoneOutline } from '@vicons/ionicons5'
import { communityApi, type TeamApplication } from '@/api/community'
import { formatDateTime } from '@/utils/community'

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
      return row.description || h('span', { style: { color: '#999' } }, '无')
    }
  },
  {
    title: '申请人',
    key: 'applicantUserId',
    width: 100
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
    width: 160,
    fixed: 'right' as const,
    render: (row: TeamApplication) => {
      if (row.status !== 'PENDING') {
        return h('span', { style: { color: '#999' } }, '-')
      }
      return h(
        NSpace,
        { size: 'small' },
        {
          default: () => [
            h(
              NButton,
              {
                size: 'small',
                type: 'success',
                onClick: () => openReview(row, 'approve')
              },
              { default: () => '通过' }
            ),
            h(
              NButton,
              {
                size: 'small',
                type: 'error',
                onClick: () => openReview(row, 'reject')
              },
              { default: () => '拒绝' }
            )
          ]
        }
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

function openReview(application: TeamApplication, action: 'approve' | 'reject') {
  currentApplication.value = application
  reviewAction.value = action
  reviewForm.value.reviewComment = ''
  reviewVisible.value = true
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
  color: #999;
  margin-bottom: 8px;
}

.page-heading h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}

.page-heading p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.table-alert {
  margin-bottom: 16px;
}
</style>
