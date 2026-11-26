import { Button, Layout, Menu, Typography } from 'antd'
import {
  BookOutlined,
  HomeOutlined,
  LogoutOutlined,
  NotificationOutlined,
  ReadOutlined,
  ScheduleOutlined,
  TableOutlined,
  TeamOutlined,
  TrophyOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { ROLE_LABEL } from '../constants/roles'
import { clearSession, getSession } from '../utils/auth'

const { Header, Sider, Content } = Layout
const { Title } = Typography

/** 四个角色共用同一套布局，差异只在标题、默认称呼与菜单 */
const LAYOUT_CONFIG = {
  STUDENT: {
    title: '学生门户',
    fallbackName: '同学',
    menu: [
      { key: '/student', icon: <HomeOutlined />, label: '首页' },
      { key: '/student/courses', icon: <BookOutlined />, label: '课程列表' },
      { key: '/student/my-courses', icon: <ScheduleOutlined />, label: '我的选课' },
      { key: '/student/schedule', icon: <TableOutlined />, label: '课程表' },
      { key: '/student/grades', icon: <TrophyOutlined />, label: '我的成绩' },
    ],
  },
  TEACHER: {
    title: '教师门户',
    fallbackName: '老师',
    menu: [
      { key: '/teacher', icon: <HomeOutlined />, label: '首页' },
      { key: '/teacher/courses', icon: <BookOutlined />, label: '我的授课' },
    ],
  },
  LEADER: {
    title: '领导门户',
    fallbackName: '领导',
    menu: [
      { key: '/leader', icon: <HomeOutlined />, label: '首页概览' },
      { key: '/leader/students', icon: <UserOutlined />, label: '学生管理' },
      { key: '/leader/teachers', icon: <TeamOutlined />, label: '教师管理' },
    ],
  },
  ADMIN: {
    title: '管理后台',
    fallbackName: '管理员',
    menu: [
      { key: '/admin', icon: <HomeOutlined />, label: '数据概览' },
      { key: '/admin/announcements', icon: <NotificationOutlined />, label: '公告管理' },
      { key: '/admin/news', icon: <ReadOutlined />, label: '新闻管理' },
    ],
  },
}

/** 成绩管理页路径形如 /teacher/grades/:courseId，菜单选中时高亮「我的授课」 */
function normalizePath(pathname) {
  if (pathname.startsWith('/teacher/grades')) return '/teacher/courses'
  return pathname
}

export default function RoleLayout({ role }) {
  const navigate = useNavigate()
  const location = useLocation()
  const config = LAYOUT_CONFIG[role]
  const realName = getSession()?.realName || config.fallbackName

  const handleLogout = () => {
    clearSession()
    navigate('/login', { replace: true })
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={220} breakpoint="lg" collapsedWidth={0}>
        <div style={{ padding: '20px 16px', textAlign: 'center', color: '#fff' }}>
          <Title level={5} style={{ color: '#fff', margin: 0 }}>
            智慧校园
          </Title>
          <div style={{ opacity: 0.65, fontSize: 12, marginTop: 4 }}>{config.title}</div>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[normalizePath(location.pathname)]}
          items={config.menu}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
          }}
        >
          <span>
            欢迎，{realName}（{ROLE_LABEL[role]}）
          </span>
          <Button icon={<LogoutOutlined />} onClick={handleLogout}>
            退出登录
          </Button>
        </Header>
        <Content
          style={{ margin: 24, padding: 24, background: '#fff', borderRadius: 8, minHeight: 280 }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
