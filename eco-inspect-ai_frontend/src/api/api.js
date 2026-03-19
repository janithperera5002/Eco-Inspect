import { auth } from './auth.js'

async function request(method, path, body) {
  const headers = { 
    'Content-Type': 'application/json' 
  }
  const token = auth.getToken()
  if (token) {
    headers['Authorization'] = 'Bearer ' + token
  }
  const options = { method, headers }
  if (body) {
    options.body = JSON.stringify(body)
  }
  const res = await fetch('/api' + path, options)
  if (res.status === 401) {
    auth.logout(); return
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 
      'Request failed: ' + res.status)
  }
  return res.json()
}

export const reportAPI = {
  getAll: (p) => request('GET', '/reports' 
    + (p ? '?' + new URLSearchParams(p) : '')),
  getById: (id) => request('GET', 
    '/reports/' + id),
  getAnalysis: (id) => request('GET', 
    '/reports/' + id + '/analysis'),
  create: (data) => request('POST', 
    '/reports', data),
  getForMap: () => request('GET', '/reports/map')
}

export const caseAPI = {
  getAll: (p) => request('GET', '/cases' 
    + (p ? '?' + new URLSearchParams(p) : '')),
  getById: (id) => request('GET', 
    '/cases/' + id),
  create: (data) => request('POST', 
    '/cases', data),
  updateStatus: (id, status) => request('PUT', 
    '/cases/' + id + '/status', { status }),
  addUpdate: (id, data) => request('POST', 
    '/cases/' + id + '/updates', data),
  getTimeline: (id) => request('GET', 
    '/cases/' + id + '/updates'),
  resolve: (id, notes) => request('PUT', 
    '/cases/' + id + '/resolve', 
    { resolutionNotes: notes })
}

export const analyticsAPI = {
  getSummary: () => request('GET', 
    '/analytics/summary'),
  getByRegion: () => request('GET', 
    '/analytics/by-region'),
  getUrgencyBreakdown: () => request('GET', 
    '/analytics/urgency')
}

export const userAPI = {
  getAll: () => request('GET', '/users'),
  getById: (id) => request('GET', 
    '/users/' + id),
  createOfficer: (data) => request('POST', 
    '/users/officer', data),
  updateRole: (id, role) => request('PUT', 
    '/users/' + id + '/role', { role })
}
