import { BookOutlined, ReadOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import { App, Card, Col, Descriptions, Empty, Row, Space, Statistic, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { getCourseStats, getStudentStats, getTeacherStats } from '../../services/api'

const { Title, Text } = Typography

/** 把 { key: count } 渲染成 "标签 数量" 列表 */
function Distribution({ data, color }) {
  const entries = Object.entries(data ?? {})
  if (entries.length === 0)
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" />
  return (
    <Space direction="vertical" size="small">
      {entries.map(([key, count]) => (
        <Space key={key} size="middle">
          <Tag color={color}>{key}</Tag>
          <Text strong>{count}</Text>
        </Space>
      ))}
    </Space>
  )
}

export default function LeaderDashboard() {
  const { message } = App.useMessage()
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [students, teachers, courses] = await Promise.all([
        getStudentStats(),
        getTeacherStats(),
        getCourseStats(),
      ])
      setStats({ students, teachers, courses })
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div>
      <Title level={4}>数据概览</Title>

      <Row gutter={[24, 24]}>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="学生总数"
              value={stats?.students.totalStudents ?? 0}
              prefix={<UserOutlined style={{ color: '#1677ff' }} />}
              styles={{ content: { color: '#1677ff', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="教师总数"
              value={stats?.teachers.totalTeachers ?? 0}
              prefix={<TeamOutlined style={{ color: '#52c41a' }} />}
              styles={{ content: { color: '#52c41a', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="开课门数"
              value={stats?.courses.totalCourses ?? 0}
              prefix={<ReadOutlined style={{ color: '#fa8c16' }} />}
              styles={{ content: { color: '#fa8c16', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card loading={loading}>
            <Statistic
              title="有效选课记录"
              value={stats?.courses.totalSelections ?? 0}
              prefix={<BookOutlined style={{ color: '#722ed1' }} />}
              styles={{ content: { color: '#722ed1', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="学生分布" loading={loading}>
            <Descriptions
              column={1}
              size="small"
              items={[
                {
                  key: 'dept',
                  label: '按院系',
                  children: <Distribution data={stats?.students.byDepartment} color="blue" />,
                },
                {
                  key: 'status',
                  label: '按状态',
                  children: <Distribution data={stats?.students.byStatus} color="green" />,
                },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="教师分布" loading={loading}>
            <Descriptions
              column={1}
              size="small"
              items={[
                {
                  key: 'type',
                  label: '按类型',
                  children: <Distribution data={stats?.teachers.byType} color="purple" />,
                },
                {
                  key: 'dept',
                  label: '按院系',
                  children: <Distribution data={stats?.teachers.byDepartment} color="orange" />,
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
