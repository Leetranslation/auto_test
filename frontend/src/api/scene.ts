import request from '@/utils/request'

export function sceneList(params: any) {
  return request.get('/scene/list', { params })
}
export function sceneCreate(data: any) {
  return request.post('/scene/create', data)
}
export function sceneUpdate(data: any) {
  return request.post('/scene/update', data)
}
export function sceneDelete(id: number) {
  return request.post('/scene/delete', null, { params: { id } })
}
export function sceneStatus(id: number, status: string) {
  return request.post('/scene/status', null, { params: { id, status } })
}
export function sceneVersionList(sceneId: number) {
  return request.get('/scene/version/list', { params: { sceneId } })
}
export function sceneGraph(sceneId: number, versionNo?: number) {
  return request.get('/scene/graph', { params: { sceneId, versionNo } })
}
export function sceneVersionSave(data: any) {
  return request.post('/scene/version/save', data)
}
