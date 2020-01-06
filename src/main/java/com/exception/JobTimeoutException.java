package com.exception;

import com.concurrent.Job;
import com.concurrent.JobException;

import java.util.List;

public class JobTimeoutException extends JobException {

    public JobTimeoutException(List<Job<?>> allJobs, Throwable rootCause) {
        super(allJobs, rootCause);
    }
}
