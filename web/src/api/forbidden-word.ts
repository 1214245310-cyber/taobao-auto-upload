import request from './request'

// 违禁词管理接口
export function getForbiddenWords() {
  return request.get('/system/forbidden-word/list')
}

export function addForbiddenWord(data: {
  word: string
  category?: string
  level?: number
  remark?: string
  enabled?: string
  matchType?: string
  targetLibrary?: string
}) {
  return request.post('/system/forbidden-word/add', data)
}

/** 批量导入：一行一个词，支持「词,标签,备注」 */
export function batchAddForbiddenWord(data: {
  text?: string
  lines?: string[]
  category?: string
  matchType?: string
  targetLibrary?: string
  remark?: string
}) {
  return request.post('/system/forbidden-word/batch-add', data)
}

export function updateForbiddenWord(data: {
  id: number
  word?: string
  category?: string
  level?: number
  remark?: string
  enabled?: string
  matchType?: string
  targetLibrary?: string
}) {
  return request.post('/system/forbidden-word/update', data)
}

export function toggleForbiddenWord(id: number, enabled: boolean) {
  return request.post('/system/forbidden-word/toggle', { id, enabled })
}

export function deleteForbiddenWord(id: number) {
  return request.post('/system/forbidden-word/delete', { id })
}
