package com.concurrent;

import java.util.List;

public class DoNothingJobFacadeImpl implements JobFacade {
    @Override
    public List<JobDetails> filter(JobFilter filter) {
        return null;
    }

    @Override
    public BatchProcess readBatchProcess(String id) {
        return null;
    }

    @Override
    public BatchProcess batchCreate(List<Job<?>> jobs) {
        return null;
    }

    @Override
    public void update(Job<?> job) {

    }
}
