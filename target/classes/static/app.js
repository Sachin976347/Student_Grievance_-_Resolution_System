/* ═══════════════════════════════════════════════════════════════
   Student Grievance System — Main Application Script
   ═══════════════════════════════════════════════════════════════ */

// ── API base paths ─────────────────────────────────────────────
const API  = '/api/grievances';
const AUTH = '/api/auth';
const REP  = '/api/reports';
const FILE = '/api/files';

// ── App state ──────────────────────────────────────────────────
let token      = localStorage.getItem('token')    || null;
let role       = localStorage.getItem('role')     || null;
let username   = localStorage.getItem('username') || null;
let fullName   = localStorage.getItem('fullName') || null;
let allData    = [];
let filterStatus = 'all';
let modalGid   = null;
let monthChart = null;
let resChart   = null;

const COLORS = ['#1D9E75','#378ADD','#EF9F27','#E24B4A','#B4B2A9','#7F77DD','#D85A30'];

// ═══════════════════════════════════════════════════════════════
// INIT
// ═══════════════════════════════════════════════════════════════
window.addEventListener('DOMContentLoaded', () => {
  if (token) bootApp();
  else       showLogin();
});

// ═══════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════
function showLogin() {
  document.getElementById('loginCard').style.display    = 'block';
  document.getElementById('registerCard').style.display = 'none';
  clearAuthErr();
}

function showRegister() {
  document.getElementById('loginCard').style.display    = 'none';
  document.getElementById('registerCard').style.display = 'block';
  clearAuthErr();
}

function clearAuthErr() {
  document.getElementById('loginErr').style.display = 'none';
  document.getElementById('regErr').style.display   = 'none';
}

function showAuthErr(el, msg) {
  el.textContent    = msg;
  el.style.display  = 'block';
}

async function doLogin() {
  const u   = document.getElementById('l-user').value.trim();
  const p   = document.getElementById('l-pass').value.trim();
  const err = document.getElementById('loginErr');

  if (!u || !p) { showAuthErr(err, 'Enter username and password.'); return; }

  try {
    const res  = await fetch(AUTH + '/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p })
    });
    const data = await res.json();
    if (!res.ok) { showAuthErr(err, data.error || 'Login failed.'); return; }
    saveSession(data);
    bootApp();
  } catch (e) {
    showAuthErr(err, 'Server error. Is the backend running?');
  }
}

