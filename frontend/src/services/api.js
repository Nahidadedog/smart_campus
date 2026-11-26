import axios from 'axios'
import { clearSession, getSession } from '../utils/auth'

/**
 * 统一的 HTTP 客户端：
 * - 请求拦截器自动附带 Bearer token
 * - 响应拦截器把 { code, message, data } 解包为 data；code !== 200 时抛出 ApiError
 * - HTTP 401：清除本地会话并跳转登录页（登录页本身除外）
 * - HTTP 403 / 其它错误：统一转为带 message 的 ApiError，页面只需 catch 后提示
 */
export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const FALLBACK_MESSAGE = '请求失败，请稍后重试'

function messageFrom(error) {
  const { status, data } = error.response ?? {}
  if (data?.message) return data.message
  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) return '没有权限执行此操作'
  if (status === 404) return '请求的资源不存在'
  if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  if (!error.response) return '网络异常，请检查后端服务是否启动'
  return FALLBACK_MESSAGE
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getSession()?.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 文件流等非标准响应直接返回
    if (body === null || typeof body !== 'object' || !('code' in body)) return body
    if (body.code !== 200) throw new ApiError(body.code, body.message ?? FALLBACK_MESSAGE)
    return body.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 && !window.location.pathname.startsWith('/login')) {
      clearSession()
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      window.location.href = `/login?redirect=${redirect}`
      // 跳转期间不再抛错，避免页面重复弹提示
      return new Promise(() => {})
    }
    return Promise.reject(new ApiError(status ?? -1, messageFrom(error)))
  },
)

// ---------- 认证 ----------
export const login = (data) => http.post('/auth/login', data)
export const register = (data) => http.post('/auth/register', data)

// ---------- 门户公开 ----------
export const getAnnouncements = (params) => http.get('/public/announcements', { params })
export const getAnnouncement = (id) => http.get(`/public/announcements/${id}`)
export const getNews = (params) => http.get('/public/news', { params })
export const getNewsDetail = (id) => http.get(`/public/news/${id}`)
export const getCourses = (params) => http.get('/public/courses', { params })
export const getCourse = (id) => http.get(`/public/courses/${id}`)

// ---------- 学生 ----------
export const getStudentProfile = () => http.get('/student/profile')
export const updateStudentContact = (data) => http.put('/student/profile/contact', data)
export const getMyCourses = () => http.get('/student/courses')
export const selectCourse = (courseId) => http.post(`/student/courses/${courseId}`)
export const dropCourse = (courseId) => http.delete(`/student/courses/${courseId}`)
export const getMyGrades = (params) => http.get('/student/grades', { params })

// ---------- 教师 ----------
export const getTeacherProfile = () => http.get('/teacher/profile')
export const getTeacherCourses = () => http.get('/teacher/courses')
export const getCourseStudents = (courseId) => http.get(`/teacher/courses/${courseId}/students`)
export const getCourseGrades = (courseId) => http.get(`/teacher/courses/${courseId}/grades`)
export const saveGrade = (courseId, data) => http.put(`/teacher/courses/${courseId}/grades`, data)

// ---------- 领导 / 管理员共用统计 ----------
export const getStudentStats = () => http.get('/leader/statistics/students')
export const getTeacherStats = () => http.get('/leader/statistics/teachers')
export const getCourseStats = () => http.get('/leader/statistics/courses')
export const getAllStudents = () => http.get('/leader/students')
export const getAllTeachers = () => http.get('/leader/teachers')

// ---------- 管理员：内容管理 ----------
export const getAdminAnnouncements = () => http.get('/admin/announcements')
export const createAnnouncement = (data) => http.post('/admin/announcements', data)
export const deleteAnnouncement = (id) => http.delete(`/admin/announcements/${id}`)
export const getAdminNews = () => http.get('/admin/news')
export const createNews = (data) => http.post('/admin/news', data)
export const deleteNews = (id) => http.delete(`/admin/news/${id}`)

export default http
