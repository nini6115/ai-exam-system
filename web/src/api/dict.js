/**
 * 数据字典接口 —— 对应 DictTypeController（/dict/type）与 DictDataController（/dict/data）
 * 除 listByType（登录即可）外均 admin。
 * 注意：字典类型分页是 /dict/type/page，字典数据分页是 /dict/data/page?dictTypeCode=
 */
import request from '@/utils/request'

// ---------- 字典类型 ----------
export const pageDictTypes = (params) => request.get('/dict/type/page', { params })

/** 全量字典类型（下拉用） */
export const listDictTypes = () => request.get('/dict/type/list')

export const getDictType = (id) => request.get(`/dict/type/${id}`)

export const addDictType = (data) => request.post('/dict/type', data)

export const updateDictType = (data) => request.put('/dict/type', data)

/** 删除类型（级联删数据） */
export const deleteDictType = (id) => request.delete(`/dict/type/${id}`)

// ---------- 字典数据 ----------
export const pageDictDatas = (params) => request.get('/dict/data/page', { params })

/** 按编码取启用字典项（登录即可，后端走缓存） */
export const listDictDatas = (dictTypeCode) => request.get(`/dict/data/list/${dictTypeCode}`)

export const getDictData = (id) => request.get(`/dict/data/${id}`)

export const addDictData = (data) => request.post('/dict/data', data)

export const updateDictData = (data) => request.put('/dict/data', data)

export const deleteDictData = (id) => request.delete(`/dict/data/${id}`)
