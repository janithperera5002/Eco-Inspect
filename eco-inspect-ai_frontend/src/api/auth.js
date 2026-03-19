const TOKEN_KEY = 'eco_token'
const USER_KEY  = 'eco_user'

export const auth = {

  async login(email, password) {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ email, password })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Login failed')
    }
    const data = await res.json()
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, 
      JSON.stringify(data.user || data))
    return data
  },

  logout() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    window.location.href = '/pages/auth/login.html'
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY)
  },

  getUser() {
    const u = localStorage.getItem(USER_KEY)
    return u ? JSON.parse(u) : null
  },

  isLoggedIn() {
    return !!this.getToken()
  },

  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = 
        '/pages/auth/login.html'
    }
  }
}
