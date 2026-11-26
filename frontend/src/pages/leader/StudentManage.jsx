import { SearchOutlined } from '@ant-design/icons'
import { App, Input, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getAllStudents } from '../../services/api'

const { Title } = Typography

const STATUS_COLOR = { 在读: 'green', 休学: 'orange', 毕业: 'blue', 退学: 'red' }

export default function StudentManage() {
  const { message } = App.useMessage()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getAllStudents())
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const filteredData = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return data
    return data.filter(({ studentNo, realName, department, major, className }) =>
      [studentNo, realName, department, major, className].some((field) =>
        field?.toLowerCase().includes(keyword),
      ),
    )
  }, [data, searchText])

  const columns = [
    { title: '学号', dataIndex: 'studentNo', width: 110 },
    { title: '姓名', dataIndex: 'realName', width: 100 },
    { title: '院系', dataIndex: 'department' },
    { title: '专业', dataIndex: 'major' },
    { title: '班级', dataIndex: 'className', width: 110 },
    { title: '入学年份', dataIndex: 'enrollmentYear', width: 90 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status) => <Tag color={STATUS_COLOR[status] ?? 'default'}>{status}</Tag>,
    },
    { title: '已修学分', dataIndex: 'totalCredits', width: 90, align: 'right' },
    { title: '宿舍', dataIndex: 'dormitory', width: 110 },
    { title: '联系电话', dataIndex: 'phone', width: 130 },
  ]

  return (
    <div>
      <Title level={4}>学生名册</Title>
      <Input
        placeholder="搜索学号 / 姓名 / 院系 / 专业 / 班级"
        prefix={<SearchOutlined />}
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        style={{ width: 360, marginBottom: 16 }}
        allowClear
      />
      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        size="middle"
        scroll={{ x: 'max-content' }}
        pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 名学生` }}
      />
    </div>
  )
}
