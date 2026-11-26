import { App, Button, Table, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getTeacherCourses } from '../../services/api'

const { Title } = Typography

export default function MyTeachCourses() {
  const navigate = useNavigate()
  const { message } = App.useMessage()
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setCourses(await getTeacherCourses())
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const columns = [
    { title: '课程名称', dataIndex: 'name' },
    { title: '课程代码', dataIndex: 'code', width: 100 },
    { title: '学分', dataIndex: 'credits', width: 70 },
    { title: '学时', dataIndex: 'hours', width: 70 },
    { title: '学期', dataIndex: 'semester', width: 110 },
    { title: '上课时间', dataIndex: 'schedule' },
    { title: '教室', dataIndex: 'classroom' },
    {
      title: '选课人数',
      width: 100,
      render: (_, r) => `${r.currentStudents}/${r.maxStudents}`,
    },
    {
      title: '操作',
      width: 110,
      render: (_, r) => (
        <Button type="primary" size="small" onClick={() => navigate(`/teacher/grades/${r.id}`)}>
          成绩管理
        </Button>
      ),
    },
  ]

  return (
    <div>
      <Title level={4}>我的授课</Title>
      <Table
        columns={columns}
        dataSource={courses}
        rowKey="id"
        loading={loading}
        scroll={{ x: 900 }}
      />
    </div>
  )
}
