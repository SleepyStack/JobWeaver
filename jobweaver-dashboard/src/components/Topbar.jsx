function Topbar() {
  return (
    <header className="topbar">
      <div className="logo neon">JOBWEAVER</div>
      <div className="health-row">
        <div className="health-chip">API :8080</div>
        <div className="health-chip">SCHEDULER :8081</div>
        <div className="health-chip">WORKER :8082</div>
        <div className="health-chip">KAFKA :9092</div>
        
      </div>
    </header>
  );
}

export default Topbar;
