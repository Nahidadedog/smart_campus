import {
  App,
  AutoComplete,
  Button,
  Form,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCourse, getCourseGrades, getCourseStudents, saveGrade } from '../../services/api'

const { Title } = Typography

const LEVEL_COLOR = { 优: 'gold', 良: 'blue', 中: 'cyan', 及格: 'green', 不及格: 'red' }

export default function GradeManage() {
  const { courseId } = useParams()
  const navigate = useNavigate()
  const { message } = App.useMessage()
  const [form] = Form.useForm()

  const [course, setCourse] = useState(null)
  const [grades, setGrades] = useState([])
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const [courseData, gradeList, studentList] = await Promise.all([
        getCourse(courseId),
        getCourseGrades(courseId),
        getCourseStudents(courseId),
      ])
      setCourse(courseData)
      setGrades(gradeList)
      setStudents(studentList)
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [courseId, message])

  useEffect(() => {
    load()
  }, [load])

  /** 用户操作后手动刷新：在事件回调里切换 loading，避免在 effect 里同步 setState */
  const reload = () => {
    setLoading(true)
    load()
  }

  const studentOptions = useMemo(
    () =>
      students.map((s) => ({
        value: s.studentId,
        label: `${s.studentName} · ${s.studentNo}`,
      })),
    [students],
  )

  // 学期输入框的建议项：课程所属学期 + 该课程已有的成绩学期（去重）
  const semesterOptions = useMemo(
    () =>
      [course?.semester, ...grades.map((g) => g.semester)]
        .filter(Boolean)
        .filter((v, i, arr) => arr.indexOf(v) === i)
        .map((v) => ({ value: v })),
    [course, grades],
  )

  const openCreateModal = () => {
    form.setFieldsValue({ semester: course?.semester ?? '2025-2026-1' })
    setModalOpen(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await saveGrade(courseId, values)
      message.success('成绩已保存')
      setModalOpen(false)
      form.resetFields()
      reload()
    } catch (err) {
      // validateFields 抛出的是校验错误对象，没有 message 字段
      if (err?.message) message.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const columns = [
    { title: '学号', dataIndex: 'studentNo', width: 120 },
    { title: '学生', dataIndex: 'studentName', width: 120 },
    { title: '分数', dataIndex: 'score', width: 80 },
    {
      title: '等级',
      dataIndex: 'gradeLevel',
      width: 90,
      render: (v) => <Tag color={LEVEL_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    { title: '学分', dataIndex: 'credits', width: 80 },
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
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          成绩管理{course ? `：${course.name}（${course.code}）` : ''}
        </Title>
        <Space>
          <Button type="primary" onClick={openCreateModal}>
            录入 / 更新成绩
          </Button>
          <Button onClick={() => navigate('/teacher/courses')}>返回</Button>
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={grades}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title="录入 / 更新成绩"
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText="保存"
        cancelText="取消"
        confirmLoading={saving}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="studentId"
            label="学生"
            rules={[{ required: true, message: '请选择学生' }]}
          >
            <Select
              placeholder="请选择选课学生"
              options={studentOptions}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="score" label="分数" rules={[{ required: true, message: '请输入分数' }]}>
            <InputNumber min={0} max={100} step={0.5} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="semester"
            label="学期"
            rules={[
              { required: true, message: '请输入学期' },
              { pattern: /^\d{4}-\d{4}-[12]$/, message: '格式应为 2025-2026-1' },
            ]}
          >
            <AutoComplete placeholder="如 2025-2026-1" options={semesterOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
