import { App, Select, Space, Table, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getMyGrades } from '../../services/api'

const { Title } = Typography

const LEVEL_COLOR = { 优: 'gold', 良: 'blue', 中: 'cyan', 及格: 'green', 不及格: 'red' }

export default function MyGrades() {
  const { message } = App.useMessage()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [semester, setSemester] = useState(undefined)

  const load = useCallback(async () => {
    try {
      setData(await getMyGrades())
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  // 学期选项从数据中推导，不再写死
  const semesterOptions = useMemo(() => {
    const unique = [...new Set(data.map((g) => g.semester).filter(Boolean))]
    unique.sort((a, b) => b.localeCompare(a))
    return unique.map((s) => ({ value: s, label: `${s} 学期` }))
  }, [data])

  const filteredData = useMemo(
    () => (semester ? data.filter((g) => g.semester === semester) : data),
    [data, semester],
  )

  const summary = useMemo(() => {
    if (!filteredData.length) return { avg: '-', passed: 0, credits: 0 }
    const passed = filteredData.filter((g) => g.score >= 60)
    const avg = filteredData.reduce((sum, g) => sum + (g.score ?? 0), 0) / filteredData.length
    const credits = passed.reduce((sum, g) => sum + (g.credits ?? 0), 0)
    return { avg: avg.toFixed(1), passed: passed.length, credits: credits.toFixed(1) }
  }, [filteredData])

  const columns = [
    { title: '课程名称', render: (_, r) => r.courseName },
    { title: '学分', dataIndex: 'credits', width: 80 },
    { title: '分数', dataIndex: 'score', width: 80 },
    {
      title: '等级',
      dataIndex: 'gradeLevel',
      width: 90,
      render: (v) => <Tag color={LEVEL_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    { title: '学期', dataIndex: 'semester', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (v) => <Tag color={v === '正常' ? 'green' : 'orange'}>{v}</Tag>,
    },
  ]

  return (
    <div>
      <Title level={4}>我的成绩</Title>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          placeholder="按学期筛选"
          style={{ width: 220 }}
          allowClear
          value={semester}
          onChange={setSemester}
          options={semesterOptions}
        />
        <Typography.Text type="secondary">
          平均分 {summary.avg} · 及格 {summary.passed}/{filteredData.length} · 已修学分{' '}
          {summary.credits}
        </Typography.Text>
      </Space>
      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
    </div>
  )
}
