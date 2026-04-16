import JobItem from "./JobItem";

function JobList() {
const jobs = [
{
id: "a1b2c3",
type: "SIMULATION",
status: "running",
time: "now",
retry: 0,
steps: ["completed","completed","completed","running","pending"],
},
{
id: "b2c3d4",
status: "pending",
time: "4s",
retry: 1,
steps: ["completed","completed","pending","pending","pending"],
},
{
id: "c3d4e5",
status: "completed",
time: "5s",
retry: 0,
steps: ["completed","completed","completed","completed","completed"],
},
{
id: "d4e5f6",
status: "failed",
time: "retrying",
retry: 3,
steps: ["completed","completed","failed","pending","pending"],
},
];

return (
<> <div className="section-header">Recent Jobs</div>

```
  {jobs.map((job) => (
    <JobItem key={job.id} job={job} />
  ))}
</>


);
}

export default JobList;
