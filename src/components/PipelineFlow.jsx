import { useEffect, useState } from "react";

function PipelineFlow({ job }) {
const [animatedSteps, setAnimatedSteps] = useState([]);

useEffect(() => {
if (!job) return;

```
setAnimatedSteps([]);

job.steps.forEach((step, i) => {
  setTimeout(() => {
    setAnimatedSteps((prev) => [...prev, step]);
  }, i * 400);
});
```

}, [job]);

if (!job) {
return <div style={{ padding: "20px" }}>Select a job</div>;
}

const labels = ["CLIENT", "API", "SCHEDULER", "WORKER", "OUTCOME"];

return (
<> <div className="pipeline-header">
Job Trace — <span className="c-accent">{job.id}</span> </div>

```
  <div className="pipeline-flow">
    {labels.map((label, i) => {
      const status = animatedSteps[i];

      return (
        <div
          key={i}
          className={`flow-card ${
            status === "running"
              ? "glow-blue"
              : status === "completed"
              ? "glow-green"
              : status === "failed"
              ? "glow-red"
              : ""
          }`}
        >
          <b>{label}</b> → {status || "waiting"}
        </div>
      );
    })}
  </div>
</>

);
}

export default PipelineFlow;
