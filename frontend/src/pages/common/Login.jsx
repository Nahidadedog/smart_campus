import { App, Button, Card, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { homePathFor } from '../../constants/roles'
import { login } from '../../services/api'
import { getSession, saveSession } from '../../utils/auth'

const { Title } = Typography

const HOME_PATH_BY_ROLE = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  LEADER: '/leader',
  ADMIN: '/admin',
}

export default function Login() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { message } = App.useMessage()

  // 已登录用户直接回到自己的首页
  useEffect(() => {
    const session = getSession()
    if (session?.token) {
      navigate(homePathFor(session.role), { replace: true })
    }
  }, [navigate])

  const handleFinish = async (values) => {
    try {
      const data = await login(values)
      saveSession(data)
      message.success(`欢迎回来，${data.realName}`)
      const redirect = searchParams.get('redirect')
      navigate(
        redirect && redirect.startsWith('/') && !redirect.startsWith('/login')
          ? redirect
          : (HOME_PATH_BY_ROLE[data.role] ?? '/'),
        {
          replace: true,
        },
      )
    } catch (err) {
      message.error(err.message)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #f0f5ff 0%, #f0f2f5 100%)',
      }}
    >
      <Card style={{ width: 420, boxShadow: '0 2px 8px rgba(0,0,0,0.08)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ marginBottom: 4 }}>
            智慧校园服务平台
          </Title>
          <span style={{ color: '#999' }}>请登录您的账号</span>
        </div>
        <Form
          layout="vertical"
          onFinish={handleFinish}
          size="large"
          initialValues={{ username: '', password: '' }}
        >
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 12 }}>
            <Button type="primary" htmlType="submit" block>
              登 录
            </Button>
          </Form.Item>
          <div style={{ textAlign: 'center', color: '#999', fontSize: 12, lineHeight: 1.8 }}>
            演示账号（密码均为 123456）
            <br />
            学生 student1 · 教师 teacher1 · 领导 leader1 · 管理员 admin
          </div>
        </Form>
      </Card>
    </div>
  )
}
