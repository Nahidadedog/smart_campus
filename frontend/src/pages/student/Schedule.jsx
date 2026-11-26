import { App, Table, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { getMyCourses } from '../../services/api'

const { Title } = Typography

const DAYS = ['周一', '周二', '周三', '周四', '周五']
const PERIODS = ['1-2节', '3-4节', '5-6节', '7-8节', '9-10节']
const DAY_INDEX = Object.fromEntries(DAYS.map((d, i) => [d, i]))

/** 解析「周一3-4节,周三3-4节」格式的上课时间 */
function parseSchedule(schedule) {
  if (!schedule || schedule === '待定') return []
  return schedule
    .split(/[,，]/)
    .map((part) => part.trim().match(/^(.+?)(\d+-\d+节)$/))
    .filter(Boolean)
    .map(([, day, period]) => ({ day: DAY_INDEX[day], period }))
    .filter(({ day }) => day !== undefined)
}

export default function Schedule() {
  const { message } = App.useMessage()
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      setCourses(await getMyCourses())
    } catch (err) {
      message.error(err.message)
    } finally {
      setLoading(false)
    }
  }, [message])

  useEffect(() => {
    load()
  }, [load])

  const cells = useMemo(() => {
    const grid = Object.fromEntries(PERIODS.map((p) => [p, {}]))
    courses.forEach(({ course }) => {
      parseSchedule(course.schedule).forEach(({ day, period }) => {
        if (grid[period]) grid[period][day] = course
      })
    })
    return grid
  }, [courses])

  const columns = [
    { title: '节次', dataIndex: 'period', width: 90, fixed: 'left' },
    ...DAYS.map((name, dayIndex) => ({
      title: name,
      render: (_, { period }) => {
        const course = cells[period][dayIndex]
        if (!course) return <span style={{ color: '#d9d9d9' }}>—</span>
        return (
          <div>
            <div style={{ fontWeight: 600, color: '#1677ff' }}>{course.name}</div>
            <div style={{ fontSize: 12, color: '#666' }}>{course.classroom}</div>
            <div style={{ fontSize: 12, color: '#999' }}>{course.teacherName}</div>
          </div>
        )
      },
    })),
  ]

  return (
    <div>
      <Title level={4}>我的课程表</Title>
      <Table
        columns={columns}
        dataSource={PERIODS.map((p) => ({ period: p, key: p }))}
        pagination={false}
        bordered
        loading={loading}
        scroll={{ x: 800 }}
      />
    </div>
  )
}
