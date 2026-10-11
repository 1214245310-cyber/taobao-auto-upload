import request from './request'

// 批量上新接口（基于逆向的 /tb/publish* 命名空间）
export function publishList(params: Record<string, unknown>) {
  return request.post('/tb/publish/list', params)
}

export function publishStat(params: Record<string, unknown>) {
  return request.post('/tb/publish/stat', params)
}

export function publishLimits() {
  return request.get('/tb/publish/limits')
}

export function publishOperators() {
  return request.get('/tb/publish/operators')
}

export function publishRestart(id: number | string) {
  return request.put(`/tb/publish/${id}/restart`)
}

export function publishPause(id: number | string) {
  return request.put(`/tb/publish/${id}/pause`)
}

export function publishResume(id: number | string) {
  return request.put(`/tb/publish/${id}/resume`)
}

export function publishTerminate(id: number | string) {
  return request.put(`/tb/publish/${id}/terminate`)
}
