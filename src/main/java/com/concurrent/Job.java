package com.concurrent;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.cache.PeriodTransformer;
import com.common.def.LoggingConstants;
import com.common.def.ThreadLocalKey;
import com.common.def.ThreadLocalUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Job<V> implements Callable<V>
{
    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    protected String id;
    protected String batchId;
    protected V result;
    protected Future<?> future;
    protected Throwable exception;
    protected Long expirationTime;
    protected Long timeoutMillis;
    protected Long submissionTimeMillis;
    protected Long executionStartTimeMillis;
    protected Map<Object, Object> threadLocalMap;
    protected Map<String, Object> mdcMap;
    protected boolean tearDownInvoked;
    protected String description;
    protected JobStatus status;
    private String jobManagerName;
    protected StackTraceElement[] parentStackTrace;
    protected SubmitMode submitMode;

    @Override
    public V call() throws Exception
    {
        try
        {
            initCall();

            result = execute();
        }
        catch (Error error)
        {
            LOG.error("call(): Error during job {}.", this, error);
        }
        catch (Exception ex)
        {
            if (ex instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            else
            {
                setException(ex);
            }
            this.status = JobStatus.FAILED;

            throw ex;
        }
        finally
        {
            //make sure we don't run destroy multiple times unnecessarily
            if (!tearDownInvoked)
            {
                tearDown();
            }

        }
        return result;
    }

    private void initCall()
    {
        // find the exact time this job will expire, starting from the moment it begins executing
        executionStartTimeMillis = System.currentTimeMillis();
        expirationTime = executionStartTimeMillis + timeoutMillis;

        threadLocalMap.put(ThreadLocalKey.REQUEST_START_MILLIS, executionStartTimeMillis);
        threadLocalMap.put(LoggingConstants.FIELD_JOB_MANAGER_NAME, jobManagerName);
        threadLocalMap.put(LoggingConstants.FIELD_JOB_MANAGER_MODE, submitMode);

        ThreadLocalUtil.setAll(threadLocalMap);
        Set<Entry<String, Object>> entrySet = mdcMap.entrySet();

        for (Entry<String, Object> entry : entrySet)
        {
            MDC.put(entry.getKey(), entry.getValue());
        }

        StackTraceUtil.addToThreadLocal(parentStackTrace);

        // if trace guid is null, generate new one and add it to
        // thread local and MDC context
        String traceGuid = ThreadLocalKey.TRACE_GUID.get();
        if (traceGuid == null)
        {
            traceGuid = UUID.randomUUID().toString();
            ThreadLocalKey.TRACE_GUID.put(traceGuid);
            MDC.put(ThreadLocalKey.TRACE_GUID.getMdcKey(), traceGuid);
        }
    }

    public abstract V execute() throws Exception;

    public String describe()
    {
        // Callers can override this method to provide a human readable description
        return null;
    }

    public boolean isTimedOut()
    {
        if (expirationTime == null)
        {
            // job has been queued but has not made it to the thread pool
            return false;
        }

        return System.currentTimeMillis() >= expirationTime;
    }

    public boolean isFutureDone()
    {
        return future.isDone();
    }

    public Long getMillisSinceSubmission()
    {
        return System.currentTimeMillis() - submissionTimeMillis;
    }

    public Long getMillisSinceExecution()
    {
        return executionStartTimeMillis == null ? null : System.currentTimeMillis() - executionStartTimeMillis;
    }

    private void tearDown()
    {
        //copy thread local to a new map before clearing it
        //so we can access it in Evictor for debugging
        threadLocalMap = new HashMap<>(ThreadLocalUtil.getAll());
        ThreadLocalUtil.clear();
        MDC.clear();
        tearDownInvoked = true;

        // see method comments
        destroy();
    }

    public V getResult()
    {
        return result;
    }

    public void setResult(V result)
    {
        this.result = result;
    }

    public Future<?> getFuture()
    {
        return future;
    }

    public void setFuture(Future<?> future)
    {
        this.future = future;
    }

    public Throwable getException()
    {
        return exception;
    }

    public void setException(Throwable exception)
    {
        this.exception = exception;
    }

    public boolean isExceptionSet()
    {
        return exception != null;
    }

    public Long getTimeoutMillis()
    {
        return timeoutMillis;
    }

    public void setTimeoutMillis(Long timeoutMillis)
    {
        this.timeoutMillis = timeoutMillis;
    }

    public Long getSubmissionTimeMillis()
    {
        return submissionTimeMillis;
    }

    public void setSubmissionTimeMillis(Long submissionTimeMillis)
    {
        this.submissionTimeMillis = submissionTimeMillis;
    }

    public void setTimeout(Long timout, TimeUnit unit)
    {
        this.timeoutMillis = TimeUnit.MILLISECONDS.convert(timout, unit);
    }

    public void setTimeout(String timeout)
    {
        Long millis = PeriodTransformer.MILLIS.transform(timeout);
        setTimeoutMillis(millis);
    }

    public Map<Object, Object> getThreadLocalMap()
    {
        return threadLocalMap;
    }

    public void setThreadLocalMap(Map<Object, Object> threadLocalMap)
    {
        this.threadLocalMap = threadLocalMap;
    }

    public Long getExpirationTime()
    {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime)
    {
        this.expirationTime = expirationTime;
    }

    public Map<String, Object> getMdcMap()
    {
        return mdcMap;
    }

    public String getId()
    {
        if (id == null)
        {
            id = UUID.randomUUID().toString();
        }

        return id;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setMdcMap(Map<String, Object> mdcMap)
    {
        this.mdcMap = mdcMap == null ? new HashMap() : new HashMap(mdcMap);
    }

    public Long getExecutionStartTimeMillis()
    {
        return executionStartTimeMillis;
    }

    public String getDescription()
    {
        return describe();
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public void setStatus(JobStatus status)
    {
        this.status = status;
    }

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    public String getJobManagerName()
    {
        return jobManagerName;
    }

    public void setJobManagerName(String jobManagerName)
    {
        this.jobManagerName = jobManagerName;
    }

    public boolean isSuccess()
    {
        return getException() == null && getResult() != null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public String toString()
    {

        ToStringBuilder toStringBuilder = new ToStringBuilder(this);

        Optional.ofNullable(buildToStringMap()).orElse(Collections.emptyMap()).
                entrySet().stream().filter(e -> e.getValue() != null).
                forEach(e -> toStringBuilder.append(e.getKey(), e.getValue()));

        return toStringBuilder.toString();
    }

    protected Map<String, Object> buildToStringMap()
    {
        Map<String, Object> toStringMap = new LinkedHashMap<>();
        toStringMap.put("id", id);
        toStringMap.put("status", status);
        toStringMap.put("jobManagerName", jobManagerName);
        toStringMap.put("batchId", batchId);
        return toStringMap;
    }

    public StackTraceList getFullBackgroundStack()
    {
        return (StackTraceList) threadLocalMap.get(StackTraceUtil.THREAD_LOCAL_KEY);
    }

    public StackTraceElement[] getParentStackTrace()
    {
        return parentStackTrace;
    }

    public void setParentStackTrace(StackTraceElement[] parentStackTrace)
    {
        this.parentStackTrace = parentStackTrace;
    }

    public SubmitMode getSubmitMode()
    {
        return submitMode;
    }

    public void setSubmitMode(SubmitMode mode)
    {
        this.submitMode = mode;
    }

    /*
     * Called after tearDown so jobs can optionally
     * override tearDown behavior without risk of forgetting
     * to call super.
     */
    public void destroy()
    {
    }

    public void destroyThreadLocalCopy()
    {
        if (threadLocalMap != null)
        {
            threadLocalMap.clear();
        }
    }

}