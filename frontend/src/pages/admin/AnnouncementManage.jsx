import { DeleteOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import {
  App,
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { createAnnouncement, deleteAnnouncement, getAdminAnnouncements } from '../../services/api'
import { formatDateTime } from '../../utils/format'

const { Title } = Typography
const { TextArea } = Input

const ANNOUNCEMENT_TYPES = ['学校公告', '院系公告', '教务通知']

const TYPE_COLOR = { 学校公告: 'red', 院系公告: 'blue', 教务通知: 'orange' }

export default function AnnouncementManage() {
  const { message } = App.useMessage()
  const [form] = Form.useForm()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getAdminAnnouncements())
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

  const handleDelete = async (record) => {
    try {
      await deleteAnnouncement(record.id)
      message.success(`已删除：${record.title}`)
      reload()
    } catch (err) {
      message.error(err.message)
    }
  }

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await createAnnouncement({ ...values, type: values.type ?? '学校公告' })
      message.success('公告已发布')
      setModalOpen(false)
      form.resetFields()
      reload()
    } catch (err) {
      // validateFields 抛出的是表单校验错误对象，没有 message 字段
      if (err?.message) message.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const filteredData = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return data
    return data.filter(({ title, publisherName, type }) =>
      [title, publisherName, type].some((field) => field?.toLowerCase().includes(keyword)),
    )
  }, [data, searchText])

  const columns = [
    {
      title: '标题',
      render: (_, r) => (
        <Space>
          {r.top && <Tag color="gold">置顶</Tag>}
          <span>{r.title}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 100,
      render: (t) => <Tag color={TYPE_COLOR[t] ?? 'default'}>{t}</Tag>,
    },
    { title: '发布人', dataIndex: 'publisherName', width: 100 },
    { title: '发布时间', dataIndex: 'publishTime', width: 160, render: (v) => formatDateTime(v) },
    {
      title: '操作',
      width: 100,
      render: (_, r) => (
        <Popconfirm
          title="确定删除该公告？"
          onConfirm={() => handleDelete(r)}
          okText="删除"
          okButtonProps={{ danger: true }}
          cancelText="取消"
        >
          <Button danger size="small" icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      ),
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
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <Title level={4} style={{ margin: 0 }}>
          公告管理
        </Title>
        <Space>
          <Input
            placeholder="搜索标题 / 类型 / 发布人"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 280 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            发布公告
          </Button>
        </Space>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          size="middle"
          pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 条公告` }}
        />
      </Card>

      <Modal
        title="发布公告"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
        okText="发布"
        cancelText="取消"
        confirmLoading={saving}
        destroyOnHidden
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ type: '学校公告', top: false }}
          preserve={false}
        >
          <Form.Item
            name="title"
            label="标题"
            rules={[
              { required: true, message: '请输入标题' },
              { max: 200, message: '标题不能超过 200 字' },
            ]}
          >
            <Input placeholder="公告标题" showCount maxLength={200} />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <TextArea rows={6} placeholder="公告正文" />
          </Form.Item>
          <Space size="large">
            <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
              <Select
                style={{ width: 160 }}
                options={ANNOUNCEMENT_TYPES.map((t) => ({ value: t }))}
              />
            </Form.Item>
            <Form.Item name="top" label="置顶" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}
