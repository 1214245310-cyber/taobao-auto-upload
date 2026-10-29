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

// 发送邮箱验证码（QQ 邮箱 SMTP，后端 60 秒冷却）
export function sendEmailCode(email: string) {
  return request.post('/auth/email-code', { email })
}

// 注册：密码在后端以 BCrypt 存储，前端不做任何加密
export function register(data: {
  username: string
  password: string
  confirmPassword: string
  nickname?: string
  email: string
  emailCode: string
}) {
  return request.post('/auth/register', data)
}

// 忘记密码：账号 + 注册邮箱匹配后才发验证码
export function sendResetCode(data: { username: string; email: string }) {
  return request.post('/auth/password/send-code', data)
}

// 忘记密码：账号 + 邮箱 + 验证码 + 新密码
export function resetPasswordByEmail(data: {
  username: string
  email: string
  emailCode: string
  newPassword: string
  confirmPassword: string
}) {
  return request.post('/auth/password/reset', data)
}

export function getInfo() {
  return request.get('/system/user/getInfo')
}
