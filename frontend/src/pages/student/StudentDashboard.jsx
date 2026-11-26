import { BookOutlined, FileTextOutlined, TrophyOutlined, UserOutlined } from '@ant-design/icons'
import { App, Card, Col, Descriptions, Row, Space, Statistic, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { getMyCourses, getMyGrades, getStudentProfile } from '../../services/api'
import { formatMoney } from '../../utils/format'

const { Title } = Typography

export default function StudentDashboard() {
  const { message } = App.useMessage()
  const [profile, setProfile] = useState(null)
  const [courseCount, setCourseCount] = useState(0)
  const [gradeCount, setGradeCount] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [profileData, courses, grades] = await Promise.all([
        getStudentProfile(),
        getMyCourses(),
        getMyGrades(),
      ])
      setProfile(profileData)
      setCourseCount(courses.length)
      setGradeCount(grades.length)
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const profileItems = profile
    ? [
        { key: 'no', label: '学号', children: profile.studentNo },
        { key: 'name', label: '姓名', children: profile.realName },
        { key: 'dept', label: '院系', children: profile.department },
        { key: 'major', label: '专业', children: profile.major },
        { key: 'class', label: '班级', children: profile.className },
        { key: 'year', label: '入学年份', children: profile.enrollmentYear },
        {
          key: 'status',
          label: '状态',
          children: (
            <Tag color={profile.status === '在读' ? 'green' : 'orange'}>{profile.status}</Tag>
          ),
        },
        { key: 'balance', label: '一卡通余额', children: formatMoney(profile.cardBalance) },
      ]
    : []

  return (
    <div>
      <Title level={4}>学生首页</Title>

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
              title="本学期已选课程"
              value={courseCount}
              prefix={<BookOutlined style={{ color: '#1677ff' }} />}
              styles={{ content: { color: '#1677ff', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="已出成绩课程"
              value={gradeCount}
              prefix={<TrophyOutlined style={{ color: '#52c41a' }} />}
              styles={{ content: { color: '#52c41a', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="已修学分"
              value={profile?.totalCredits ?? 0}
              suffix="学分"
              prefix={<FileTextOutlined style={{ color: '#fa8c16' }} />}
              styles={{ content: { color: '#fa8c16', fontWeight: 'bold' } }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}
