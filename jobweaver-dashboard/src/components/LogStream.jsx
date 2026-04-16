import { useEffect, useState } from "react";

function LogStream({ job }) {
const [logs, setLogs] = useState([]);

useEffect(() => {
if (!job) return;

setLogs([]);

const sample = [
  "Job accepted",
  "Kafka event published",
  "Scheduler dispatching",
  "Worker started",
  "Execution running",
];

sample.forEach((msg, i) => {
  setTimeout(() => {
    setLogs((prev) => [...prev, msg]);
  }, i * 700);
});


}, [job]);

return ( <div>
{logs.map((log, i) => ( <div key={i} className="log-line">
{log} </div>
))} </div>
);
}

export default LogStream;
