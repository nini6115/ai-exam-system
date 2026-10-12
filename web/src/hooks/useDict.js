/**
 * 字典下拉数据 hook
 *
 * 用法：
 *   const { list, labelOf } = useDict('question_category')
 *   labelOf(3) // → 字典标签
 */
import { ref } from 'vue'
import { useDictStore } from '@/store/dict'

export function useDict(code) {
  const dictStore = useDictStore()
  const list = ref([])
  const loading = ref(false)

  function refresh() {
    loading.value = true
    dictStore
      .ensureDict(code)
      .then((data) => {
        list.value = data || []
      })
      .finally(() => {
        loading.value = false
      })
  }

  refresh()

  function labelOf(value) {
    const item = list.value.find((d) => String(d.value) === String(value))
    return item ? item.label : value
  }

  return { list, loading, labelOf, refresh }
}
