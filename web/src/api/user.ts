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

// 编辑用户：昵称 / 邮箱 / 备注 / 角色 / 状态（email 传空表示解绑）
export function updateUser(data: {
  id: number
  nickname?: string
  email?: string
  role?: string
  status?: string
  remark?: string
}) {
  return request.post('/system/user/update', data)
}

export function deleteUser(userId: number) {
  return request.post('/system/user/delete', { userId })
}

export function resetUserPwd(userId: number, newPassword: string) {
  return request.post('/system/user/reset-pwd', { userId, newPassword })
}

// 当前登录用户修改自己的密码
export function changePwd(oldPassword: string, newPassword: string) {
  return request.post('/system/user/change-pwd', { oldPassword, newPassword })
}
