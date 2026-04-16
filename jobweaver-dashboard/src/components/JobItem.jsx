function JobItem({ job }) {
return (
<div className={`job-item ${job.status === "running" ? "active" : ""}`}>

```
  {/* JOB ID */}
  <div className="job-id neon">
    {job.id}
  </div>

  {/* META ROW */}
  <div className="job-meta">
    <span className="job-type">
      {job.type}
    </span>

    <span
      className={`badge badge-${job.status} ${
        job.status === "running"
          ? "neon"
          : job.status === "completed"
          ? "neon-green"
          : job.status === "failed"
          ? "neon-red"
          : job.status === "pending"
          ? "neon-yellow"
          : ""
      }`}
    >
      {job.status.toUpperCase()}
    </span>
  </div>

  {/* TIME */}
  <div className="job-time">
    {job.time}
  </div>

</div>

);
}

export default JobItem;
