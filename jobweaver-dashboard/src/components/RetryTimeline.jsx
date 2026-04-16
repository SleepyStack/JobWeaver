function RetryTimeline() {
const retries = [
{ id: "c3d4", text: "retry 2/3", error: "Simulation error" },
{ id: "d4e5", text: "retry 1/2", error: "Timeout" },
{ id: "a7b8", text: "DLQ", error: "Max retries reached" },
];

return (
<> <div className="section-header">Retry Activity</div>

```
  {retries.map((r, i) => (
    <div key={i} className="retry-event">
      <div className="retry-body">
        <div className="retry-title">{r.id} — {r.text}</div>
        <div className="retry-meta">{r.error}</div>
      </div>
    </div>
  ))}
</>


);
}

export default RetryTimeline;
