package com.observer;

import com.concurrent.JobManager;

import java.util.List;

public interface JobManagerProvider
{
    public List<JobManager> getJobManagers();

}
