import request from '@/utils/request'

export function scheduleList(projectId: number) {
  return request.get('/schedule/list', { params: { projectId } })
}
export function scheduleCreate(data: any) {
  return request.post('/schedule/create', data)
}
export function scheduleDelete(id: number) {
  return request.post('/schedule/delete', null, { params: { id } })
}
export function scheduleStatus(id: number, status: number) {
  return request.post('/schedule/status', null, { params: { id, status } })
}
