import request from '@/utils/request'

export function projectList(params: any) {
  return request.get('/project/list', { params })
}
export function projectCreate(data: any) {
  return request.post('/project/create', data)
}
export function projectUpdate(data: any) {
  return request.post('/project/update', data)
}
export function projectStatus(id: number, status: number) {
  return request.post('/project/status', null, { params: { id, status } })
}
export function projectDelete(id: number) {
  return request.post('/project/delete', null, { params: { id } })
}
