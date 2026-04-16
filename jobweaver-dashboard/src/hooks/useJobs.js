import { useEffect, useState } from "react";
import { fetchJobs } from "../services/api";

const useJobs = () => {
  const [jobs, setJobs] = useState([]);

  useEffect(() => {
    const loadJobs = async () => {
      const data = await fetchJobs();
      setJobs(data);
    };

    loadJobs();

    const interval = setInterval(loadJobs, 5000); // auto refresh
    return () => clearInterval(interval);
  }, []);

  return jobs;
};

export default useJobs;
