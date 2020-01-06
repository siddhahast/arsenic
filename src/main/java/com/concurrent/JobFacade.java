package com.concurrent;

import java.util.List;

import com.dibs.lib.concurrent.job.data.BatchProcess;
import com.dibs.service.v1.concurrency.data.JobDetails;

public interface JobFacade
{
    public List<JobDetails> filter(JobFilter filter);

    public BatchProcess readBatchProcess(String id);
    public BatchProcess batchCreate(List<Job<?>> jobs);

    public void update(Job<?> job);
}
