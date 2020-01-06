package com.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import com.cache.PeriodTransformer;
import com.common.def.ThreadLocalKey;
import com.common.def.ThreadLocalUtil;
import com.exception.JobCancellationException;
import com.exception.JobInterruptedException;
import com.exception.JobTimeoutException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.MDC;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.ExceptionUtils;

import com.dibs.lib.concurrent.job.data.BatchProcess;
import com.dibs.lib.concurrent.job.facade.impl.DoNothingJobFacadeImpl;
import com.dibs.service.common.def.ThreadLocalRequestMetric;
import com.dibs.service.common.log.LogArg;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class JobManager
{
    private static final Logger LOG = LogManager.getLogger(JobManager.class);

    //If batch timeout is defaulting to sum of individual jobs,
    // how much buffer to add to that sum.
    private static final Long BATCH_TIMEOUT_SUM_BUFFER_MILLIS = 50L;

    public static final Long BATCH_TIMEOUT_UNDEFINED = -1L;

    public static final Integer DEFAULT_THREAD_POOL_SIZE = 20;
    public static final Long DEFAULT_TIMEOUT_MILLIS = 30000L;
    public static final Long DEFAULT_FOREGROUND_BATCH_MILLIS = BATCH_TIMEOUT_UNDEFINED;
    public static final Long DEFAULT_EVICTION_MILLIS = 5000L;

    public static final JobFacade DEFAULT_JOB_FACADE = new DoNothingJobFacadeImpl();

    private ExecutorService delegateExecutorService;

    private ListeningExecutorService executorService;

    private ScheduledThreadPoolExecutor evictionExecutorService;

    private final Queue<Job<?>> backgroundJobs = new ConcurrentLinkedQueue<>();
    private final Set<JobSubmissionResult> foregroundJobs = new HashSet<>();

    private String name;

    private Integer threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    private Long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    private long foregroundBatchTimeoutMillis = DEFAULT_FOREGROUND_BATCH_MILLIS;
    private Long evictionFrequencyMillis = DEFAULT_EVICTION_MILLIS;

    private JobFacade jobFacade = DEFAULT_JOB_FACADE;

    private Map<SubmitMode, Set<Object>> blacklistThreadLocalKeys = defaultThreadLocalBlacklistKeys();

    public JobManager()
    {
        this(null);
    }

    private static Map<SubmitMode, Set<Object>> defaultThreadLocalBlacklistKeys()
    {
        Set<Object> backgroundKeys = new HashSet<>();
        backgroundKeys.add(LogArg.THREAD_LOCAL_KEY);
        backgroundKeys.add(ThreadLocalKey.OBSERVABLE_EVENT);
        backgroundKeys.add(StackTraceUtil.THREAD_LOCAL_KEY);
        Arrays.stream(ThreadLocalRequestMetric.values()).//
                forEach(m -> backgroundKeys.add(m));

        Set<Object> blockJobKeys = Sets.newHashSet(LogArg.THREAD_LOCAL_KEY,
                ThreadLocalKey.OBSERVABLE_EVENT,
                StackTraceUtil.THREAD_LOCAL_KEY);

        Map<SubmitMode, Set<Object>> threadLocalBlackistKeyMap = new HashMap<>();
        threadLocalBlackistKeyMap.put(SubmitMode.BACKGROUND, backgroundKeys);
        threadLocalBlackistKeyMap.put(SubmitMode.FOREGROUND, blockJobKeys);
        threadLocalBlackistKeyMap.put(SubmitMode.BACKGROUND_WAIT, blockJobKeys);

        return threadLocalBlackistKeyMap;
    }

    public JobManager(String name)
    {
        this.name = name;
    }

    /**
     * Execute jobs in parallel waiting for the response <tt>timeoutMillis</tt> ms.
     * Job are dependent on each other. If one job fails the the rest of
     * the jobs will be canceled. Underlying exception will be wrapped
     * inside of JobException.
     *
     * @param jobs
     * @throws JobException
     */
    public void submitForeground(Job<?>... jobs) throws JobException
    {
        submitForeground(foregroundBatchTimeoutMillis, jobs);
    }

    /**
     * Execute jobs in parallel waiting for the response <tt>timeout</tt> ms.
     * Job are dependent on each other. If one job fails the the rest of
     * the jobs will be canceled. Underlying exception will be wrapped
     * inside of JobException.
     *
     * @param batchTimeout
     * @param jobs
     * @throws JobException
     */
    public void submitForeground(long batchTimeout, Job<?>... jobs) throws JobException
    {
        prepare();

        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.debug("submitForeground() - submitting {} jobs", jobs.length);
        JobSubmissionResult result = null;

        try
        {
            result = submitJobs(SubmitMode.FOREGROUND, batchTimeout, jobs);
            batchTimeout = result.getBatchTimeoutMillis();
            result.resolveWithTimeout();
            LOG.debug("submitForeground() - finished {} jobs in {} ms", jobs.length, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
        catch (CancellationException ex)
        {
            LOG.debug("submitForeground() - Canceling jobs after {} ms for exception:",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS), ex);

            JobCancellationException jobCancelException = new JobCancellationException(Arrays.asList(jobs), ex);
            cancelJobs(result);
            throw jobCancelException;
        }
        catch (TimeoutException ex)
        {
            JobTimeoutException jobTimeoutException = new JobTimeoutException(Arrays.asList(jobs), batchTimeout, ex);
            cancelJobs(result);
            throw jobTimeoutException;
        }
        catch(InterruptedException ex)
        {
            JobInterruptedException jobInterruptedException = new JobInterruptedException(Arrays.asList(jobs), ex);
            cancelJobs(result);

            Thread.currentThread().interrupt();

            throw jobInterruptedException;
        }
        catch (Throwable ex)
        {
            JobException jobException = new JobException(Arrays.asList(jobs), ex);
            LOG.debug("submitForeground() - Exception encountered while executing jobs, canceling all remaining jobs", ex);
            cancelJobs(result);
            throw jobException;
        }
        finally
        {
            cleanUpForegroundJobsState(result);
        }
    }

    private void cleanUpForegroundJobsState(JobSubmissionResult result)
    {
        if (result != null) {
            result.getJobs().stream().forEach(Job::destroyThreadLocalCopy);

            foregroundJobs.remove(result);
        }
    }

    /**
     * Execute jobs in parallel waiting for the response <tt>timeoutMillis</tt> ms.
     * Job are independent on each other. If one job fails the the rest of
     * the jobs will not be impacted. Caller needs to check each job for the status.
     *
     * @param jobs
     */
    public void submitAndWait(Job<?>... jobs)
    {
        submitAndWait(foregroundBatchTimeoutMillis, jobs);
    }

    /**
     * Execute jobs in parallel waiting for the response <tt>timeout</tt> ms.
     * Job are independent on each other. If one job fails the the rest of
     * the jobs will not be impacted. Caller needs to check each job for the status.
     *
     * @param batchTimeoutMillis
     * @param jobs
     */
    public void submitAndWait(long batchTimeoutMillis, Job<?>... jobs)
    {
        prepare();

        JobSubmissionResult result = null;

        try
        {

            result = submitJobs(SubmitMode.BACKGROUND_WAIT, batchTimeoutMillis, jobs);

            for (Job<?> job : result.getJobs())
            {
                try
                {
                    long timeoutRemaining = result.getBatchTimeoutMillis() - job.getMillisSinceSubmission();

                    job.getFuture().get(timeoutRemaining < 0 ? 0 : timeoutRemaining, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException ex)
                {
                    LOG.error("submitAndWait(): Interrupted while running jobs. Cancelling all jobs. {} millis since submission and {} millis since execution",
                            job.getMillisSinceSubmission(), job.getMillisSinceExecution(), ex);

                    cancelJobs(result);

                    Thread.currentThread().interrupt();

                    throw new RuntimeException(ex);
                }
                catch (ExecutionException | TimeoutException ex)
                {
                    LOG.error("submitAndWait(): Error executing job. Throwing error {} millis since submission and {} millis since execution ",
                            job.getMillisSinceSubmission(), job.getMillisSinceExecution(), ex);
                }
            }
        }
        finally
        {
            cleanUpForegroundJobsState(result);
        }

    }

    /**
     * Execute jobs in parallel not waiting for the response. Completed jobs
     * will be cleared out of the background queue. If
     * <tt>JobFacade</tt> is configured, job status will be persisted and
     * made available via <tt>JobController</tt>. Returns <tt>BatchProcess</tt>
     * which contains persisted job details.
     *
     * @param jobs
     * @return
     */
    public BatchProcess submitBackground(Job<?>... jobs)
    {
        prepare();

        JobSubmissionResult jobSubmissionResult = submitJobs(SubmitMode.BACKGROUND, BATCH_TIMEOUT_UNDEFINED, jobs);
        backgroundJobs.addAll(jobSubmissionResult.getJobs());
        BatchProcess batchProcess = jobFacade.batchCreate(Arrays.asList(jobs));

        return batchProcess;
    }

    private JobSubmissionResult submitJobs(SubmitMode mode, long batchTimeout, Job<?>... jobs)
    {
        JobSubmissionResult result = new JobSubmissionResult();


        long timeoutMillisSum = 0;
        StackTraceElement[] stackTrace = StackTraceUtil.buildStackTrace();

        for (Job<?> job : jobs)
        {
            ListenableFuture<?> future = submitJob(job, mode, stackTrace);
            timeoutMillisSum += job.getTimeoutMillis();
            result.addJob(future, job);
        }


        result.setBatchTimeoutMillis(batchTimeout == BATCH_TIMEOUT_UNDEFINED ?
                timeoutMillisSum + BATCH_TIMEOUT_SUM_BUFFER_MILLIS :
                batchTimeout);

        if (mode == SubmitMode.FOREGROUND || mode == SubmitMode.BACKGROUND_WAIT)
        {
            foregroundJobs.add(result);
        }


        return result;
    }

    private ListenableFuture<?> submitJob(Job<?> job, SubmitMode mode, StackTraceElement[] stackTrace)
    {

        job.setMdcMap(MDC.getContext());
        job.setSubmissionTimeMillis(System.currentTimeMillis());
        job.setJobManagerName(getName());
        job.setSubmitMode(mode);
        job.setParentStackTrace(stackTrace);

        if (job.getTimeoutMillis() == null)
        {
            job.setTimeoutMillis(timeoutMillis);
        }

        loadThreadLocalKeys(job, mode);

        ListenableFuture<?> future = executorService.submit(job);
        job.setFuture(future);
        return future;
    }

    private void loadThreadLocalKeys(Job<?> job, SubmitMode mode)
    {
        job.setThreadLocalMap(ThreadLocalUtil.getAll());

        Set<Object> blacklistKeys = blacklistThreadLocalKeys.get(mode);

        if (CollectionUtils.isNotEmpty(blacklistKeys))
        {
            blacklistKeys.forEach(k -> job.getThreadLocalMap().remove(k));
        }

    }

    private void cancelJobs(JobSubmissionResult jobSubmissionResult)
    {
        if (jobSubmissionResult != null)
        {

            for (Job<?> job : jobSubmissionResult.getJobs())
            {
                LOG.debug("cancelJobs() - Canceling Job: {}", job.getClass().getName());

                if (!job.isFutureDone())
                {
                    job.getFuture().cancel(true);
                }
            }
        }

    }


    private void prepare()
    {
        // lazy init
        if (this.executorService == null)
        {
            this.delegateExecutorService = Executors.newFixedThreadPool(threadPoolSize, new DibsThreadFactory(name));
            this.executorService = MoreExecutors.listeningDecorator(delegateExecutorService);
        }

        // lazy init
        if (this.evictionExecutorService == null)
        {
            this.evictionExecutorService = new ScheduledThreadPoolExecutor(1);
            this.evictionExecutorService.scheduleAtFixedRate(new Evictor(), 0L, evictionFrequencyMillis, TimeUnit.MILLISECONDS);
        }

        if (name == null)
        {
            LOG.warn("Job manager name should be set {}", ExceptionUtils.getFullStackTrace(new Throwable()));
        }

    }

    @PreDestroy
    public void shutdown()
    {
        if (executorService != null)
        {
            executorService.shutdown();
        }

        if (evictionExecutorService != null)
        {
            evictionExecutorService.shutdownNow();
        }
    }

    class Evictor implements Runnable
    {

        @Override
        public void run()
        {
            try
            {
                ThreadLocalUtil.clear();

                LOG.trace("Evictor.run(): Running with {} jobs", backgroundJobs.size());

                if (!backgroundJobs.isEmpty())
                {
                    int backgroundJobSize = backgroundJobs.size();

                    // Process all the background jobs
                    for (int i = 0; i < backgroundJobSize; i++)
                    {
                        Job<?> job = backgroundJobs.poll();
                        if (job != null && job.getFuture() != null)
                        {
                            evictJob(job);
                        }
                    }
                }

            }
            catch (Exception ex)
            {
                LOG.error("EvictionProcessor.run(): Unhandled exception in eviction thread.", ex);
            }
        }

        public void evictJob(Job<?> job)
        {
            boolean cleanUpJobState = true;

            try
            {
                //copy into Evictor's thread local so we can have the job's logging context
                ThreadLocalUtil.setAll(new HashMap<>(job.getThreadLocalMap()));


                if (!job.isFutureDone() && job.isTimedOut())
                {
                    // Job has timed out, cancel it
                    job.getFuture().cancel(true);
                    LOG.error("Evictor.evictJob(): Async job {} timed out. Canceled {} millis after submission, and {} millis after execution. Full background stack {}",
                            job, job.getMillisSinceSubmission(), job.getMillisSinceExecution(), job.getFullBackgroundStack());

                    job.setStatus(JobStatus.TIMED_OUT);
                    jobFacade.update(job);
                }
                else if (job.getFuture().isCancelled())
                {
                    // Job has been canceled, log it
                    LOG.info("Evictor.evictJob(): Aysnc job {} canceled", job);
                }
                else if (!job.isFutureDone())
                {
                    // Job is still working, put it back
                    backgroundJobs.add(job);

                    //we're not finished yet so don't clean up the job's state
                    cleanUpJobState = false;
                }
                else if (job.isFutureDone())
                {
                    logFinshedJob(job);
                    //it's done, mark success/failure in db
                    if (job.getStatus() == JobStatus.PENDING || job.getStatus() == null)
                    {
                        job.setStatus(JobStatus.SUCCEEDED);
                    }

                    jobFacade.update(job);
                }

            }

            catch (Exception ex)
            {
                LOG.error("Evictor.evictJob(): Error evicting job {}", job, ex);
            }
            finally
            {
                ThreadLocalUtil.clear();

                //clean up the job's copy of threadlocal b/c otherwise it will keep it in memory
                if (cleanUpJobState)
                {
                    job.destroyThreadLocalCopy();
                }
            }
        }

        private void logFinshedJob(Job job)
        {
            if (job.getException() != null)
            {
                // exception
                LOG.error("Evictor.logFinshedJob(): Async job {} finished unsuccessfully {} millis after submission, and {} millis after execution. Full background stack {}",
                        job, job.getMillisSinceSubmission(), job.getMillisSinceExecution(), job.getFullBackgroundStack(), job.getException());
            }
            else if (LOG.isTraceEnabled())
            {
                //success trace log
                Arrays.stream(ThreadLocalRequestMetric.values()).//
                        forEach(m -> LogArg.add(m.getMdcKey(), m.getLong()));

                LOG.trace("Evictor.logFinshedJob(): Async job {} finished successfully {} millis after submission, and {} millis after execution. Full background trace {}", //
                        job, job.getMillisSinceSubmission(), job.getMillisSinceExecution(), job.getFullBackgroundStack());
            }
            else if (LOG.isDebugEnabled())
            {
                //success info log
                Arrays.stream(ThreadLocalRequestMetric.values()).//
                        forEach(m -> LogArg.add(m.getMdcKey(), m.getLong()));

                LOG.debug("Evictor.logFinshedJob(): Async job {} finished successfully {} millis after submission, and {} millis after execution.", //
                        job, job.getMillisSinceSubmission(), job.getMillisSinceExecution());
            }

        }

    }

    static class DibsThreadFactory implements ThreadFactory
    {

        private final String jobManagerName;

        private final AtomicInteger threadCounter;

        public DibsThreadFactory(String jobManagerName)
        {
            this.jobManagerName = jobManagerName;

            threadCounter = new AtomicInteger();
        }

        @Override
        public Thread newThread(Runnable r)
        {
            int threadId = threadCounter.getAndIncrement();

            ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

            Thread thread = defaultThreadFactory.newThread(r);

            thread.setName(jobManagerName + "-" + threadId);

            return thread;
        }

    }

    public List<Job<?>> getBackgroundJobs()
    {
        return new ArrayList<Job<?>>(backgroundJobs);
    }

    public List<Job<?>> getForegroundJobs()
    {
        List<Job<?>> jobs = new ArrayList<>();
        if (!foregroundJobs.isEmpty())
        {
            Collection<Collection<Job<?>>> batches = foregroundJobs.stream().map(JobSubmissionResult::getJobs).collect(Collectors.toList());

            for (Collection<Job<?>> batch : batches)
            {
                jobs.addAll(batch);
            }

        }

        return jobs;
    }

    public List<Job<?>> getJobs()
    {
        List<Job<?>> jobs = new ArrayList<>();
        jobs.addAll(getBackgroundJobs());
        jobs.addAll(getForegroundJobs());

        return jobs;
    }

    public Long getTimeoutMillis()
    {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis)
    {
        this.timeoutMillis = timeoutMillis;
    }

    public void setTimeout(String timeout)
    {
        Long millis = PeriodTransformer.MILLIS.transform(timeout);
        setTimeoutMillis(millis);

    }

    public Integer getThreadPoolSize()
    {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize)
    {
        this.threadPoolSize = threadPoolSize;
    }

    public Long getEvictionFrequencyMillis()
    {
        return evictionFrequencyMillis;
    }

    public void setEvictionFrequencyMillis(Long evictionFrequencyMillis)
    {
        this.evictionFrequencyMillis = evictionFrequencyMillis;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setJobFacade(JobFacade jobFacade)
    {
        this.jobFacade = jobFacade;
    }

    public long getForegroundBatchTimeoutMillis()
    {
        return foregroundBatchTimeoutMillis;
    }

    public void setForegroundBatchTimeoutMillis(long batchTimeoutMillis)
    {
        this.foregroundBatchTimeoutMillis = batchTimeoutMillis;
    }

    public Integer getNumberOfActiveThreads() {
        if (delegateExecutorService == null) {
            return 0;
        }
        return delegateExecutorService instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) delegateExecutorService).getActiveCount() : null;
    }

    private class JobSubmissionResult
    {
        private Map<ListenableFuture<?>, Job<?>> futuresMap;
        private long batchTimeoutMillis;
        private UUID uuid;

        JobSubmissionResult()
        {
            this.setUuid(UUID.randomUUID());
            this.setFuturesMap(new HashMap<>());
        }

        Collection<Job<?>> getJobs()
        {
            return getFuturesMap().values();
        }

        @Override
        public int hashCode() {
            return this.uuid.hashCode();
        }

        public Map<ListenableFuture<?>, Job<?>> getFuturesMap()
        {
            return futuresMap;
        }

        public void setFuturesMap(Map<ListenableFuture<?>, Job<?>> futuresMap)
        {
            this.futuresMap = futuresMap;
        }

        public ListenableFuture<List<Object>> getCollectiveFuture()
        {
            return Futures.allAsList(this.getFuturesMap().keySet());
        }

        public long getBatchTimeoutMillis()
        {
            return batchTimeoutMillis;
        }

        public void setBatchTimeoutMillis(long batchTimeoutMillis)
        {
            this.batchTimeoutMillis = batchTimeoutMillis;
        }

        public UUID getUuid()
        {
            return uuid;
        }

        public void setUuid(UUID uuid)
        {
            this.uuid = uuid;
        }

        public void addJob(ListenableFuture<?> future, Job<?> job)
        {
            futuresMap.put(future, job);
        }

        public List<Object> resolveWithTimeout() throws InterruptedException, ExecutionException, TimeoutException
        {
            return this.getCollectiveFuture().get(batchTimeoutMillis, TimeUnit.MILLISECONDS);
        }

    }

}
