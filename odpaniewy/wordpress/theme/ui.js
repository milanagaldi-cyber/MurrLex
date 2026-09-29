document.addEventListener('click', event => {
  const dialog = document.getElementById('video-dialog');
  if (!dialog) return;
  if (event.target.closest('[data-video]')) dialog.showModal();
  if (event.target.closest('.close-video') || event.target === dialog) dialog.close();
});
document.getElementById('video-dialog')?.addEventListener('close', event => event.target.querySelector('video').pause());