async function doRegister() {
  const err  = document.getElementById('regErr');
  const body = {
    fullName: document.getElementById('r-name').value.trim(),
    username: document.getElementById('r-user').value.trim(),
    email:    document.getElementById('r-email').value.trim(),
    password: document.getElementById('r-pass').value.trim()
  };

  if (!body.username || !body.email || !body.password) {
    showAuthErr(err, 'Username, email and password are required.'); return;
  }
  if (body.password.length < 6) {
    showAuthErr(err, 'Password must be at least 6 characters.'); return;
  }

  try {
    const res  = await fetch(AUTH + '/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) { showAuthErr(err, data.error || 'Registration failed.'); return; }
    showToast('Account created! Please log in.');
    showLogin();
    document.getElementById('l-user').value = body.username;
  } catch (e) {
    showAuthErr(err, 'Server error.');
  }
}

function saveSession(d) {
  token    = d.token;
  role     = d.role;
  username = d.username;
  fullName = d.fullName || d.username;
  localStorage.setItem('token',    token);
  localStorage.setItem('role',     role);
  localStorage.setItem('username', username);
  localStorage.setItem('fullName', fullName);
}

function doLogout() {
  ['token', 'role', 'username', 'fullName'].forEach(k => localStorage.removeItem(k));
  token = role = username = fullName = null;
  document.getElementById('appShell').style.display  = 'none';
  document.getElementById('authShell').style.display = 'flex';
  showLogin();
}

// ═══════════════════════════════════════════════════════════════
// BOOT APP AFTER LOGIN
// ═══════════════════════════════════════════════════════════════
function bootApp() {
  document.getElementById('authShell').style.display = 'none';
  document.getElementById('appShell').style.display  = 'flex';

  const isAdmin  = role === 'ROLE_ADMIN';
  const initials = (fullName || username || '?')
    .split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();

  document.getElementById('sideAvatar').textContent  = initials;
  document.getElementById('sideName').textContent    = fullName || username;
  document.getElementById('sideRole').textContent    = isAdmin ? 'Administrator' : 'Student';
  document.getElementById('topAvatar').textContent   = initials;
  document.getElementById('topRoleBadge').className  = 'role-badge ' + (isAdmin ? 'role-admin' : 'role-student');
  document.getElementById('topRoleBadge').textContent = isAdmin ? 'Admin' : 'Student';

  document.getElementById('adminNav').style.display   = isAdmin ? 'block' : 'none';
  document.getElementById('studentNav').style.display = isAdmin ? 'none'  : 'block';

  if (isAdmin) navigate('dashboard', document.querySelector('#adminNav .nav-item'));
  else         navigate('myDash',    document.querySelector('#studentNav .nav-item'));
}

// ═══════════════════════════════════════════════════════════════
// NAVIGATION
// ═══════════════════════════════════════════════════════════════
function navigate(view, el) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById('view-' + view).classList.add('active');
  if (el) el.classList.add('active');

  const titles = {
    dashboard : 'Dashboard',
    all       : 'All Grievances',
    pending   : 'Pending',
    overdue   : 'Overdue Grievances',
    resolved  : 'Resolved',
    reports   : 'Analytics & Reports',
    submit    : 'Submit Grievance',
    myDash    : 'My Dashboard',
    mine      : 'My Grievances'
  };
  document.getElementById('pageTitle').textContent = titles[view] || view;

  // Show / hide submit form based on role — admin sees blocked banner, student sees form
  if (view === 'submit') {
    const isAdmin = role === 'ROLE_ADMIN';
    document.getElementById('submit-admin-block').style.display = isAdmin ? 'flex' : 'none';
    document.getElementById('submit-form-card').style.display   = isAdmin ? 'none' : 'block';
  }

  const loaders = {
    dashboard : loadAdminDash,
    all       : loadAll,
    pending   : loadPending,
    overdue   : loadOverdue,
    resolved  : loadResolved,
    reports   : loadReports,
    myDash    : loadStudentDash,
    mine      : loadMine
  };
  loaders[view]?.();
}

// ═══════════════════════════════════════════════════════════════
// API HELPER
// ═══════════════════════════════════════════════════════════════
async function apiFetch(url, opts = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
    ...opts
  });
  if (res.status === 401) { showToast('Session expired.', true); doLogout(); return null; }
  if (res.status === 403) { showToast('Access denied.',   true); return null; }
  if (res.status === 204) return null;
  return res.ok ? res.json() : Promise.reject(await res.json());
}

// ═══════════════════════════════════════════════════════════════
// UTILITY HELPERS
// ═══════════════════════════════════════════════════════════════
function fmt(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric'
  });
}

function fmtDue(dt, overdue) {
  if (!dt) return '—';
  const d = fmt(dt);
  return overdue ? `<span class="chip chip-red">Overdue</span> ${d}` : d;
}

function statusPill(s) {
  const c = { Open: 's-open', 'In Progress': 's-progress', Resolved: 's-resolved', Closed: 's-closed' };
  return `<span class="pill ${c[s] || 's-closed'}"><span class="pill-dot"></span>${s}</span>`;
}

function priClass(p) {
  return p === 'High' ? 'p-high' : p === 'Medium' ? 'p-med' : 'p-low';
}

function gid(id) {
  return '#GR-' + String(id).padStart(4, '0');
}

function showToast(msg, err = false) {
  const t = document.getElementById('toast');
  t.textContent       = msg;
  t.style.background  = err ? '#E24B4A' : '#1a1a18';
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 3000);
}

