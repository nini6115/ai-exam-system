/**
 * 字典 store —— 按字典类型编码缓存字典项
 * 字典管理页增删改后调 invalidate 使缓存失效
 */
import { defineStore } from 'pinia'
import { listDictDatas } from '@/api/dict'

export const useDictStore = defineStore('dict', {
  state: () => ({
    /** { [dictTypeCode]: { list, loaded } } */
    map: {},
    /** 在途请求去重：{ [dictTypeCode]: Promise } */
    inflight: {},
  }),

  actions: {
    /**
     * 取字典项列表（有缓存直接返回；并发调用共享同一 Promise）
     * @returns {Promise<Array>} 字典项数组
     */
    ensureDict(code) {
      if (this.map[code]?.loaded) return Promise.resolve(this.map[code].list)
      if (this.inflight[code]) return this.inflight[code]
      this.inflight[code] = listDictDatas(code)
        .then((list) => {
          this.map[code] = { list: list || [], loaded: true }
          return this.map[code].list
        })
        .finally(() => {
          delete this.inflight[code]
        })
      return this.inflight[code]
    },

    /** 字典项变更后失效缓存 */
    invalidate(code) {
      if (code) delete this.map[code]
    },
  },
})
