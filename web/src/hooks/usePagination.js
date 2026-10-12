/**
 * 分页列表状态复用
 *
 * 约定：fetchFn 接收 {...query, pageNum, pageSize}，返回 PageVO {list,total}
 * 失败时静默（拦截器已统一 toast）
 *
 * 用法：
 *   const p = usePagination(api.listUsers, { username: '' })
 *   onMounted(p.search)
 */
import { reactive, toRefs } from 'vue'

export function usePagination(fetchFn, initialQuery = {}, defaultPageSize = 10) {
  const initialSnapshot = { ...initialQuery }
  const state = reactive({
    loading: false,
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: defaultPageSize,
    query: { ...initialQuery },
  })

  async function load() {
    state.loading = true
    try {
      const data = await fetchFn({ ...state.query, pageNum: state.pageNum, pageSize: state.pageSize })
      state.list = data?.list || []
      state.total = Number(data?.total || 0)
    } catch {
      state.list = []
      state.total = 0
    } finally {
      state.loading = false
    }
  }

  /** 条件查询（回到第 1 页） */
  function search() {
    state.pageNum = 1
    return load()
  }

  /** 重置筛选后查询（恢复到初始值） */
  function reset() {
    Object.keys(state.query).forEach((k) => {
      state.query[k] = initialSnapshot[k]
    })
    return search()
  }

  function onCurrentChange(page) {
    state.pageNum = page
    return load()
  }

  function onSizeChange(size) {
    state.pageSize = size
    state.pageNum = 1
    return load()
  }

  return { ...toRefs(state), query: state.query, search, reset, load, onCurrentChange, onSizeChange }
}
