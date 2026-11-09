// StatusPill — a small colored label that shows "UP" or "DOWN".
// Used on the home page to show if the backend and database are working.
export default function StatusPill({ label, isUp }) {
  return (
    <span className={`status-pill ${isUp ? 'status-pill--up' : 'status-pill--down'}`}>
      {label}: {isUp ? 'UP' : 'DOWN'}
    </span>
  )
}