function drawDonut(byCat, svgId, legId) {
  const cats  = Object.entries(byCat);
  const total = cats.reduce((s, [, v]) => s + v, 0);
  const svg   = document.getElementById(svgId);
  const cx = 65, cy = 65, r = 46, sw = 22, circ = 2 * Math.PI * r;
  let offset = 0, circles = '';

  cats.forEach(([, count], i) => {
    const d = total > 0 ? count / total * circ : 0;
    circles += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none"
      stroke="${COLORS[i % COLORS.length]}" stroke-width="${sw}"
      stroke-dasharray="${d} ${circ - d}"
      stroke-dashoffset="${-offset}"
      transform="rotate(-90 ${cx} ${cy})"/>`;
    offset += d;
  });

  if (total === 0) {
    circles = `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#e0e0dc" stroke-width="${sw}"/>`;
  }

  svg.innerHTML = circles
    + `<circle cx="${cx}" cy="${cy}" r="${r - sw / 2}" fill="white"/>`
    + `<text x="${cx}" y="${cy - 4}" text-anchor="middle" font-size="20" font-weight="600"
         font-family="Syne,sans-serif" fill="#1a1a18">${total}</text>`
    + `<text x="${cx}" y="${cy + 12}" text-anchor="middle" font-size="10"
         fill="#888780" font-family="DM Sans,sans-serif">total</text>`;

  document.getElementById(legId).innerHTML = cats.length
    ? cats.map(([cat, count], i) =>
        `<div class="l-item">
           <div class="l-left">
             <div class="l-dot" style="background:${COLORS[i % COLORS.length]}"></div>${cat}
           </div>
           <div class="l-count">${count}</div>
         </div>`).join('')
    : '<div style="color:var(--text-sec);font-size:12px">No data yet</div>';
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — DASHBOARD
// ═══════════════════════════════════════════════════════════════
async function loadAdminDash() {
  try {
    const [sum, recent] = await Promise.all([apiFetch(API + '/dashboard'), apiFetch(API)]);
    if (!sum || !recent) return;

    document.getElementById('a-total').textContent   = sum.total;
    document.getElementById('a-pending').textContent = (+sum.open + +sum.inProgress);
    document.getElementById('a-resolved').textContent = sum.resolved;
    document.getElementById('a-overdue').textContent  = sum.overdue || 0;
    document.getElementById('a-high').textContent     = sum.highPriority;

    const rate = sum.total > 0 ? Math.round(sum.resolved / sum.total * 100) : 0;
    document.getElementById('a-rate').textContent = rate + '% rate';

    const rows = recent.slice(-6).reverse();
    document.getElementById('dash-tbody').innerHTML = rows.length
      ? rows.map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.studentName}</td>
          <td>${g.category}</td>
          <td>${statusPill(g.status)}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px">${fmtDue(g.dueDate, g.overdue)}</td>
          <td><button class="btn btn-ghost btn-sm" onclick="openModal(${g.id}, true)">Edit</button></td>
        </tr>`).join('')
      : '<tr><td colspan="7" class="empty">No grievances yet.</td></tr>';

    drawDonut(sum.byCategory || {}, 'donutA', 'cat-legend-a');
  } catch (e) {
    showToast('Dashboard error', true);
  }
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — ALL GRIEVANCES
// ═══════════════════════════════════════════════════════════════
async function loadAll() {
  try {
    allData = await apiFetch(API);
    if (allData) renderAll();
  } catch (e) { showToast('Error loading grievances', true); }
}

function renderAll() {
  const data = filterStatus === 'all'
    ? allData
    : allData.filter(g => g.status === filterStatus);

  document.getElementById('all-tbody').innerHTML = data.length
    ? data.map(g => `<tr>
        <td class="mono">${gid(g.id)}</td>
        <td>${g.studentName}</td>
        <td>${g.category}</td>
        <td>${statusPill(g.status)}${g.overdue ? '<span class="chip chip-red" style="margin-left:4px">Overdue</span>' : ''}</td>
        <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
        <td style="font-size:12px">${fmtDue(g.dueDate, g.overdue)}</td>
        <td style="font-size:12px">${g.fileName
          ? `<a onclick="downloadFile(${g.id})" style="color:var(--info);cursor:pointer;text-decoration:underline">${g.fileName}</a>`
          : '—'}</td>
        <td style="font-size:12px;color:var(--text-sec)">${fmt(g.submittedAt)}</td>
        <td style="display:flex;gap:6px">
          <button class="btn btn-ghost btn-sm" onclick="openModal(${g.id}, true)">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="deleteGrievance(${g.id})">Del</button>
        </td>
      </tr>`).join('')
    : '<tr><td colspan="9" class="empty">No grievances found.</td></tr>';
}

function filterAll(s, btn) {
  filterStatus = s;
  document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  renderAll();
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — PENDING
// ═══════════════════════════════════════════════════════════════
async function loadPending() {
  try {
    const data = await apiFetch(API + '/pending');
    if (!data) return;
    document.getElementById('pending-tbody').innerHTML = data.length
      ? data.map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.studentName}</td>
          <td>${g.category}</td>
          <td>${statusPill(g.status)}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px">${fmtDue(g.dueDate, g.overdue)}</td>
          <td><button class="btn btn-primary btn-sm" onclick="openModal(${g.id}, true)">Update</button></td>
        </tr>`).join('')
      : '<tr><td colspan="7" class="empty">No pending grievances!</td></tr>';
  } catch (e) { showToast('Error', true); }
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — OVERDUE
// ═══════════════════════════════════════════════════════════════
async function loadOverdue() {
  try {
    const data = await apiFetch(API + '/overdue');
    if (!data) return;
    document.getElementById('overdue-tbody').innerHTML = data.length
      ? data.map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.studentName}</td>
          <td>${g.category}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px;color:var(--dan)">${fmt(g.dueDate)}</td>
          <td style="font-size:12px">${g.assignedTo || '—'}</td>
          <td><button class="btn btn-primary btn-sm" onclick="openModal(${g.id}, true)">Resolve</button></td>
        </tr>`).join('')
      : '<tr><td colspan="7" class="empty">No overdue grievances — all within SLA!</td></tr>';
  } catch (e) { showToast('Error', true); }
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — RESOLVED
// ═══════════════════════════════════════════════════════════════
async function loadResolved() {
  try {
    const data = await apiFetch(API + '/status/Resolved');
    if (!data) return;
    document.getElementById('resolved-tbody').innerHTML = data.length
      ? data.map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.studentName}</td>
          <td>${g.category}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px;color:var(--text-sec)">${fmt(g.submittedAt)}</td>
          <td style="font-size:12px;color:var(--acc)">${fmt(g.resolvedAt)}</td>
          <td style="font-size:12px;color:var(--text-sec)">${g.remarks || '—'}</td>
        </tr>`).join('')
      : '<tr><td colspan="7" class="empty">No resolved grievances yet.</td></tr>';
  } catch (e) { showToast('Error', true); }
}

