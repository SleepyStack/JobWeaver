import { useEffect, useState } from "react";

function KafkaTopics() {
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
value: p.value + Math.floor(Math.random() * 5), // small increments
})),
}))
);
}, 2000);

```
return () => clearInterval(interval);
```

}, []);

return (
<> <div className="section-header">Kafka Topics</div>

```
  {topics.map((topic, i) => (
    <div key={i} className="topic-block">
      <div className="topic-name">{topic.name}</div>

      {topic.partitions.map((p, idx) => {
        const percent = Math.min(p.value / 300 * 100, 100);

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
</>

);
}

export default KafkaTopics;
