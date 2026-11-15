import request from '@/utils/request'

export function serviceList(projectId: number) {
  return request.get('/api/service/list', { params: { projectId } })
}
export function serviceCreate(data: any) {
  return request.post('/api/service/create', data)
}
export function serviceDelete(id: number) {
  return request.post('/api/service/delete', null, { params: { id } })
}
export function definitionList(projectId: number, serviceId?: number) {
  return request.get('/api/definition/list', { params: { projectId, serviceId } })
}
export function definitionCreate(data: any) {
  return request.post('/api/definition/create', data)
}
export function definitionUpdate(data: any) {
  return request.post('/api/definition/update', data)
}
export function definitionDelete(id: number) {
  return request.post('/api/definition/delete', null, { params: { id } })
}
export function versionList(definitionId: number) {
  return request.get('/api/definition/version/list', { params: { definitionId } })
}
export function versionSave(data: any) {
  return request.post('/api/definition/version/save', data)
}
export function debugSend(data: any) {
  return request.post('/debug/send', data)
}
