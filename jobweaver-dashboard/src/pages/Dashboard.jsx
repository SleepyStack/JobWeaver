import Topbar from "../components/Topbar";
import StatsRow from "../components/StatsRow";
import JobList from "../components/JobList";
import PipelineFlow from "../components/PipelineFlow";
import RightPanel from "../components/RightPanel";

import "../App.css";

function Dashboard() {
return (
<>
{/* TOP BAR */} <Topbar />

```
  {/* MAIN GRID */}
  <div className="main">

    {/* STATS */}
    <StatsRow />

    {/* LEFT PANEL */}
    <aside className="panel-left">
      <JobList />
    </aside>

    {/* CENTER PANEL */}
    <main className="panel-center">
      <PipelineFlow />
    </main>

    {/* RIGHT PANEL */}
    <RightPanel />

  </div>
</>

);
}

export default Dashboard;
