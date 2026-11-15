import request from '@/utils/request'

export function dashboardSummary() {
  return request.get('/dashboard/summary')
}
export function recordConsume(data: any) {
  return request.post('/record/consume', data)
}
