import { SearchOutlined } from '@ant-design/icons'
import { App, Input, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getAllTeachers } from '../../services/api'

const { Title } = Typography

const TITLE_COLOR = { 教授: 'purple', 副教授: 'blue', 讲师: 'green' }

export default function TeacherManage() {
  const { message } = App.useMessage()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getAllTeachers())
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
    return data.filter(({ teacherNo, realName, department, title, researchArea }) =>
      [teacherNo, realName, department, title, researchArea].some((field) =>
        field?.toLowerCase().includes(keyword),
      ),
    )
  }, [data, searchText])

  const columns = [
    { title: '工号', dataIndex: 'teacherNo', width: 110 },
    { title: '姓名', dataIndex: 'realName', width: 100 },
    { title: '院系', dataIndex: 'department' },
    {
      title: '职称',
      dataIndex: 'title',
      width: 100,
      render: (t) => <Tag color={TITLE_COLOR[t] ?? 'default'}>{t}</Tag>,
    },
    { title: '教师类型', dataIndex: 'teacherType', width: 110 },
    { title: '研究方向', dataIndex: 'researchArea' },
    { title: '办公地点', dataIndex: 'office', width: 130 },
    { title: '政治面貌', dataIndex: 'politicalStatus', width: 100 },
    { title: '联系电话', dataIndex: 'phone', width: 130 },
    { title: '邮箱', dataIndex: 'email', width: 200 },
  ]

  return (
    <div>
      <Title level={4}>教师名册</Title>
      <Input
        placeholder="搜索工号 / 姓名 / 院系 / 职称 / 研究方向"
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
        pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 名教师` }}
      />
    </div>
  )
}
