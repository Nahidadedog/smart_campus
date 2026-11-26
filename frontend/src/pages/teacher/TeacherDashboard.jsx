import { BookOutlined, ReadOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import { App, Card, Col, Descriptions, Row, Space, Statistic, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { getTeacherCourses, getTeacherProfile } from '../../services/api'

const { Title } = Typography

export default function TeacherDashboard() {
  const { message } = App.useMessage()
  const [profile, setProfile] = useState(null)
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [profileData, courseList] = await Promise.all([
        getTeacherProfile(),
        getTeacherCourses(),
      ])
      setProfile(profileData)
      setCourses(courseList)
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const totalStudents = courses.reduce((sum, c) => sum + (c.currentStudents ?? 0), 0)
  const currentSemester = courses[0]?.semester

  const profileItems = profile
    ? [
        { key: 'no', label: '工号', children: profile.teacherNo },
        { key: 'name', label: '姓名', children: profile.realName },
        { key: 'dept', label: '院系', children: profile.department },
        { key: 'title', label: '职称', children: <Tag color="blue">{profile.title}</Tag> },
        { key: 'type', label: '教师类型', children: profile.teacherType ?? '-' },
        { key: 'area', label: '研究方向', children: profile.researchArea },
        { key: 'office', label: '办公地点', children: profile.office },
        {
          key: 'contact',
          label: '联系方式',
          children: [profile.phone, profile.email].filter(Boolean).join(' / ') || '-',
        },
      ]
    : []

  return (
    <div>
      <Title level={4}>教师首页</Title>

      <Card
        loading={loading}
        style={{ marginBottom: 24 }}
        title={
          <Space>
            <UserOutlined /> 个人信息
          </Space>
        }
      >
        <Descriptions bordered column={{ xs: 1, sm: 2, md: 4 }} size="small" items={profileItems} />
      </Card>

      <Row gutter={24}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="授课门数"
              value={courses.length}
              prefix={<BookOutlined style={{ color: '#1677ff' }} />}
              styles={{ content: { color: '#1677ff', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="选课学生总数"
              value={totalStudents}
              suffix="人"
              prefix={<TeamOutlined style={{ color: '#52c41a' }} />}
              styles={{ content: { color: '#52c41a', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="当前授课学期"
              value={currentSemester ?? '-'}
              prefix={<ReadOutlined style={{ color: '#fa8c16' }} />}
              styles={{ content: { color: '#fa8c16', fontWeight: 'bold', fontSize: 24 } }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
