/**
 * 登录会话的本地存储。只存 token 与展示用的基本信息，
 * 所有需要鉴权的数据都以服务端为准，这里仅用于路由守卫与页头展示。
 */
const STORAGE_KEY = 'smart-campus-session'

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? null
  } catch {
    return null
  }
}

export function saveSession({ token, userId, username, realName, role }) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, userId, username, realName, role }))
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
}

export function isAuthenticated() {
  return Boolean(getSession()?.token)
}
