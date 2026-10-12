<script setup>
/**
 * 枚举标签：传 labelMap/tagMap + 值，渲染 el-tag
 * 用法：<EnumTag :label-map="SHEET_STATUS" :tag-map="SHEET_STATUS_TAG" :value="row.status" />
 */
import { computed } from 'vue'

const props = defineProps({
  labelMap: { type: Object, required: true },
  tagMap: { type: Object, default: () => ({}) },
  value: { type: [Number, String], default: null },
})

const label = computed(() => props.labelMap[props.value] ?? props.value ?? '-')
const tag = computed(() => props.tagMap[props.value] || 'info')
</script>

<template>
  <el-tag :type="tag" disable-transitions>{{ label }}</el-tag>
</template>
