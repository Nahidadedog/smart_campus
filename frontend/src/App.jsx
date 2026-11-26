import { Spin } from 'antd'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { homePathFor } from './constants/roles'
import RoleLayout from './layouts/RoleLayout'
import Home from './pages/common/Home'
import Login from './pages/common/Login'
import { getSession } from './utils/auth'

// 页面级代码分割：首屏只加载门户与登录页，各角色页面按需加载
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'))
const AnnouncementManage = lazy(() => import('./pages/admin/AnnouncementManage'))
const NewsManage = lazy(() => import('./pages/admin/NewsManage'))
const LeaderDashboard = lazy(() => import('./pages/leader/LeaderDashboard'))
const StudentManage = lazy(() => import('./pages/leader/StudentManage'))
const TeacherManage = lazy(() => import('./pages/leader/TeacherManage'))
const CourseList = lazy(() => import('./pages/student/CourseList'))
const MyCourses = lazy(() => import('./pages/student/MyCourses'))
const MyGrades = lazy(() => import('./pages/student/MyGrades'))
const Schedule = lazy(() => import('./pages/student/Schedule'))
const StudentDashboard = lazy(() => import('./pages/student/StudentDashboard'))
const GradeManage = lazy(() => import('./pages/teacher/GradeManage'))
const MyTeachCourses = lazy(() => import('./pages/teacher/MyTeachCourses'))
const TeacherDashboard = lazy(() => import('./pages/teacher/TeacherDashboard'))

/** 路由守卫：未登录去登录页；角色不匹配则回到自己的首页 */
function RequireRole({ role, children }) {
  const session = getSession()
  if (!session?.token) {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  if (role && session.role !== role) {
    return <Navigate to={homePathFor(session.role)} replace />
  }
  return children
}

const pageFallback = (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 280 }}>
    <Spin size="large" />
  </div>
)

export default function App() {
  return (
    <Suspense fallback={pageFallback}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />

        <Route
          path="/student"
          element={
            <RequireRole role="STUDENT">
              <RoleLayout role="STUDENT" />
            </RequireRole>
          }
        >
          <Route index element={<StudentDashboard />} />
          <Route path="courses" element={<CourseList />} />
          <Route path="my-courses" element={<MyCourses />} />
          <Route path="schedule" element={<Schedule />} />
          <Route path="grades" element={<MyGrades />} />
        </Route>

        <Route
          path="/teacher"
          element={
            <RequireRole role="TEACHER">
              <RoleLayout role="TEACHER" />
            </RequireRole>
          }
        >
          <Route index element={<TeacherDashboard />} />
          <Route path="courses" element={<MyTeachCourses />} />
          <Route path="grades/:courseId" element={<GradeManage />} />
        </Route>

        <Route
          path="/leader"
          element={
            <RequireRole role="LEADER">
              <RoleLayout role="LEADER" />
            </RequireRole>
          }
        >
          <Route index element={<LeaderDashboard />} />
          <Route path="students" element={<StudentManage />} />
          <Route path="teachers" element={<TeacherManage />} />
        </Route>

        <Route
          path="/admin"
          element={
            <RequireRole role="ADMIN">
              <RoleLayout role="ADMIN" />
            </RequireRole>
          }
        >
          <Route index element={<AdminDashboard />} />
          <Route path="announcements" element={<AnnouncementManage />} />
          <Route path="news" element={<NewsManage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
