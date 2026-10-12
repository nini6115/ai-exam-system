<script setup>
/**
 * 字典下拉：v-model + dictTypeCode，数据走 dict store 缓存
 * 用法：<DictSelect v-model="query.type" dict-type-code="question_type" clearable />
 */
import { ref, onMounted } from 'vue'
import { useDictStore } from '@/store/dict'

const props = defineProps({
  dictTypeCode: { type: String, required: true },
  clearable: { type: Boolean, default: false },
  placeholder: { type: String, default: '请选择' },
})
const model = defineModel({ type: [String, Number], default: null })

const dictStore = useDictStore()
const options = ref([])

onMounted(() => {
  dictStore.ensureDict(props.dictTypeCode).then((list) => {
    options.value = list || []
  })
})
</script>

<template>
  <el-select v-model="model" :clearable="clearable" :placeholder="placeholder" style="width: 100%">
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="item.label"
      :value="item.value"
    />
  </el-select>
</template>
