import request from '@/utils/request'

export function appList(projectId: number) {
  return request.get('/ui/app/list', { params: { projectId } })
}
export function appCreate(data: any) {
  return request.post('/ui/app/create', data)
}
export function appUpdate(data: any) {
  return request.post('/ui/app/update', data)
}
export function appDelete(id: number) {
  return request.post('/ui/app/delete', null, { params: { id } })
}
export function pageList(projectId: number, applicationId?: number) {
  return request.get('/ui/page/list', { params: { projectId, applicationId } })
}
export function pageCreate(data: any) {
  return request.post('/ui/page/create', data)
}
export function pageUpdate(data: any) {
  return request.post('/ui/page/update', data)
}
export function pageDelete(id: number) {
  return request.post('/ui/page/delete', null, { params: { id } })
}
export function locatorList(pageObjectId: number) {
  return request.get('/ui/locator/list', { params: { pageObjectId } })
}
export function locatorSave(data: any) {
  return request.post('/ui/locator/save', data)
}
export function locatorDelete(id: number) {
  return request.post('/ui/locator/delete', null, { params: { id } })
}