// ═══════════════════════════════════════════════════════════════
// ADMIN — REPORTS / ANALYTICS
// ═══════════════════════════════════════════════════════════════
async function loadReports() {
  try {
    const [sum, monthly, resTime, authority, overdue] = await Promise.all([
      apiFetch(REP + '/summary'),
      apiFetch(REP + '/monthly'),
      apiFetch(REP + '/resolution-time'),
      apiFetch(REP + '/authority'),
      apiFetch(REP + '/overdue')
    ]);
    if (!sum) return;

    document.getElementById('r-total').textContent   = sum.total;
    document.getElementById('r-rate').textContent    = (sum.resolutionRate || 0) + '%';
    document.getElementById('r-overdue').textContent = sum.overdue || 0;
    document.getElementById('r-high').textContent    = sum.highPriority || 0;

    // Monthly submissions bar chart
    if (monthChart) monthChart.destroy();
    const mCtx = document.getElementById('monthlyChart').getContext('2d');
    monthChart = new Chart(mCtx, {
      type: 'bar',
      data: {
        labels:   (monthly || []).map(r => r.month + ' ' + r.year),
        datasets: [{ label: 'Submissions', data: (monthly || []).map(r => r.count), backgroundColor: '#1D9E75', borderRadius: 4 }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } } }
    });

    // Avg resolution time bar chart
    if (resChart) resChart.destroy();
    const rCtx = document.getElementById('resolutionChart').getContext('2d');
    resChart = new Chart(rCtx, {
      type: 'bar',
      data: {
        labels:   (resTime || []).map(r => r.category),
        datasets: [{ label: 'Avg days', data: (resTime || []).map(r => r.avgDays), backgroundColor: '#378ADD', borderRadius: 4 }]
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
    });

    // Authority performance table
    document.getElementById('authority-tbody').innerHTML = (authority || []).length
      ? (authority || []).map(a => `<tr>
          <td>${a.authority}</td>
          <td>${a.total}</td>
          <td>${a.resolved}</td>
          <td><span class="chip ${a.resolutionRate >= 70 ? 'chip-green' : a.resolutionRate >= 40 ? 'chip-yellow' : 'chip-red'}">${a.resolutionRate}%</span></td>
        </tr>`).join('')
      : '<tr><td colspan="4" class="empty">No authority data yet.</td></tr>';

    // Overdue table
    document.getElementById('rep-overdue-tbody').innerHTML = (overdue || []).length
      ? (overdue || []).map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.studentName}</td>
          <td>${g.category}</td>
          <td style="color:var(--dan);font-size:12px">${fmt(g.dueDate)}</td>
        </tr>`).join('')
      : '<tr><td colspan="4" class="empty">No overdue grievances.</td></tr>';

  } catch (e) { showToast('Reports error', true); console.error(e); }
}

// ═══════════════════════════════════════════════════════════════
// STUDENT — MY DASHBOARD
// ═══════════════════════════════════════════════════════════════
async function loadStudentDash() {
  try {
    const [sum, mine] = await Promise.all([
      apiFetch(API + '/mine/dashboard'),
      apiFetch(API + '/mine')
    ]);
    if (!sum || !mine) return;

    document.getElementById('s-total').textContent = sum.total;
    document.getElementById('s-open').textContent  = sum.open;
    document.getElementById('s-prog').textContent  = sum.inProgress;
    document.getElementById('s-res').textContent   = sum.resolved;

    const rows = mine.slice(-5).reverse();
    document.getElementById('my-dash-tbody').innerHTML = rows.length
      ? rows.map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.category}</td>
          <td>${statusPill(g.status)}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px">${fmtDue(g.dueDate, g.overdue)}</td>
          <td style="font-size:12px;color:var(--text-sec)">${fmt(g.submittedAt)}</td>
        </tr>`).join('')
      : '<tr><td colspan="6" class="empty">No grievances yet.</td></tr>';

    drawDonut(sum.byCategory || {}, 'donutS', 'cat-legend-s');
  } catch (e) { showToast('Dashboard error', true); }
}

