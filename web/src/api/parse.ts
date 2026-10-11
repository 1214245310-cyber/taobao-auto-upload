import request from './request'

// 解析裂变接口（基于逆向的 /goods/parse-* 命名空间）
export function parseChannels() {
  return request.get('/goods/parse-channel/list')
}

export function parseTaskContext() {
  return request.get('/goods/parse-task/submit-context')
}

export function parseItem(params: Record<string, unknown>) {
  return request.post('/goods/parse-item', params)
}

export function parseTaskUpload(params: Record<string, unknown>) {
  return request.post('/goods/parse-task/upload', params)
}
