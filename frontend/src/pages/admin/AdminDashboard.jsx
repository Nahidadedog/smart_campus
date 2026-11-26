import {
  BellOutlined,
  BookOutlined,
  CoffeeOutlined,
  ReadOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { App, Card, Col, Row, Statistic, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import {
  getAdminAnnouncements,
  getAdminNews,
  getCourseStats,
  getStudentStats,
  getTeacherStats,
} from '../../services/api'

const { Title } = Typography

export default function AdminDashboard() {
  const { message } = App.useMessage()
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [students, teachers, courses, announcements, news] = await Promise.all([
        getStudentStats(),
        getTeacherStats(),
        getCourseStats(),
        getAdminAnnouncements(),
        getAdminNews(),
      ])
      setStats({
        students: students.totalStudents,
        teachers: teachers.totalTeachers,
        courses: courses.totalCourses,
        selections: courses.totalSelections,
        announcements: announcements.length,
        news: news.length,
      })
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const items = [
    {
      title: '学生总数',
      value: stats?.students,
      icon: <UserOutlined style={{ color: '#1677ff' }} />,
      color: '#1677ff',
    },
    {
      title: '教师总数',
      value: stats?.teachers,
      icon: <TeamOutlined style={{ color: '#52c41a' }} />,
      color: '#52c41a',
    },
    {
      title: '开课门数',
      value: stats?.courses,
      icon: <ReadOutlined style={{ color: '#fa8c16' }} />,
      color: '#fa8c16',
    },
    {
      title: '有效选课记录',
      value: stats?.selections,
      icon: <BookOutlined style={{ color: '#722ed1' }} />,
      color: '#722ed1',
    },
    {
      title: '公告数量',
      value: stats?.announcements,
      icon: <BellOutlined style={{ color: '#eb2f96' }} />,
      color: '#eb2f96',
    },
    {
      title: '新闻数量',
      value: stats?.news,
      icon: <CoffeeOutlined style={{ color: '#13c2c2' }} />,
      color: '#13c2c2',
    },
  ]

  return (
    <div>
      <Title level={4}>管理后台</Title>
      <Row gutter={[24, 24]}>
        {items.map((item) => (
          <Col key={item.title} xs={24} sm={12} lg={8}>
            <Card loading={loading}>
              <Statistic
                title={item.title}
                value={item.value ?? 0}
                prefix={item.icon}
                styles={{ content: { color: item.color, fontWeight: 'bold' } }}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}
