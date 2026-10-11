import request from './request'

// 登录 / 验证码接口（RuoYi-Vue-Plus 标准端点，逆向实测可用）
export function getCode() {
  return request.get('/auth/code')
}

export function login(data: {
  username: string
  password: string
  code: string
  uuid: string
  clientId: string
}) {
  return request.post('/auth/login', data)
}

export function getInfo() {
  return request.get('/system/user/getInfo')
}
