package com.exception;

import com.concurrent.Job;
import com.concurrent.JobException;

import java.util.List;

public class JobInterruptedException extends JobException {
    public JobInterruptedException(List<Job<?>> allJobs, Throwable rootCause) {
        super(allJobs, rootCause);
    }
}
