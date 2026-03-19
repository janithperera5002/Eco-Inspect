import { URGENCY, STATUS, CATEGORIES } 
  from './constants.js'

export function timeAgo(dateString) {
  const date = new Date(dateString)
  const now = new Date()
  const seconds = Math.floor((now - date) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return minutes + 'm ago'
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return hours + 'h ago'
  const days = Math.floor(hours / 24)
  if (days < 7) return days + 'd ago'
  return date.toLocaleDateString('en-GB', 
    { day:'numeric', month:'short', year:'numeric' })
}

export function formatDate(dateString) {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString(
    'en-GB', { 
      day: 'numeric', month: 'long', 
      year: 'numeric', hour: '2-digit', 
      minute: '2-digit' 
    }
  )
}

export function truncate(text, max) {
  if (!text) return ''
  return text.length > max 
    ? text.substring(0, max) + '...' 
    : text
}

export function urgencyBadge(level) {
  const u = URGENCY[level] || URGENCY.medium
  return '<span class="badge ' + u.badgeClass 
         + '">' + u.label + '</span>'
}

export function statusBadge(status) {
  const s = STATUS[status] || STATUS.received
  return '<span class="badge ' + s.badgeClass 
         + '">' + s.label + '</span>'
}

export function categoryLabel(key) {
  return CATEGORIES[key] || key || 'Unknown'
}

export function isOverdue(dueDateString) {
  if (!dueDateString) return false
  return new Date(dueDateString) < new Date()
}

export function showToast(message, type = 'success') {
  const existing = document.getElementById(
    'eco-toast'
  )
  if (existing) existing.remove()
  const colors = { 
    success: '#2DFF7F', 
    error: '#FF4D6D', 
    info: '#4DA6FF' 
  }
  const toast = document.createElement('div')
  toast.id = 'eco-toast'
  toast.style.cssText = `
    position:fixed; bottom:24px; right:24px;
    background:#111A14; color:#D4EAD9;
    border:1px solid ${colors[type]};
    border-left:4px solid ${colors[type]};
    border-radius:10px; padding:14px 20px;
    font-family:sans-serif; font-size:14px;
    z-index:9999; max-width:320px;
    box-shadow:0 4px 20px rgba(0,0,0,0.4);
    animation: slideIn 0.3s ease;
  `
  toast.textContent = message
  document.body.appendChild(toast)
  setTimeout(() => toast.remove(), 3500)
}

export function showModal(message, onConfirm) {
  const overlay = document.createElement('div')
  overlay.style.cssText = `
    position:fixed; inset:0;
    background:rgba(0,0,0,0.7);
    display:flex; align-items:center;
    justify-content:center; z-index:9998;
  `
  overlay.innerHTML = '<div style="background:#111A14;' 
    + 'border:1px solid #1E3325;border-radius:12px;'
    + 'padding:28px;max-width:400px;width:90%;'
    + 'text-align:center;">'
    + '<p style="color:#D4EAD9;margin-bottom:20px;'
    + 'font-size:15px;">' + message + '</p>'
    + '<button id="modal-cancel" style="background:' 
    + 'transparent;border:1px solid #1E3325;'
    + 'color:#5A7A62;border-radius:8px;'
    + 'padding:10px 20px;cursor:pointer;'
    + 'margin-right:10px;">Cancel</button>'
    + '<button id="modal-confirm" style="background:'
    + '#FF4D6D;border:none;color:white;'
    + 'border-radius:8px;padding:10px 20px;'
    + 'cursor:pointer;">Confirm</button></div>'
  document.body.appendChild(overlay)
  document.getElementById('modal-cancel')
    .onclick = () => overlay.remove()
  document.getElementById('modal-confirm')
    .onclick = () => { overlay.remove(); onConfirm() }
}
