import request from './axios' // 已封装的 axios 实例，baseURL = '/api'

// 保存拓扑图数据（需携带 projectId、nodes、links、customElements）
export const saveTopology = (data) => {
  return request.post('/topology/save', data)
}

// 加载拓扑图数据（根据 projectId 查询）
export const loadTopology = ({ projectId }) => {
  return request.get('/topology/load', {
    params: { projectId }
  })
}