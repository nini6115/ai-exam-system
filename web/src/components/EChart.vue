<script setup>
/**
 * ECharts 轻封装：props.option 变更自动 setOption；容器尺寸变化自动 resize；卸载 dispose
 * 用法：<EChart :option="barOption" :height="300" />
 */
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 300 },
  loading: { type: Boolean, default: false },
})

const el = ref(null)
let chart = null
let resizeObserver = null

function render() {
  if (!chart) return
  chart.setOption(props.option, true)
  if (props.loading) chart.showLoading()
  else chart.hideLoading()
}

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(el.value)
})

watch(() => props.option, render, { deep: true })
watch(() => props.loading, render)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="el" :style="{ width: '100%', height: `${height}px` }" />
</template>
