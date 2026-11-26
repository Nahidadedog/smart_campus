import { App, Button, Card, Col, List, Modal, Row, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAnnouncement, getAnnouncements, getNews, getNewsDetail } from '../../services/api'
import { getSession } from '../../utils/auth'
import { formatDateTime } from '../../utils/format'

const { Title, Paragraph } = Typography

export default function Home() {
  const navigate = useNavigate()
  const { message } = App.useMessage()

  const [announcements, setAnnouncements] = useState([])
  const [news, setNews] = useState([])
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState(null) // { kind: 'announcement' | 'news', data }

  const load = useCallback(async () => {
    try {
      const [announcementList, newsList] = await Promise.all([getAnnouncements(), getNews()])
      setAnnouncements(announcementList)
      setNews(newsList)
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const openAnnouncement = async (id) => {
    try {
      setDetail({ kind: 'announcement', data: await getAnnouncement(id) })
    } catch (err) {
      message.error(err.message)
    }
  }

  const openNews = async (id) => {
    try {
      const data = await getNewsDetail(id)
      setDetail({ kind: 'news', data })
      // 详情接口会让浏览量 +1，同步更新列表中的数字
      setNews((prev) =>
        prev.map((item) => (item.id === id ? { ...item, viewCount: data.viewCount } : item)),
      )
    } catch (err) {
      message.error(err.message)
    }
  }

  const session = getSession()
  const entryPath = session?.token ? `/${session.role.toLowerCase()}` : '/login'
  const entryText = session?.token ? '进入我的门户' : '登录系统'

  const renderList = (items, kind) => (
    <List
      loading={loading}
      dataSource={items}
      locale={{ emptyText: '暂无内容' }}
      renderItem={(item) => (
        <List.Item
          style={{ cursor: 'pointer' }}
          onClick={() => (kind === 'news' ? openNews(item.id) : openAnnouncement(item.id))}
        >
          <List.Item.Meta
            title={
              <span>
                {kind === 'announcement' && item.top && (
                  <Tag color="red" style={{ marginRight: 8 }}>
                    置顶
                  </Tag>
                )}
                {item.title}
              </span>
            }
            description={
              kind === 'news'
                ? `${formatDateTime(item.publishTime)} · 浏览 ${item.viewCount}`
                : `${item.type} · ${formatDateTime(item.publishTime)}`
            }
          />
        </List.Item>
      )}
    />
  )

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: 24 }}>
      <div style={{ textAlign: 'center', margin: '24px 0 40px' }}>
        <Title level={2} style={{ marginBottom: 8 }}>
          智慧校园服务平台
        </Title>
        <Paragraph type="secondary" style={{ fontSize: 16 }}>
          整合校园信息，提供便捷服务
        </Paragraph>
        <Button type="primary" size="large" onClick={() => navigate(entryPath)}>
          {entryText}
        </Button>
      </div>

      <Row gutter={24}>
        <Col xs={24} md={12}>
          <Card title="系统公告" size="small">
            {renderList(announcements, 'announcement')}
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title="学校新闻" size="small">
            {renderList(news, 'news')}
          </Card>
        </Col>
      </Row>

      <Modal
        title={detail?.kind === 'announcement' ? '公告详情' : '新闻详情'}
        open={Boolean(detail)}
        onCancel={() => setDetail(null)}
        footer={null}
        width={720}
      >
        {detail && (
          <article>
            <Title level={4} style={{ marginBottom: 8 }}>
              {detail.data.title}
            </Title>
            <Paragraph type="secondary" style={{ marginBottom: 16 }}>
              {detail.kind === 'announcement'
                ? `类型：${detail.data.type} · 发布时间：${formatDateTime(detail.data.publishTime)}`
                : `分类：${detail.data.category} · 发布时间：${formatDateTime(detail.data.publishTime)} · 浏览：${detail.data.viewCount}`}
            </Paragraph>
            <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.9 }}>{detail.data.content}</div>
          </article>
        )}
      </Modal>
    </div>
  )
}
