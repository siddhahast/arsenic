package com.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

public class JobException extends Exception
{
    private List<Job<?>> allJobs;
    private List<Job<?>> completeJobs;
    private List<Job<?>> incompleteJobs;

    public JobException(List<Job<?>> allJobs, Throwable rootCause)
    {
        super(rootCause);
        this.allJobs = allJobs;
        this.completeJobs = new ArrayList<>();
        this.incompleteJobs = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(allJobs))
        {
            for (Job<?> job : allJobs)
            {
                if (!job.isFutureDone() || job.getFuture().isCancelled())
                {
                    incompleteJobs.add(job);
                }
                else
                {
                    completeJobs.add(job);

                }
            }

        }

    }

    public List<? extends Job> getCompleteJobs()
    {
        return completeJobs;
    }

    public List<? extends Job> getIncompleteJobs()
    {
        return incompleteJobs;
    }

    public List<Job<?>> getAllJobs()
    {
        return allJobs;
    }

    @Override
    public String toString()
    {
        Collection<Class> completeClasses = completeJobs.stream().map(c->c.getClass()).collect(Collectors.toList());
        Collection<Class> incompleteClasses = incompleteJobs.stream().map(c->c.getClass()).collect(Collectors.toList());

        String counts = String.format("(%d) complete + (%d) incomplete = (%d) total jobs", incompleteJobs.size(), completeJobs.size(), incompleteJobs.size() + completeJobs.size());

        return new StringBuilder().append(getMessage()).append("\n").append(counts).append("\ncompleteClasses=").append(completeClasses).append("\nincompleteClasses=").append(incompleteClasses)
                .toString();

    }

}
