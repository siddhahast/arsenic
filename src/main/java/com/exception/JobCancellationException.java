package com.exception;

import com.concurrent.Job;
import com.concurrent.JobException;

import java.util.List;

public class JobCancellationException extends JobException {

    public JobCancellationException(List<Job<?>> allJobs, Throwable rootCause) {
        super(allJobs, rootCause);
    }
}
