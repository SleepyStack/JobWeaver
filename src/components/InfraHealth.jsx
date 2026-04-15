function InfraHealth() {
const infra = [
{ name: "postgres-api", port: "5432" },
{ name: "postgres-scheduler", port: "5433" },
{ name: "postgres-worker", port: "5434" },
{ name: "kafka", port: "9092" },
];

return (
<> <div className="section-header">Infrastructure</div>

```
  {infra.map((i, index) => (
    <div key={index} className="infra-row">
      <div className="infra-name">{i.name}</div>
      <div className="infra-port">:{i.port}</div>
      <div className="infra-status status-up">UP</div>
    </div>
  ))}
</>


);
}

export default InfraHealth;
