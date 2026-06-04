import axios from 'axios'

export const AUTH_TOKEN_KEY = 'careerpilot_auth_token'

const client = axios.create({
  baseURL: '/api',
  timeout: 90000
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && Object.prototype.hasOwnProperty.call(body, 'success')) {
      if (!body.success) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      response.data = body.data
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

export function parseResume(file) {
  const formData = new FormData()
  formData.append('file', file)
  return client.post('/resumes/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function buildResume(payload) {
  return client.post('/resumes/build', payload)
}

export function analyzeResume(payload) {
  return client.post('/analysis', payload)
}

export function loadRecords() {
  return client.get('/analysis/records')
}

export function loadRecordDetail(id) {
  return client.get(`/analysis/records/${id}`)
}

export function deleteRecord(id) {
  return client.delete(`/analysis/records/${id}`)
}

export function loadAiStatus() {
  return client.get('/ai/status')
}

export function generateQuestions(payload) {
  return client.post('/interview/questions', payload)
}

export function sendChatMessage(payload) {
  return client.post('/chat', payload)
}

export function loadChatSessions() {
  return client.get('/chat/sessions')
}

export function loadChatSessionDetail(id) {
  return client.get(`/chat/sessions/${id}`)
}

export function deleteChatSession(id) {
  return client.delete(`/chat/sessions/${id}`)
}

export function register(payload) {
  return client.post('/auth/register', payload)
}

export function login(payload) {
  return client.post('/auth/login', payload)
}

export function sendAuthCode(payload) {
  return client.post('/auth/send-code', payload)
}

export function resetPassword(payload) {
  return client.post('/auth/reset-password', payload)
}

export function loadCurrentUser() {
  return client.get('/auth/me')
}
