import { useEffect, useState } from "react";

function RightPanel() {
  const [topics, setTopics] = useState([
    {
      name: "job-created",
      color: "var(--yellow)",
      partitions: [
        { label: "p0", value: 204 },
        { label: "p1", value: 198 },
        { label: "p2", value: 187 },
      ],
    },
    {
      name: "run-job",
      color: "var(--accent)",
      partitions: [
        { label: "p0", value: 187 },
        { label: "p1", value: 172 },
        { label: "p2", value: 165 },
      ],
    },
    {
      name: "job-completed",
      color: "var(--green)",
      partitions: [{ label: "p0", value: 251 }],
    },
    {
      name: "job-failed",
      color: "var(--red)",
      partitions: [{ label: "p0", value: 33 }],
    },
  ]);

  useEffect(() => {
    const interval = setInterval(() => {
      setTopics((prev) =>
        prev.map((topic) => ({
          ...topic,
          partitions: topic.partitions.map((p) => ({
            ...p,
            value: p.value + Math.floor(Math.random() * 5),
          })),
        }))
      );
    }, 2000);

    return () => clearInterval(interval);
  }, []);

  return (
    <aside className="panel-right">

      <div className="section-header">Kafka Topics</div>

      {topics.map((topic, i) => (
        <div key={i} className="topic-block">
          <div className="topic-name">{topic.name}</div>

          {topic.partitions.map((p, idx) => {
            const percent = Math.min((p.value / 300) * 100, 100);

            return (
              <div key={idx} className="topic-bar-row">
                <span className="topic-bar-label">{p.label}</span>

                <div className="topic-bar-bg">
                  <div
                    className="topic-bar-fill"
                    style={{
                      width: `${percent}%`,
                      background: topic.color,
                    }}
                  />
                </div>

                <span className="topic-bar-val">{p.value}</span>
              </div>
            );
          })}
        </div>
      ))}

      <div className="section-header">Retry Activity</div>

      {[
        { id: "c3d4e5", msg: "retry 2/3", err: "Simulation error" },
        { id: "d4e5f6", msg: "retry 1/2", err: "Timeout" },
        { id: "a7b8c9", msg: "DLQ", err: "Max retries reached" },
      ].map((r, i) => (
        <div key={i} className="retry-event">
          <div className="retry-body">
            <div className="retry-title">
              {r.id} — {r.msg}
            </div>
            <div className="retry-meta">{r.err}</div>
          </div>
        </div>
      ))}

      <div className="section-header">Infrastructure</div>

      {[
        { name: "postgres-api", port: "5432" },
        { name: "postgres-scheduler", port: "5433" },
        { name: "postgres-worker", port: "5434" },
        { name: "kafka", port: "9092" },
      ].map((i, idx) => (
        <div key={idx} className="infra-row">
          <div>{i.name}</div>
          <div>:{i.port}</div>
          <div className="status-up">UP</div>
        </div>
      ))}

      <div className="section-header">Worker Pool</div>

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
      </div>

    </aside>
  );
}

export default RightPanel;