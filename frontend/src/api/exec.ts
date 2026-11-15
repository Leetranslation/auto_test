import request from '@/utils/request'

export function execRun(data: any) {
  return request.post('/exec/run', data)
}
export function execList(params: any) {
  return request.get('/exec/list', { params })
}
export function execDetail(executionId: number) {
  return request.get('/exec/detail', { params: { executionId } })
}
