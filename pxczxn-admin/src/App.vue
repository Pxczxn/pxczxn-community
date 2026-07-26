  <template>
  <n-config-provider
    :theme="themeStore.naiveTheme"
    :theme-overrides="currentThemeOverrides"
    :locale="zhCN"
    :date-locale="dateZhCN"
  >
    <n-loading-bar-provider>
      <n-message-provider>
        <n-dialog-provider>
          <GlobalApiProvider />
          <router-view />
          <Watermark />
        </n-dialog-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import {
  dateZhCN,
  zhCN,
  useMessage,
  useDialog,
  useLoadingBar,
  type GlobalThemeOverrides
} from 'naive-ui'
import { defineComponent, computed, onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import Watermark from '@/components/Watermark/index.vue'

const themeStore = useThemeStore()

// 确保主题状态正确应用到 body
onMounted(() => {
  themeStore.updateBodyClass()
})

// 立即更新 body 类名（确保在渲染前应用）
themeStore.updateBodyClass()

// 注入全局API
const GlobalApiProvider = defineComponent({
  setup() {
    window.$message = useMessage()
    window.$dialog = useDialog()
    window.$loadingBar = useLoadingBar()
    return () => null
  }
})

// 生成主题色配置
function generateColorVariants(baseColor: string) {
  // 计算较浅和较深的颜色
  const lighten = (color: string, amount: number) => {
    const hex = color.replace('#', '')
    const r = Math.min(255, parseInt(hex.slice(0, 2), 16) + amount)
    const g = Math.min(255, parseInt(hex.slice(2, 4), 16) + amount)
    const b = Math.min(255, parseInt(hex.slice(4, 6), 16) + amount)
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
  }
  const darken = (color: string, amount: number) => {
    const hex = color.replace('#', '')
    const r = Math.max(0, parseInt(hex.slice(0, 2), 16) - amount)
    const g = Math.max(0, parseInt(hex.slice(2, 4), 16) - amount)
    const b = Math.max(0, parseInt(hex.slice(4, 6), 16) - amount)
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
  }
  return {
    primary: baseColor,
    hover: lighten(baseColor, 30),
    pressed: darken(baseColor, 20),
    suppl: darken(baseColor, 10),
    // 暗色主题下使用的亮色版本（提高对比度）
    light: lighten(baseColor, 60),
    lighter: lighten(baseColor, 80)
  }
}

// 亮色主题配置
const lightThemeOverrides = computed<GlobalThemeOverrides>(() => {
  const colors = generateColorVariants(themeStore.primaryColor)
  return {
    common: {
      primaryColor: colors.primary,
      primaryColorHover: colors.hover,
      primaryColorPressed: colors.pressed,
      primaryColorSuppl: colors.suppl,
      // warning 颜色也使用主题色
      warningColor: colors.primary,
      warningColorHover: colors.hover,
      warningColorPressed: colors.pressed,
      warningColorSuppl: colors.suppl,
      textColorBase: '#1F2937',
      textColor1: '#1F2937',
      textColor2: '#6B7280',
      textColor3: '#9CA3AF',
      borderColor: '#E5E7EB',
      dividerColor: '#E5E7EB',
      inputColor: '#F9FAFB',
      tableColor: '#FFFFFF',
      cardColor: '#FFFFFF',
      modalColor: '#FFFFFF',
      bodyColor: '#F3F4F6',
      hoverColor: '#F3F4F6',
      borderRadius: '8px',
      borderRadiusSmall: '6px'
    },
    Button: {
      borderRadiusMedium: '8px',
      borderRadiusSmall: '6px',
      heightMedium: '36px',
      // warning 类型按钮使用主题色（用于 dialog.warning 的确认按钮）
      colorWarning: colors.primary,
      colorWarningHover: colors.hover,
      colorWarningPressed: colors.pressed,
      colorWarningFocus: colors.hover,
      borderWarning: `1px solid ${colors.primary}`,
      borderWarningHover: `1px solid ${colors.hover}`,
      borderWarningPressed: `1px solid ${colors.pressed}`,
      borderWarningFocus: `1px solid ${colors.hover}`,
      textColorWarning: '#ffffff',
      textColorWarningHover: '#ffffff',
      textColorWarningPressed: '#ffffff',
      textColorWarningFocus: '#ffffff',
      rippleColorWarning: colors.primary,
      // ghost 类型 warning 按钮
      colorGhostWarning: 'transparent',
      colorGhostWarningHover: `${colors.primary}15`,
      colorGhostWarningPressed: `${colors.primary}25`,
      colorGhostWarningFocus: `${colors.primary}15`,
      textColorGhostWarning: colors.primary,
      textColorGhostWarningHover: colors.hover,
      textColorGhostWarningPressed: colors.pressed,
      textColorGhostWarningFocus: colors.hover,
      borderGhostWarning: `1px solid ${colors.primary}`,
      borderGhostWarningHover: `1px solid ${colors.hover}`,
      borderGhostWarningPressed: `1px solid ${colors.pressed}`,
      borderGhostWarningFocus: `1px solid ${colors.hover}`,
      rippleColorGhostWarning: colors.primary
    },
    Card: {
      borderRadius: '12px',
      paddingMedium: '20px',
      titleFontSizeMedium: '16px'
    },
    DataTable: {
      borderRadius: '12px',
      thColor: '#F9FAFB',
      thTextColor: '#6B7280',
      thFontWeight: '600',
      tdColor: '#FFFFFF'
    },
    Input: {
      borderRadius: '8px',
      heightMedium: '36px'
    },
    Form: {
      labelFontSizeTopMedium: '14px',
      labelTextColor: '#374151'
    },
    Menu: {
      itemHeight: '44px',
      borderRadius: '8px',
      itemColorActive: '#F3F4F6',
      itemColorActiveHover: '#E5E7EB',
      itemTextColorActive: colors.primary,
      itemTextColorActiveHover: colors.primary,
      itemIconColorActive: colors.primary,
      itemIconColorActiveHover: colors.primary
    },
    Tag: {
      borderRadius: '6px',
      // info 类型标签使用主题色
      colorInfo: `${colors.primary}15`,
      colorInfoHover: `${colors.primary}25`,
      colorInfoPressed: `${colors.primary}35`,
      textColorInfo: colors.primary,
      borderInfo: `1px solid ${colors.primary}40`
    },
    Dialog: {
      borderRadius: '12px'
    }
  }
})

// 暗色主题配置
const darkThemeOverrides = computed<GlobalThemeOverrides>(() => {
  const colors = generateColorVariants(themeStore.primaryColor)
  return {
    common: {
      primaryColor: colors.light, // 暗色主题下使用亮色版本
      primaryColorHover: colors.lighter,
      primaryColorPressed: colors.primary,
      primaryColorSuppl: colors.hover,
      // warning 颜色也使用主题色
      warningColor: colors.light,
      warningColorHover: colors.lighter,
      warningColorPressed: colors.primary,
      warningColorSuppl: colors.hover,
      bodyColor: '#101014',
      cardColor: '#18181c',
      modalColor: '#18181c',
      popoverColor: '#27272a',
      tableColor: '#18181c',
      inputColor: '#27272a',
      borderColor: '#3f3f46',
      dividerColor: '#3f3f46',
      hoverColor: '#27272a',
      borderRadius: '8px',
      borderRadiusSmall: '6px'
    },
    Button: {
      borderRadiusMedium: '8px',
      borderRadiusSmall: '6px',
      heightMedium: '36px',
      colorSecondary: '#27272a',
      colorSecondaryHover: '#3f3f46',
      colorSecondaryPressed: '#52525b',
      // 确保主色按钮文字为白色
      textColorPrimary: '#ffffff',
      textColorHoverPrimary: '#ffffff',
      textColorPressedPrimary: '#ffffff',
      textColorFocusPrimary: '#ffffff',
      // 确保文字按钮在暗色主题下可见（使用亮色）
      textColorText: colors.light,
      textColorTextHover: colors.lighter,
      textColorTextPressed: colors.primary,
      textColorTextFocus: colors.light,
      // 默认按钮文字颜色
      textColor: '#ffffffd1',
      textColorHover: '#ffffff',
      textColorPressed: '#ffffffa6',
      // warning 类型按钮使用主题色（用于 dialog.warning 的确认按钮）
      colorWarning: colors.light,
      colorWarningHover: colors.lighter,
      colorWarningPressed: colors.primary,
      colorWarningFocus: colors.lighter,
      borderWarning: `1px solid ${colors.light}`,
      borderWarningHover: `1px solid ${colors.lighter}`,
      borderWarningPressed: `1px solid ${colors.primary}`,
      borderWarningFocus: `1px solid ${colors.lighter}`,
      textColorWarning: '#ffffff',
      textColorWarningHover: '#ffffff',
      textColorWarningPressed: '#ffffff',
      textColorWarningFocus: '#ffffff',
      rippleColorWarning: colors.light,
      // ghost 类型 warning 按钮
      colorGhostWarning: 'transparent',
      colorGhostWarningHover: `${colors.light}15`,
      colorGhostWarningPressed: `${colors.light}25`,
      colorGhostWarningFocus: `${colors.light}15`,
      textColorGhostWarning: colors.light,
      textColorGhostWarningHover: colors.lighter,
      textColorGhostWarningPressed: colors.primary,
      textColorGhostWarningFocus: colors.lighter,
      borderGhostWarning: `1px solid ${colors.light}`,
      borderGhostWarningHover: `1px solid ${colors.lighter}`,
      borderGhostWarningPressed: `1px solid ${colors.primary}`,
      borderGhostWarningFocus: `1px solid ${colors.lighter}`,
      rippleColorGhostWarning: colors.light
    },
    Card: {
      borderRadius: '12px',
      paddingMedium: '20px',
      titleFontSizeMedium: '16px',
      color: '#18181c',
      borderColor: '#3f3f46'
    },
    DataTable: {
      borderRadius: '12px',
      thFontWeight: '600',
      thColor: '#262629',
      tdColor: '#18181c',
      tdColorHover: '#262629',
      borderColor: '#3f3f46'
    },
    Input: {
      borderRadius: '8px',
      heightMedium: '36px',
      color: '#27272a',
      colorFocus: '#27272a',
      border: '1px solid #3f3f46',
      borderHover: '1px solid #52525b',
      borderFocus: `1px solid ${colors.light}`
    },
    Form: {
      labelFontSizeTopMedium: '14px'
    },
    Menu: {
      itemHeight: '44px',
      borderRadius: '8px',
      color: '#18181c',
      itemColorActive: '#27272a',
      itemColorActiveHover: '#3f3f46',
      // 确保菜单文字在暗色主题下可见
      itemTextColor: '#ffffffa6',
      itemTextColorHover: '#ffffff',
      // 选中状态使用亮色版本提高对比度
      itemTextColorActive: colors.light,
      itemTextColorActiveHover: colors.lighter,
      itemIconColor: '#ffffffa6',
      itemIconColorHover: '#ffffff',
      itemIconColorActive: colors.light,
      itemIconColorActiveHover: colors.lighter,
      // 子菜单文字颜色
      itemTextColorChildActive: colors.light,
      itemTextColorChildActiveHover: colors.lighter,
      itemIconColorChildActive: colors.light,
      itemIconColorChildActiveHover: colors.lighter
    },
    Tag: {
      borderRadius: '6px',
      // info 类型标签使用主题色（暗色模式下使用亮色版本）
      colorInfo: `${colors.light}20`,
      colorInfoHover: `${colors.light}30`,
      colorInfoPressed: `${colors.light}40`,
      textColorInfo: colors.light,
      borderInfo: `1px solid ${colors.light}50`
    },
    Dialog: {
      borderRadius: '12px',
      color: '#18181c'
    },
    Popover: {
      color: '#27272a'
    },
    Dropdown: {
      color: '#27272a'
    },
    InternalSelection: {
      color: '#27272a',
      colorActive: '#27272a',
      border: '1px solid #3f3f46',
      borderHover: '1px solid #52525b',
      borderActive: `1px solid ${colors.light}`,
      borderFocus: `1px solid ${colors.light}`
    },
    Tabs: {
      // 页签选中色也使用亮色
      tabTextColorActiveLine: colors.light,
      tabTextColorHoverLine: colors.lighter,
      tabTextColorActiveBar: colors.light,
      tabTextColorHoverBar: colors.lighter,
      barColor: colors.light
    }
  }
})

// 星空主题：保留暗色组件语义，并使用蓝紫与青色构建高对比运营界面。
const starryThemeOverrides = computed<GlobalThemeOverrides>(() => ({
  common: {
    primaryColor: '#7C8CFF',
    primaryColorHover: '#9AA7FF',
    primaryColorPressed: '#6272E8',
    primaryColorSuppl: '#38D9F2',
    infoColor: '#38D9F2',
    successColor: '#34D399',
    warningColor: '#FBBF24',
    errorColor: '#FB7185',
    bodyColor: '#070B1D',
    cardColor: '#0F1733',
    modalColor: '#0F1733',
    popoverColor: '#121C3D',
    tableColor: '#0F1733',
    inputColor: '#121C3D',
    borderColor: '#26345F',
    dividerColor: '#26345F',
    hoverColor: '#18254B',
    textColorBase: '#F4F7FF',
    textColor1: '#F4F7FF',
    textColor2: '#A8B3CF',
    textColor3: '#74809E',
    borderRadius: '8px',
    borderRadiusSmall: '6px'
  },
  Button: {
    borderRadiusMedium: '8px',
    borderRadiusSmall: '6px',
    heightMedium: '36px',
    textColorPrimary: '#FFFFFF',
    textColorHoverPrimary: '#FFFFFF',
    textColorPressedPrimary: '#FFFFFF',
    textColorFocusPrimary: '#FFFFFF'
  },
  Card: {
    borderRadius: '12px',
    paddingMedium: '20px',
    titleFontSizeMedium: '16px',
    color: '#0F1733',
    borderColor: '#26345F'
  },
  DataTable: {
    borderRadius: '12px',
    thFontWeight: '600',
    thColor: '#121C3D',
    thTextColor: '#A8B3CF',
    tdColor: '#0F1733',
    tdColorHover: '#18254B',
    borderColor: '#26345F'
  },
  Input: {
    borderRadius: '8px',
    heightMedium: '36px',
    color: '#121C3D',
    colorFocus: '#121C3D',
    border: '1px solid #26345F',
    borderHover: '1px solid #44558B',
    borderFocus: '1px solid #7C8CFF'
  },
  InternalSelection: {
    color: '#121C3D',
    colorActive: '#121C3D',
    border: '1px solid #26345F',
    borderHover: '1px solid #44558B',
    borderActive: '1px solid #7C8CFF',
    borderFocus: '1px solid #7C8CFF'
  },
  Menu: {
    itemHeight: '44px',
    borderRadius: '8px',
    color: '#0F1733',
    itemColorActive: '#1B2853',
    itemColorActiveHover: '#223363',
    itemTextColor: '#A8B3CF',
    itemTextColorHover: '#F4F7FF',
    itemTextColorActive: '#9AA7FF',
    itemTextColorActiveHover: '#B4BEFF',
    itemIconColor: '#74809E',
    itemIconColorHover: '#F4F7FF',
    itemIconColorActive: '#7C8CFF',
    itemIconColorActiveHover: '#9AA7FF',
    itemTextColorChildActive: '#9AA7FF',
    itemTextColorChildActiveHover: '#B4BEFF',
    itemIconColorChildActive: '#7C8CFF',
    itemIconColorChildActiveHover: '#9AA7FF'
  },
  Tag: {
    borderRadius: '6px',
    colorInfo: 'rgba(56, 217, 242, .12)',
    textColorInfo: '#67E8F9',
    borderInfo: '1px solid rgba(56, 217, 242, .3)'
  },
  Dialog: {
    borderRadius: '12px',
    color: '#0F1733'
  },
  Popover: { color: '#121C3D' },
  Dropdown: { color: '#121C3D' },
  Tabs: {
    tabTextColorActiveLine: '#9AA7FF',
    tabTextColorHoverLine: '#B4BEFF',
    tabTextColorActiveBar: '#9AA7FF',
    tabTextColorHoverBar: '#B4BEFF',
    barColor: '#7C8CFF'
  }
}))

// 根据当前主题选择配置
const currentThemeOverrides = computed(() => {
  if (themeStore.isStarry) return starryThemeOverrides.value
  return themeStore.isDark ? darkThemeOverrides.value : lightThemeOverrides.value
})
</script>
