// k6 抢课压测脚本
//
// 流程：setup 阶段用 admin 建一门只有 SEATS 个名额的课，并注册 STUDENTS 个学生；
//      default 阶段每个虚拟用户（一个学生）同时抢一次；
//      teardown 阶段等 MQ 把积压的落库指令消费完后，核对数据库已选人数不超过名额（零超卖）。
//
// 运行方式：
//   docker compose --profile loadtest run --rm loadtest
//   或本地：k6 run -e BASE_URL=http://localhost:8080 -e STUDENTS=300 -e SEATS=50 select-course.js
//
// 想对比不同模式，把后端分别以 STOCK_MODE/PERSIST_MODE 启动，再各跑一次，比较 p(95) 与 QPS。
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter } from 'k6/metrics'
import exec from 'k6/execution'

const BASE = __ENV.BASE_URL || 'http://localhost:8080'
const STUDENTS = Number(__ENV.STUDENTS || 300)
const SEATS = Number(__ENV.SEATS || 50)
const PASSWORD = '123456'

// 名额有限，大多数请求被 409 / 429 拒绝是预期行为；只有 5xx 和网络错误才算失败
http.setResponseCallback(http.expectedStatuses(200, 201, 409, 429))

const acquired = new Counter('selection_acquired')
const soldOut = new Counter('selection_sold_out')
const duplicate = new Counter('selection_duplicate')
const rateLimited = new Counter('selection_rate_limited')
const errors = new Counter('selection_error')

export const options = {
  scenarios: {
    // 所有虚拟用户各执行一次，同时起跑，模拟"整点开抢"
    rush: {
      executor: 'per-vu-iterations',
      vus: STUDENTS,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    // 全部请求（含 setup 的登录 / 注册）与只看选课接口（tags.name = select）的子指标
    http_req_duration: ['p(95)<1500'],
    'http_req_duration{name:select}': ['p(95)<1500'],
    checks: ['rate>0.95'],
  },
}

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' }

function login(username) {
  const res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ username, password: PASSWORD }),
    { headers: JSON_HEADERS, tags: { name: 'login' } })
  check(res, { 'login ok': (r) => r.status === 200 }) ||
    console.error(`login ${username} failed: ${res.status} ${res.body}`)
  return res.json('data.token')
}

export function setup() {
  const admin = login('admin')
  const code = `LT${Date.now()}`
  const courseRes = http.post(`${BASE}/api/admin/courses`, JSON.stringify({
    name: `抢课压测 ${code}`,
    code,
    credits: 1,
    hours: 16,
    type: '选修',
    teacherId: 1,
    semester: '2025-2026-2',
    schedule: '待定',
    classroom: '待定',
    maxStudents: SEATS,
  }), { headers: { ...JSON_HEADERS, Authorization: `Bearer ${admin}` }, tags: { name: 'create-course' } })
  if (!check(courseRes, { 'course created': (r) => r.status === 200 })) {
    console.error(`create course failed: ${courseRes.status} ${courseRes.body}`)
    exec.test.abort('无法创建压测课程，请确认 admin 账号与 /api/admin/courses 接口')
  }
  const courseId = courseRes.json('data.id')

  // 预热名额缓存：Redis 模式下避免第一个请求触发懒加载；db 模式下是空操作
  http.post(`${BASE}/api/admin/cache/courses/refresh`, null,
    { headers: { Authorization: `Bearer ${admin}` }, tags: { name: 'warm-up' } })

  // 并发注册压测学生，每批 20 个
  const tokens = []
  const BATCH = 20
  for (let start = 0; start < STUDENTS; start += BATCH) {
    const requests = []
    for (let i = start; i < Math.min(start + BATCH, STUDENTS); i++) {
      const suffix = `${Date.now().toString(36)}${i}`
      requests.push(['POST', `${BASE}/api/auth/register`, JSON.stringify({
        username: `lt_${suffix}`,
        password: PASSWORD,
        realName: `压测学生${i}`,
        studentNo: `LT${suffix.toUpperCase()}`,
        department: '压测学院',
        enrollmentYear: '2026',
      }), { headers: JSON_HEADERS, tags: { name: 'register' } }])
    }
    const responses = http.batch(requests)
    for (const res of responses) {
      if (!check(res, { 'register ok': (r) => r.status === 200 })) {
        console.error(`register failed: ${res.status} ${res.body.slice(0, 200)}`)
      }
      tokens.push(res.json('data.token'))
    }
  }
  return { courseId, tokens }
}

export default function (data) {
  const token = data.tokens[exec.vu.idInTest - 1]
  if (!token) {
    return
  }
  const res = http.post(`${BASE}/api/student/courses/${data.courseId}`, null, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: 'select' },
  })
  const message = res.json && res.json() && res.json().message ? res.json().message : ''
  if (res.status === 200) {
    acquired.add(1)
  } else if (res.status === 429) {
    rateLimited.add(1)
  } else if (res.status === 0) {
    // 没拿到响应：连接被拒、读超时等，后端已经出问题了，不能混进"重复选课"里
    errors.add(1)
    console.error(`select failed: ${res.error || 'no response'}`)
  } else if (message.includes('名额已满')) {
    soldOut.add(1)
  } else {
    duplicate.add(1)
  }
  check(res, { 'status is 200/409/429': (r) => [200, 409, 429].includes(r.status) })
}

export function teardown(data) {
  const admin = login('admin')
  // 等 MQ 消费者把积压的落库指令写完（同步模式立刻就是最终值）：最多等 30 秒，数字稳定两轮即认为落库完成
  let current = -1
  let stable = 0
  let res
  for (let i = 0; i < 15 && stable < 2; i++) {
    sleep(2)
    res = http.get(`${BASE}/api/public/courses/${data.courseId}`,
      { headers: { Authorization: `Bearer ${admin}` }, tags: { name: 'verify' } })
    const now = res.json('data.currentStudents')
    stable = now === current ? stable + 1 : 0
    current = now
    if (current === SEATS) break
  }
  const participants = data.tokens.filter((t) => !!t).length
  console.log(`压测结果：名额 ${SEATS}，参与 ${participants}，数据库已选 ${current}`)

  check(res, {
    '零超卖：已选人数 <= 名额': () => current <= SEATS,
    '名额全部落库（参与人数 >= 名额时已选人数 == 名额）': () => participants < SEATS || current === SEATS,
  })
}
