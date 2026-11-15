import request from '@/utils/request'

export function envList(projectId: number) {
  return request.get('/env/list', { params: { projectId } })
}
export function envCreate(data: any) {
  return request.post('/env/create', data)
}
export function envUpdate(data: any) {
  return request.post('/env/update', data)
}
export function envDelete(id: number) {
  return request.post('/env/delete', null, { params: { id } })
}
export function variableList(projectId: number, environmentId: number) {
  return request.get('/env/variables', { params: { projectId, environmentId } })
}
export function variableSave(data: any) {
  return request.post('/env/variable/save', data)
}
export function variableDelete(id: number) {
  return request.post('/env/variable/delete', null, { params: { id } })
}
