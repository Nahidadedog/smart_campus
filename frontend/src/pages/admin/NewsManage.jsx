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
  Table,
  Tag,
  Typography,
} from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { createNews, deleteNews, getAdminNews } from '../../services/api'
import { formatDateTime } from '../../utils/format'

const { Title } = Typography
const { TextArea } = Input

const CATEGORY_COLOR = { 校内新闻: 'blue', 学术动态: 'purple', 活动通知: 'green' }

export default function NewsManage() {
  const { message } = App.useMessage()
  const [form] = Form.useForm()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [searchText, setSearchText] = useState('')

  const load = useCallback(async () => {
    try {
      setData(await getAdminNews())
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
      await deleteNews(record.id)
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
      await createNews(values)
      message.success('新闻已发布')
      setModalOpen(false)
      form.resetFields()
      reload()
    } catch (err) {
      if (err?.message) message.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const filteredData = useMemo(() => {
    const keyword = searchText.trim().toLowerCase()
    if (!keyword) return data
    return data.filter(({ title, authorName, category }) =>
      [title, authorName, category].some((field) => field?.toLowerCase().includes(keyword)),
    )
  }, [data, searchText])

  const columns = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '分类',
      dataIndex: 'category',
      width: 100,
      render: (c) => <Tag color={CATEGORY_COLOR[c] ?? 'default'}>{c}</Tag>,
    },
    { title: '作者', dataIndex: 'authorName', width: 100 },
    { title: '浏览量', dataIndex: 'viewCount', width: 90, align: 'right' },
    { title: '发布时间', dataIndex: 'publishTime', width: 160, render: (v) => formatDateTime(v) },
    {
      title: '操作',
      width: 100,
      render: (_, r) => (
        <Popconfirm
          title="确定删除该新闻？"
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
          新闻管理
        </Title>
        <Space>
          <Input
            placeholder="搜索标题 / 分类 / 作者"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 280 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            发布新闻
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
          pagination={{ pageSize: 10, showTotal: (t) => `共 ${t} 条新闻` }}
        />
      </Card>

      <Modal
        title="发布新闻"
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
          initialValues={{ category: '校内新闻' }}
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
            <Input placeholder="新闻标题" showCount maxLength={200} />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <TextArea rows={6} placeholder="新闻正文" />
          </Form.Item>
          <Form.Item
            name="category"
            label="分类"
            rules={[{ required: true, message: '请选择分类' }]}
          >
            <Select
              style={{ width: 200 }}
              options={Object.keys(CATEGORY_COLOR).map((c) => ({ value: c }))}
            />
          </Form.Item>
          <Form.Item
            name="coverImage"
            label="封面图片地址"
            rules={[
              { max: 500, message: '地址不能超过 500 个字符' },
              { type: 'url', message: '请输入合法的 URL' },
            ]}
          >
            <Input placeholder="https://...（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