// ═══════════════════════════════════════════════════════════════
// STUDENT — MY GRIEVANCES
// ═══════════════════════════════════════════════════════════════
async function loadMine() {
  try {
    const data = await apiFetch(API + '/mine');
    if (!data) return;
    document.getElementById('mine-tbody').innerHTML = data.length
      ? data.slice().reverse().map(g => `<tr>
          <td class="mono">${gid(g.id)}</td>
          <td>${g.category}</td>
          <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${g.description}</td>
          <td>${statusPill(g.status)}${g.overdue ? '<span class="chip chip-red" style="margin-left:4px;font-size:10px">Overdue</span>' : ''}</td>
          <td><span class="${priClass(g.priority)}">${g.priority}</span></td>
          <td style="font-size:12px">${fmtDue(g.dueDate, g.overdue)}</td>
          <td style="font-size:12px">${g.fileName
            ? `<a onclick="downloadFile(${g.id})" style="color:var(--info);cursor:pointer;text-decoration:underline">${g.fileName}</a>`
            : '—'}</td>
          <td style="font-size:12px;color:var(--text-sec)">${g.remarks || '—'}</td>
        </tr>`).join('')
      : '<tr><td colspan="8" class="empty">You have not submitted any grievances yet.</td></tr>';
  } catch (e) { showToast('Error', true); }
}

