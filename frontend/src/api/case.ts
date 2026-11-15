import request from '@/utils/request'

export function caseList(params: any) {
  return request.get('/case/list', { params })
}
export function caseCreate(data: any) {
  return request.post('/case/create', data)
}
export function caseUpdate(data: any) {
  return request.post('/case/update', data)
}
export function caseDelete(id: number) {
  return request.post('/case/delete', null, { params: { id } })
}
export function caseStatus(id: number, status: string) {
  return request.post('/case/status', null, { params: { id, status } })
}
export function caseVersionList(testCaseId: number) {
  return request.get('/case/version/list', { params: { testCaseId } })
}
export function caseVersionSave(data: any) {
  return request.post('/case/version/save', data)
}
