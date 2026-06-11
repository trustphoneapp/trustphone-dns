document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
  anchor.addEventListener('click', (event) => {
    const target = document.querySelector(anchor.getAttribute('href'));
    if (!target) return;
    event.preventDefault();
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

const hudFill = document.querySelector('.hud-fill');
if (hudFill) {
  let width = 72;
  setInterval(() => {
    width = 68 + Math.random() * 24;
    hudFill.style.width = `${width}%`;
  }, 2200);
}
