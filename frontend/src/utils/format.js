import dayjs from 'dayjs'

/** 后端返回 ISO-8601（如 2026-11-26T10:00:00），统一在展示层格式化 */
export function formatDateTime(value, pattern = 'YYYY-MM-DD HH:mm') {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format(pattern) : '-'
}

export function formatMoney(value) {
  if (value === null || value === undefined) return '-'
  return `¥${Number(value).toFixed(2)}`
}
