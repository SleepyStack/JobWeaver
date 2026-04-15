const BASE_URL = "http://localhost:8080"; // your backend API

export const getJobs = async () => {
const res = await fetch(`${BASE_URL}/jobs`);
return res.json();
};


export const getStats = async () => {
const res = await fetch(`${BASE_URL}/stats`);
return res.json();
};

export const getJobTrace = async (jobId) => {
const res = await fetch(`${BASE_URL}/jobs/${jobId}/trace`);
return res.json();
};


export const getKafkaTopics = async () => {
const res = await fetch(`${BASE_URL}/kafka`);
return res.json();
};
