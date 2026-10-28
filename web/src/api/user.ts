import request from './request'

// 用户管理接口（管理员）
export function getUsers() {
  return request.get('/system/user/list')
}

export function addUser(data: {
  username: string
  nickname?: string
  email?: string
  password: string
  role?: string
}) {
  return request.post('/system/user/add', data)
}

export function resetUserPwd(userId: number, newPassword: string) {
  return request.post('/system/user/reset-pwd', { userId, newPassword })
}

// 当前登录用户修改自己的密码
export function changePwd(oldPassword: string, newPassword: string) {
  return request.post('/system/user/change-pwd', { oldPassword, newPassword })
}
