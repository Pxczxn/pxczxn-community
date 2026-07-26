import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme, type GlobalTheme } from 'naive-ui'

export type ThemeMode = 'light' | 'dark' | 'starry'

// 预设主题色
export const themeColors = [
  { name: '深邃黑', color: '#111827' },
  { name: '科技蓝', color: '#2E5CF6' },
  { name: '薄暮红', color: '#F5222D' },
  { name: '火山橙', color: '#FA541C' },
  { name: '日暮黄', color: '#FAAD14' },
  { name: '极光绿', color: '#52C41A' },
  { name: '明青', color: '#13C2C2' },
  { name: '极客蓝', color: '#2F54EB' },
  { name: '酱紫', color: '#722ED1' },
  { name: '玫红', color: '#EB2F96' },
  { name: '天空蓝', color: '#0EA5E9' },
  { name: '薰衣草', color: '#8B5CF6' },
  { name: '珊瑚粉', color: '#F472B6' },
  { name: '薄荷绿', color: '#10B981' },
  { name: '琥珀金', color: '#F59E0B' },
  { name: '罗兰紫', color: '#A855F7' },
  { name: '玫瑰红', color: '#E11D48' },
  { name: '海洋蓝', color: '#0891B2' },
  { name: '翡翠绿', color: '#059669' },
  { name: '暮光橙', color: '#EA580C' },
]

export const useThemeStore = defineStore('theme', () => {
  // 主题模式: light / dark / starry
  const savedMode = localStorage.getItem('layout-theme')
  const mode = ref<ThemeMode>(
    savedMode === 'dark' || savedMode === 'starry' ? savedMode : 'light'
  )

  // 菜单位置
  const siderPosition = ref<'left' | 'right' | 'top'>(
    (localStorage.getItem('layout-position') as 'left' | 'right' | 'top') || 'left'
  )

  // 是否显示页签
  const showTabs = ref<boolean>(
    localStorage.getItem('layout-show-tabs') !== 'false'
  )

  // 主题色
  const primaryColor = ref<string>(
    localStorage.getItem('layout-primary-color') || '#111827'
  )

  // 顶栏是否应用主题色
  const headerUsePrimaryColor = ref<boolean>(
    localStorage.getItem('layout-header-primary') === 'true'
  )

  // 是否是暗色主题
  const isDark = computed(() => mode.value === 'dark' || mode.value === 'starry')
  const isStarry = computed(() => mode.value === 'starry')

  // Naive UI 主题
  const naiveTheme = computed<GlobalTheme | null>(() => {
    return isDark.value ? darkTheme : null
  })

  // 设置主题
  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem('layout-theme', newMode)
    // 更新 body 类名
    updateBodyClass()
  }

  // 设置菜单位置
  function setSiderPosition(position: 'left' | 'right' | 'top') {
    siderPosition.value = position
    localStorage.setItem('layout-position', position)
  }

  // 设置是否显示页签
  function setShowTabs(show: boolean) {
    showTabs.value = show
    localStorage.setItem('layout-show-tabs', String(show))
  }

  // 设置主题色
  function setPrimaryColor(color: string) {
    primaryColor.value = color
    localStorage.setItem('layout-primary-color', color)
    // 设置 CSS 变量，用于顶栏主题色
    document.documentElement.style.setProperty('--primary-color', color)
  }

  // 设置顶栏是否应用主题色
  function setHeaderUsePrimaryColor(use: boolean) {
    headerUsePrimaryColor.value = use
    localStorage.setItem('layout-header-primary', String(use))
  }

  // 更新 body 类名
  function updateBodyClass() {
    document.body.classList.remove('light-theme', 'dark-theme', 'starry-theme')
    document.body.classList.add(`${mode.value}-theme`)
  }

  // 初始化时更新 body 类名
  updateBodyClass()
  // 初始化时设置 CSS 变量
  document.documentElement.style.setProperty('--primary-color', primaryColor.value)

  return {
    mode,
    siderPosition,
    showTabs,
    primaryColor,
    headerUsePrimaryColor,
    isDark,
    isStarry,
    naiveTheme,
    setMode,
    setSiderPosition,
    setShowTabs,
    setPrimaryColor,
    setHeaderUsePrimaryColor,
    updateBodyClass
  }
}, {
  persist: false // 使用 localStorage 手动管理
})
