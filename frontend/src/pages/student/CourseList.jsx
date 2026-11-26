import { SearchOutlined } from '@ant-design/icons'
import { App, Button, Input, Space, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getCourses, getMyCourses, selectCourse } from '../../services/api'

const { Title } = Typography

const matches = (course, keyword) => {
  const text = [course.name, course.code, course.teacherName]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
  return text.includes(keyword.toLowerCase())
}

export default function CourseList() {
  const { message } = App.useMessage()
  const [courses, setCourses] = useState([])
  const [selectedIds, setSelectedIds] = useState(new Set())
  const [loading, setLoading] = useState(true)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      const [courseList, myCourses] = await Promise.all([getCourses(), getMyCourses()])
      setCourses(courseList)
      setSelectedIds(new Set(myCourses.map((item) => item.course.id)))
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  /** 用户操作后手动刷新：在事件回调里切换 loading，避免在 effect 里同步 setState */
  const reload = () => {
    setLoading(true)
    load()
  }

  const handleSelect = async (course) => {
    try {
      await selectCourse(course.id)
      message.success(`已选课：${course.name}`)
      reload()
    } catch (err) {
      message.error(err.message)
    }
  }

  const filteredCourses = useMemo(
    () => (searchText.trim() ? courses.filter((c) => matches(c, searchText.trim())) : courses),
    [courses, searchText],
  )

  const columns = [
    { title: '课程名称', dataIndex: 'name' },
    { title: '课程代码', dataIndex: 'code', width: 100 },
    { title: '教师', dataIndex: 'teacherName', width: 100 },
    { title: '学分', dataIndex: 'credits', width: 70 },
    { title: '学时', dataIndex: 'hours', width: 70 },
    {
      title: '类型',
      dataIndex: 'type',
      width: 80,
      render: (t) => <Tag color={t === '必修' ? 'blue' : 'green'}>{t}</Tag>,
    },
    { title: '学期', dataIndex: 'semester', width: 110 },
    { title: '上课时间', dataIndex: 'schedule' },
    { title: '教室', dataIndex: 'classroom' },
    {
      title: '人数',
      width: 100,
      render: (_, r) => (
        <Tag color={r.currentStudents >= r.maxStudents ? 'red' : 'green'}>
          {r.currentStudents}/{r.maxStudents}
        </Tag>
      ),
    },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, r) => {
        const selected = selectedIds.has(r.id)
        const full = r.currentStudents >= r.maxStudents
        return (
          <Button
            type="primary"
            size="small"
            disabled={selected || full}
            onClick={() => handleSelect(r)}
          >
            {selected ? '已选' : '选课'}
          </Button>
        )
      },
    },
  ]

  return (
    <div>
      <Title level={4}>课程列表</Title>
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="搜索课程名称、代码或教师"
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: 320 }}
          allowClear
        />
      </Space>
      <Table
        columns={columns}
        dataSource={filteredCourses}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 门课程` }}
        scroll={{ x: 1000 }}
      />
    </div>
  )
}
