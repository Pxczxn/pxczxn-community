<template>
  <div class="evidence-upload">
    <div class="evidence-files">
      <a
        v-for="att in attachments"
        :key="att.url"
        class="evidence-item"
        :href="att.url"
        target="_blank"
        rel="noopener"
        :title="att.name"
      >
        <img v-if="isImage(att.url)" class="evidence-item-thumb" :src="att.url" :alt="att.name" />
        <span v-else class="evidence-item-name">{{ att.name }}</span>
        <span class="evidence-item-remove" @click.prevent="removeAttachment(att)">×</span>
      </a>
      <div class="evidence-trigger">
        <n-upload
          :action="uploadUrl"
          :headers="headers"
          multiple
          :max="9"
          accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.zip,.rar,.7z,.txt"
          :default-upload="false"
          :show-file-list="false"
          @change="handleChange"
        >
          <n-icon :size="18"><AddOutline /></n-icon>
          <span>上传证据<br />图片 / 文档</span>
        </n-upload>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { type UploadFileInfo } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import { fileApi } from '@/api/system'
import { useUserStore } from '@/stores/user'

export interface EvidenceAttachment {
  name: string
  url: string
}

const props = defineProps<{
  modelValue?: EvidenceAttachment[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: EvidenceAttachment[]): void
}>()

const userStore = useUserStore()

const uploadUrl = '/api/sys/file/upload'
const headers = computed(() => ({
  'Authorization': userStore.token || ''
}))

// 已上传附件（模型值）
const attachments = ref<EvidenceAttachment[]>([])

watch(
  () => props.modelValue,
  (value) => {
    attachments.value = value ?? []
  },
  { immediate: true }
)

function isImage(url: string): boolean {
  return /\.(png|jpe?g|gif|webp|bmp|svg|avif|ico)$/i.test(url) || /^data:image\//.test(url)
}

// 选择文件后手动上传
async function handleChange({ file }: { file: UploadFileInfo }) {
  if (file.status !== 'pending' || !file.file) return
  try {
    const sysFile = await fileApi.upload(file.file)
    const url = sysFile.url || sysFile.filePath
    if (!url) return
    emit('update:modelValue', [...(props.modelValue ?? []), { name: sysFile.originalName || file.name, url }])
  } catch (e) {
    console.error('证据上传失败', e)
  }
}

function removeAttachment(att: EvidenceAttachment) {
  emit(
    'update:modelValue',
    (props.modelValue ?? []).filter(item => item.url !== att.url)
  )
}
</script>

<style scoped>
.evidence-upload {
  height: 100%;
  box-sizing: border-box;
}
.evidence-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  height: 100%;
  overflow-y: auto;
}
.evidence-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 1px solid var(--n-border-color);
  border-radius: 4px;
  overflow: hidden;
  text-decoration: none;
  background: var(--n-color);
}
.evidence-item-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.evidence-item-name {
  max-width: 100%;
  padding: 4px 6px;
  font-size: 11px;
  line-height: 1.3;
  color: var(--n-text-color-2);
  word-break: break-all;
  text-align: center;
}
.evidence-item-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 16px;
  height: 16px;
  line-height: 14px;
  text-align: center;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 0 0 0 4px;
  cursor: pointer;
}
.evidence-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 72px;
  height: 72px;
  border: 1px dashed var(--n-border-color);
  border-radius: 4px;
  color: var(--n-text-color-3);
  font-size: 11px;
  line-height: 1.4;
  text-align: center;
  cursor: pointer;
  box-sizing: border-box;
}
.evidence-trigger:hover {
  border-color: var(--n-color-primary);
  color: var(--n-color-primary);
}
.evidence-trigger :deep(.n-upload-trigger) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
}
</style>
