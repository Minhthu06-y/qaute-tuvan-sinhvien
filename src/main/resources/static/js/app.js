(() => {
  const root = document.getElementById('app-scripts');
  const toast = document.getElementById('live-toast');
  if (!root) return;
  if (root.dataset.role === 'CONSULTANT') {
    const heartbeat = () => fetch('/presence/heartbeat', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
    heartbeat();
    window.setInterval(heartbeat, 30000);
  }
  if (!toast || !window.StompJs || !window.SockJS) return;
  const userId = root.dataset.userId;
  const messageNode = document.getElementById('live-toast-message');
  const linkNode = document.getElementById('live-toast-link');
  let dismissTimer;
  const client = new StompJs.Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      const showEvent = frame => {
        const event = JSON.parse(frame.body);
        messageNode.textContent = event.message || 'Bạn có thông báo mới.';
        linkNode.href = `/questions/${event.questionId}`;
        toast.classList.add('visible');
        clearTimeout(dismissTimer);
        dismissTimer = setTimeout(() => toast.classList.remove('visible'), 9000);
      };
      client.subscribe(`/topic/notifications/${userId}`, showEvent);
      if (root.dataset.role === 'MANAGER' && root.dataset.facultyId) {
        client.subscribe(`/topic/faculties/${root.dataset.facultyId}`, showEvent);
      }
    }
  });
  client.activate();
})();