// ═══════════════════════════════════════════════════════════════
// SUBMIT GRIEVANCE
// ═══════════════════════════════════════════════════════════════
async function submitGrievance() {
  // Hard block — admin cannot submit grievances under any circumstance
  if (role === 'ROLE_ADMIN') {
    showToast('Administrators cannot submit grievances.', true);
    return;
  }

  const name = document.getElementById('f-name').value.trim();
  const sid  = document.getElementById('f-sid').value.trim();
  const desc = document.getElementById('f-desc').value.trim();
  const cat  = document.getElementById('f-cat').value;
  const pri  = document.getElementById('f-pri').value;

  if (!name || !sid || !desc || !cat) {
    showToast('Please fill all required fields.', true);
    return;
  }

  document.getElementById('submit-loading').style.display = 'inline';

  try {
    const saved = await apiFetch(API, {
      method: 'POST',
      body: JSON.stringify({
        studentName: name,
        studentId:   sid,
        email:       document.getElementById('f-email').value.trim(),
        description: desc,
        category:    cat,
        priority:    pri
      })
    });
    if (!saved) return;

    // Upload attachment if provided
    const fileInput = document.getElementById('f-file');
    if (fileInput.files.length > 0) {
      const fd = new FormData();
      fd.append('file', fileInput.files[0]);
      await fetch(`${FILE}/upload/${saved.id}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        body: fd
      });
    }

    showToast('Grievance submitted! Due: ' + fmt(saved.dueDate));
    clearForm();
    navigate('myDash', document.querySelector('#studentNav .nav-item'));

  } catch (e) {
    showToast('Submit failed.', true);
  } finally {
    document.getElementById('submit-loading').style.display = 'none';
  }
}

function clearForm() {
  ['f-name', 'f-sid', 'f-email', 'f-desc'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('f-cat').value  = '';
  document.getElementById('f-pri').value  = 'Low';
  document.getElementById('f-file').value = '';
}

// ═══════════════════════════════════════════════════════════════
// FILE DOWNLOAD
// ═══════════════════════════════════════════════════════════════
async function downloadFile(grievanceId) {
  const res = await fetch(`${FILE}/download/${grievanceId}`, {
    headers: { 'Authorization': 'Bearer ' + token }
  });
  if (!res.ok) { showToast('File not found.', true); return; }

  const blob        = await res.blob();
  const disposition = res.headers.get('Content-Disposition') || '';
  const match       = disposition.match(/filename="(.+)"/);
  const fname       = match ? match[1] : 'attachment';

  const a   = document.createElement('a');
  a.href    = URL.createObjectURL(blob);
  a.download = fname;
  a.click();
}

// ═══════════════════════════════════════════════════════════════
// MODAL — VIEW / EDIT GRIEVANCE DETAILS
// ═══════════════════════════════════════════════════════════════
async function openModal(id, isAdmin = false) {
  try {
    const url = role === 'ROLE_ADMIN' ? `${API}/${id}` : `${API}/mine/${id}`;
    const g   = await apiFetch(url);
    if (!g) return;
    modalGid = id;

    document.getElementById('modal-body').innerHTML = `
      <div class="detail-row"><span class="detail-label">ID</span>${gid(g.id)}</div>
      <div class="detail-row"><span class="detail-label">Student</span><strong>${g.studentName}</strong></div>
      <div class="detail-row"><span class="detail-label">Student ID</span>${g.studentId}</div>
      <div class="detail-row"><span class="detail-label">Email</span>${g.email || '—'}</div>
      <div class="detail-row"><span class="detail-label">Category</span>${g.category}</div>
      <div class="detail-row"><span class="detail-label">Priority</span><span class="${priClass(g.priority)}">${g.priority}</span></div>
      <div class="detail-row"><span class="detail-label">Status</span>${statusPill(g.status)}${g.overdue ? '<span class="chip chip-red" style="margin-left:6px">SLA Overdue</span>' : ''}</div>
      <div class="detail-row"><span class="detail-label">Submitted</span>${fmt(g.submittedAt)}</div>
      <div class="detail-row"><span class="detail-label">Due Date</span>${fmt(g.dueDate)}</div>
      <div class="detail-row"><span class="detail-label">Description</span><span style="font-size:12.5px;line-height:1.55">${g.description}</span></div>
      ${g.fileName ? `<div class="detail-row"><span class="detail-label">Attachment</span>
        <a onclick="downloadFile(${g.id})" style="color:var(--info);cursor:pointer;text-decoration:underline;font-size:13px">${g.fileName}</a>
      </div>` : ''}
      ${g.remarks ? `<div class="detail-row"><span class="detail-label">Remarks</span><span style="font-size:12.5px">${g.remarks}</span></div>` : ''}
    `;

    const actionsEl = document.getElementById('modal-actions');
    if (role === 'ROLE_ADMIN') {
      actionsEl.innerHTML = `
        <select id="modal-status" style="font-size:13px;padding:6px 10px">
          <option value="Open"        ${g.status === 'Open'        ? 'selected' : ''}>Open</option>
          <option value="In Progress" ${g.status === 'In Progress' ? 'selected' : ''}>In Progress</option>
          <option value="Resolved"    ${g.status === 'Resolved'    ? 'selected' : ''}>Resolved</option>
          <option value="Closed"      ${g.status === 'Closed'      ? 'selected' : ''}>Closed</option>
        </select>
        <input id="modal-assigned" placeholder="Assign to…"  value="${g.assignedTo || ''}" style="font-size:13px;padding:6px 10px;width:140px"/>
        <input id="modal-remarks"  placeholder="Add remark…" value="${g.remarks    || ''}" style="font-size:13px;padding:6px 10px;width:150px"/>
        <button class="btn btn-primary btn-sm" onclick="saveModalUpdate()">Save</button>
        <button class="btn btn-ghost btn-sm"   onclick="closeModal()">Close</button>`;
    } else {
      actionsEl.innerHTML = `<button class="btn btn-ghost btn-sm" onclick="closeModal()">Close</button>`;
    }
    document.getElementById('modal').classList.add('open');
  } catch (e) { showToast('Error loading details', true); }
}

async function saveModalUpdate() {
  try {
    await apiFetch(API + '/' + modalGid, {
      method: 'PUT',
      body: JSON.stringify({
        status:     document.getElementById('modal-status').value,
        assignedTo: document.getElementById('modal-assigned').value,
        remarks:    document.getElementById('modal-remarks').value
      })
    });
    showToast('Updated successfully!');
    closeModal();
    loadAdminDash();
    if (document.getElementById('view-all').classList.contains('active'))      loadAll();
    if (document.getElementById('view-pending').classList.contains('active'))  loadPending();
    if (document.getElementById('view-overdue').classList.contains('active'))  loadOverdue();
    if (document.getElementById('view-resolved').classList.contains('active')) loadResolved();
  } catch (e) { showToast('Update failed', true); }
}

function closeModal() {
  document.getElementById('modal').classList.remove('open');
  modalGid = null;
}

document.getElementById('modal').addEventListener('click', e => {
  if (e.target === e.currentTarget) closeModal();
});

// ═══════════════════════════════════════════════════════════════
// DELETE GRIEVANCE  (admin only)
// ═══════════════════════════════════════════════════════════════
async function deleteGrievance(id) {
  if (!confirm('Delete ' + gid(id) + '? This cannot be undone.')) return;
  try {
    await apiFetch(API + '/' + id, { method: 'DELETE' });
    showToast('Grievance deleted.');
    loadAll();
  } catch (e) { showToast('Delete failed.', true); }
}