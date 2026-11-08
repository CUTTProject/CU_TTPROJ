// The docs are served by the API itself, so every example can use this page's own origin.
document.querySelectorAll('.base-url').forEach(el => { el.textContent = location.origin; });

// Highlight the current page in the sidebar.
const page = location.pathname.split('/').pop() || 'index.html';
document.querySelectorAll('.sidebar nav a').forEach(a => {
  if (a.getAttribute('href') === page) a.classList.add('active');
});

// Mobile menu.
const toggle = document.querySelector('.menu-toggle');
if (toggle) {
  toggle.addEventListener('click', () => {
    const open = document.querySelector('.sidebar').classList.toggle('open');
    toggle.setAttribute('aria-expanded', open);
  });
}

// CSV / JSON tabs. Choosing a format switches every tab group on the page and is remembered
// across pages, so a rep who works in JSON never has to click past CSV again.
const FORMAT_KEY = 'docs-format';

function showFormat(format) {
  document.querySelectorAll('.tabs').forEach(group => {
    const hasFormat = group.querySelector(`.tab-panel[data-panel="${format}"]`);
    if (!hasFormat) return;
    group.querySelectorAll('.tab-list button').forEach(b => {
      b.setAttribute('aria-selected', b.dataset.tab === format);
      b.tabIndex = b.dataset.tab === format ? 0 : -1;
    });
    group.querySelectorAll('.tab-panel').forEach(p => { p.hidden = p.dataset.panel !== format; });
  });
}

document.querySelectorAll('.tabs').forEach(group => {
  const buttons = [...group.querySelectorAll('.tab-list button')];
  buttons.forEach((button, i) => {
    button.type = 'button';
    button.setAttribute('role', 'tab');
    button.addEventListener('click', () => {
      showFormat(button.dataset.tab);
      try { localStorage.setItem(FORMAT_KEY, button.dataset.tab); } catch { /* storage unavailable */ }
    });
    button.addEventListener('keydown', e => {
      if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
      const next = buttons[(i + (e.key === 'ArrowRight' ? 1 : buttons.length - 1)) % buttons.length];
      next.click();
      next.focus();
    });
  });
});

let savedFormat = null;
try { savedFormat = localStorage.getItem(FORMAT_KEY); } catch { /* storage unavailable */ }
showFormat(savedFormat === 'json' ? 'json' : 'csv');

// Copy buttons on code blocks.
document.querySelectorAll('pre').forEach(pre => {
  const button = document.createElement('button');
  button.className = 'copy';
  button.type = 'button';
  button.textContent = 'Copy';
  button.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(pre.querySelector('code').innerText);
      button.textContent = 'Copied';
    } catch {
      button.textContent = 'Press Ctrl+C';
    }
    setTimeout(() => { button.textContent = 'Copy'; }, 1500);
  });
  pre.appendChild(button);
});
