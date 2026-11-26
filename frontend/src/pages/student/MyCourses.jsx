import { SearchOutlined } from '@ant-design/icons'
import { App, Button, Input, Popconfirm, Table, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { dropCourse, getMyCourses } from '../../services/api'
import { formatDateTime } from '../../utils/format'

const { Title } = Typography

export default function MyCourses() {
  const { message } = App.useMessage()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getMyCourses())
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

  const handleDrop = async (course) => {
    try {
      await dropCourse(course.id)
      message.success(`已退选：${course.name}`)
      reload()
    } catch (err) {
      message.error(err.message)
    }
  }

  const filteredData = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return data
    return data.filter(
      ({ course }) =>
        course.name?.toLowerCase().includes(keyword) ||
        course.code?.toLowerCase().includes(keyword),
    )
  }, [data, searchText])

  const columns = [
    { title: '课程名称', render: (_, r) => r.course.name },
    { title: '课程代码', render: (_, r) => r.course.code, width: 100 },
    { title: '教师', render: (_, r) => r.course.teacherName, width: 100 },
    { title: '学分', render: (_, r) => r.course.credits, width: 70 },
    { title: '上课时间', render: (_, r) => r.course.schedule },
    { title: '教室', render: (_, r) => r.course.classroom },
    { title: '选课时间', render: (_, r) => formatDateTime(r.createTime), width: 160 },
    {
      title: '操作',
      width: 100,
      render: (_, r) => (
        <Popconfirm
          title="确定退选该课程？"
          onConfirm={() => handleDrop(r.course)}
          okText="退选"
          cancelText="取消"
        >
          <Button danger size="small">
            退选
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div>
      <Title level={4}>我的选课</Title>
      <Input
        placeholder="搜索课程名称或代码"
        prefix={<SearchOutlined />}
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        style={{ width: 320, marginBottom: 16 }}
        allowClear
      />
      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 门课程` }}
      />
    </div>
  )
}
