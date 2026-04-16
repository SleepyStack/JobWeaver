function WorkerPool() {
return (
<> <div className="section-header">Worker Pool</div>

```
  <div style={{ padding: "12px" }}>
    <div style={{ display: "flex", justifyContent: "space-between" }}>
      <span>Threads (12)</span>
      <span>7 active</span>
    </div>

    <div style={{ display: "grid", gridTemplateColumns: "repeat(12,1fr)", gap: "3px", marginTop: "8px" }}>
      {Array.from({ length: 12 }).map((_, i) => (
        <div
          key={i}
          style={{
            height: "18px",
            background: i < 7 ? "var(--accent)" : "var(--bg3)",
          }}
        />
      ))}
    </div>

    <div style={{ marginTop: "8px" }}>
      Queue: 5 / 100
    </div>
  </div>
</>


);
}

export default WorkerPool;
