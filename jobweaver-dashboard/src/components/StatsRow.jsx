function StatsRow() {
return ( <div className="stat-row"> <div className="stat"> <div>Total Jobs</div> <div className="stat-value c-accent">284</div> </div>

```
  <div className="stat">
    <div>Running</div>
    <div className="stat-value c-accent neon">284</div>
  </div>

  <div className="stat">
    <div>Pending</div>
    <div className="stat-value c-yellow neon-yellow">7</div>
  </div>

  <div className="stat">
    <div>Completed</div>
    <div className="stat-value c-green neon-green">251</div>
  </div>

  <div className="stat">
    <div>Retrying</div>
    <div className="stat-value c-purple neon-purple">6</div>
  </div>

  <div className="stat">
    <div>Dead Letter</div>
    <div className="stat-value c-orange neon-orange">8</div>
  </div>
</div>

);
}

export default StatsRow;
