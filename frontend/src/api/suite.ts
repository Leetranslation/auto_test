import request from '@/utils/request'

export function suiteList(projectId: number) {
  return request.get('/suite/list', { params: { projectId } })
}
export function suiteCreate(data: any) {
  return request.post('/suite/create', data)
}
export function suiteDelete(id: number) {
  return request.post('/suite/delete', null, { params: { id } })
}
export function suiteItems(suiteId: number) {
  return request.get('/suite/items', { params: { suiteId } })
}
export function suiteAddItem(suiteId: number, targetType: string, targetId: number) {
  return request.post('/suite/item/add', null, { params: { suiteId, targetType, targetId } })
}
export function suiteRemoveItem(itemId: number) {
  return request.post('/suite/item/remove', null, { params: { itemId } })
}
export function suiteRun(suiteId: number) {
  return request.post('/suite/run', null, { params: { suiteId } })
}
