function StepTrace() {
const steps = [
{ icon: "📝", name: "LOG — Starting simulation", status: "done", time: "2ms" },
{ icon: "⏱", name: "SLEEP — 2000ms", status: "done", time: "2002ms" },
{ icon: "⚙", name: "COMPUTE — 500k", status: "running", time: "running..." },
{ icon: "🌐", name: "HTTP CALL", status: "waiting", time: "-" },
];

return ( <div className="steps-trace"> <div className="steps-title">Simulation Steps</div>

```
  {steps.map((s, i) => (
    <div key={i} className="step-row">
      <div className="step-icon">{s.icon}</div>
      <div className="step-name">{s.name}</div>
      <div className="step-dur">{s.time}</div>
      <div className={`step-check ${
        s.status === "done" ? "check-done" :
        s.status === "running" ? "check-run" :
        "check-wait"
      }`}>
        {s.status === "done" ? "✓" : s.status === "running" ? "◌" : "○"}
      </div>
    </div>
  ))}
</div>


);
}

export default StepTrace;
