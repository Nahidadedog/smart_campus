/** 角色常量：与后端 UserRole 枚举名保持一致 */

export const ROLE_HOME = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  LEADER: '/leader',
  ADMIN: '/admin',
}

export const ROLE_LABEL = {
  STUDENT: '学生',
  TEACHER: '教师',
  LEADER: '领导',
  ADMIN: '管理员',
}

export const homePathFor = (role) => ROLE_HOME[role] ?? '/login'
