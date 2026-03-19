// ── API BASE ─────────────────────────────────
const API = 'http://localhost:8080/api';

// ── AUTH HELPERS ─────────────────────────────
window.auth = {
  getUser: () => {
    const u = localStorage.getItem('eco_user');
    return u ? JSON.parse(u) : null;
  },
  getToken: () => {
    const u = window.auth.getUser();
    return u ? u.token : null;
  },
  isLoggedIn: () => !!window.auth.getToken(),
  logout: () => {
    localStorage.removeItem('eco_user');
    window.location.href = 
      '/pages/auth/login.html';
  },
  requireAuth: () => {
    if (!window.auth.isLoggedIn()) {
      window.location.href = 
        '/pages/auth/login.html';
    }
  }
};

// ── API HELPER ───────────────────────────────
window.api = async function(method, path, body) {
  const headers = {
    'Content-Type': 'application/json'
  };
  const token = window.auth.getToken();
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);
  
  try {
    const res = await fetch(API + path, options);
    if (res.status === 401) {
      window.auth.logout(); return;
    }
    return await res.json();
  } catch (err) {
    console.error('API error:', err);
    throw err;
  }
};

// ── BADGE HELPERS ────────────────────────────
window.urgencyBadge = function(level) {
  const map = {
    critical: '#FF4D6D',
    high:     '#FFB830',
    medium:   '#4DA6FF',
    low:      '#2DFF7F'
  };
  const color = map[level] || '#5A7A62';
  return '<span style="background:' + color + '22;' +
    'color:' + color + ';border:1px solid ' + 
    color + ';border-radius:20px;padding:3px 10px;' +
    'font-size:11px;font-weight:600">' + 
    (level || 'unknown').toUpperCase() + '</span>';
};

window.statusBadge = function(status) {
  const map = {
    received:       '#4DA6FF',
    ai_processing:  '#FFB830',
    pending_review: '#C084FC',
    assigned:       '#00E5CC',
    in_progress:    '#2DFF7F',
    resolved:       '#2DFF7F',
    rejected:       '#FF4D6D'
  };
  const color = map[status] || '#5A7A62';
  const label = (status || 'unknown')
    .replace(/_/g,' ').toUpperCase();
  return '<span style="background:' + color + '22;' +
    'color:' + color + ';border:1px solid ' + 
    color + ';border-radius:20px;padding:3px 10px;' +
    'font-size:11px;font-weight:600">' + 
    label + '</span>';
};

window.timeAgo = function(dateStr) {
  if (!dateStr) return '—';
  const diff = Date.now() - new Date(dateStr);
  const m = Math.floor(diff/60000);
  if (m < 1) return 'just now';
  if (m < 60) return m + 'm ago';
  const h = Math.floor(m/60);
  if (h < 24) return h + 'h ago';
  return Math.floor(h/24) + 'd ago';
};

window.truncate = function(text, max) {
  if (!text) return '';
  return text.length > max 
    ? text.substring(0, max) + '...' : text;
};

window.showToast = function(msg, type) {
  const colors = {
    success: '#2DFF7F', 
    error: '#FF4D6D', 
    info: '#4DA6FF'
  };
  const c = colors[type] || colors.success;
  const t = document.createElement('div');
  t.style.cssText = 
    'position:fixed;bottom:24px;right:24px;' +
    'background:#111A14;color:#D4EAD9;' +
    'border:1px solid ' + c + ';' +
    'border-left:4px solid ' + c + ';' +
    'border-radius:10px;padding:14px 20px;' +
    'font-size:14px;z-index:9999;' +
    'box-shadow:0 4px 20px rgba(0,0,0,0.5);' +
    'max-width:300px;';
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
};

// ── DATE FORMATTER ───────────────────────────
window.formatDate = function(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString(
    'en-GB', {
      day: 'numeric', month: 'short',
      year: 'numeric', hour: '2-digit',
      minute: '2-digit'
    }
  );
};

// ── GREETING ─────────────────────────────────
window.getGreeting = function() {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
};

// ── SET OFFICER INFO ─────────────────────────
window.setOfficerInfo = function() {
  const user = window.auth.getUser();
  if (!user) return;
  const name = user.fullName || user.full_name || user.email || 'Officer';
  const role = user.role || 'officer';

  ['#officer-name', '.officer-name', '.user-name',
   '.nav-officer-name', '.sidebar-name'].forEach(sel => {
    const el = document.querySelector(sel);
    if (el) el.textContent = name;
  });

  ['#officer-role', '.officer-role', '.user-role',
   '.sidebar-role'].forEach(sel => {
    const el = document.querySelector(sel);
    if (el) el.textContent =
      role.charAt(0).toUpperCase() + role.slice(1);
  });

  ['.greeting', '.welcome-text', '#greeting'].forEach(sel => {
    const el = document.querySelector(sel);
    if (el && el.textContent.includes('Officer')) {
      el.textContent = window.getGreeting() + ', ' + name + ' 👋';
    }
  });
};

// ── SKELETON LOADER ──────────────────────────
window.showSkeleton = function(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.innerHTML =
    '<div style="padding:20px;color:#5A7A62;text-align:center">Loading...</div>';
};

window.highlightNav = function(pageName) {
    // pageName is one of: 'dashboard', 
    // 'reports', 'cases', 'analytics', 
    // 'map', 'users', 'profile'
    
    const navLinks = document.querySelectorAll(
        'nav a, .sidebar a, .nav-link, ' +
        '.sidebar-nav a');
    
    navLinks.forEach(link => {
        const href = link.getAttribute('href') || '';
        const text = link.textContent || '';
        const matches = href.toLowerCase().includes(pageName.toLowerCase()) || 
                        text.toLowerCase().includes(pageName.toLowerCase());
        
        if (matches && pageName !== '#') {
            // Add active style
            link.style.color = '#2DFF7F';
            link.style.borderLeft = '3px solid #2DFF7F';
            link.style.paddingLeft = '12px';
            link.style.background = 'rgba(45,255,127,0.08)';
        }
    });
};

console.log('Eco-Inspect shared.js loaded');
