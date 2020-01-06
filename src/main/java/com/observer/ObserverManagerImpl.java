package com.observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.ass.AssFact;
import com.cache.*;
import com.common.def.ThreadLocalRequestMetric;
import com.concurrent.JobManager;
import com.exception.ObserverException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


@Component
public class ObserverManagerImpl implements ObserverManager, JobManagerProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(ObserverManagerImpl.class);

    //tracks events mapped to phases in which they've been invoked
    private SingletonCacheWrapper<ObservableEventsByPhase> eventPhaseCache;

    Comparator<BaseObserver> baseObserverComparator = (o1, o2) -> {
        // if same order, then compare class names for order
        if (o1.getOrder() == null && o2.getOrder() == null) {
            return o1.getClass().getName().compareTo(o2.getClass().getName());
        }
        // nulls last
        if (o1.getOrder() == null) {
            return 1;
        }
        if (o2.getOrder() == null) {
            return -1;
        }
        // if same order, then compare class names for order
        if (o1.getOrder().compareTo(o2.getOrder()) == 0) {
            return o1.getClass().getName().compareTo(o2.getClass().getName());
        }
        return o1.getOrder().compareTo(o2.getOrder());
    };

    private final Set<BaseObserver> observers = new TreeSet<>(baseObserverComparator);

    private Cache<ObserverClass, Boolean> observerClassAcceptedCache;

    @Autowired
    private ApplicationContext applicationContext;

    private JobManager jobManager;

    @PostConstruct
    public void init()
    {
        //
        // init job manager
        //
        jobManager = new JobManager("observerJobManager");
        jobManager.setThreadPoolSize(100);

        //
        // init phase cache
        //
        Cache<SingletonCacheWrapper, ObservableEventsByPhase> phaseCache = (Cache) new ThreadLocalUtilCache<>().//
                setName("observableEventPhaseCache").//
                setClone(false).//
                init(applicationContext);

        eventPhaseCache = new SingletonCacheWrapper<>();
        eventPhaseCache.setCache(phaseCache);

        //
        // init observer class cache
        //
        observerClassAcceptedCache = (Cache) new GuavaCache<>().//
                setName("observerClassAcceptedCache").//
                setClone(false).//
                init(applicationContext);

        //
        // scans observers
        //
        applicationContext.getBeansOfType(BaseObserver.class).values().//
                stream().forEach(o -> registerObserver(o));
    }

    /*
     * Returns true if the given class is accepted by the given observer
     */
    protected <O, P> boolean isClassAcceptedByObserver(BaseObserver<O, P> observer, Class clazz) {
        ObserverClass<O, P> observerClass = new ObserverClass<>(observer, clazz);
        Boolean isAccepted = observerClassAcceptedCache.get(observerClass);
        if (isAccepted == null) {
            BiFunction<BaseObserver<O,P>, Class, Boolean> filterObserverClassTypes = (baseObserver, observableClass) ->
            {

                if (!observer.doesAllowSubclasses() && observer.getObservableClass().equals(observableClass))
                {
                    return true;
                }
                if (observer.doesAllowSubclasses() && observer.getObservableClass().isAssignableFrom(observableClass))
                {
                    return true;
                }

                return false;
            };

            isAccepted = filterObserverClassTypes.apply(observer, clazz);
            observerClassAcceptedCache.put(observerClass, isAccepted);
        }

        return isAccepted;
    }

    /*
     * Returns true if the given event will be run by the given observer
     */
    protected <O, P> boolean canRunEvent(BaseObserver<O, P> observer, ObservableEvent<O, P> event, Throwable exception, ConsumedEvents eventsByObserver, ObserverPhase phase)
    {
        try
        {
            boolean observableClassMatch = isClassAcceptedByObserver(observer, event.getObservable().getClass());
            if (!observableClassMatch) {
                return false;
            }

            boolean observableMatch = Optional.ofNullable(observer.getObservablePredicate()).//
                    orElse(DumbPredicate.TRUE).test(event.getObservable());
            if (!observableMatch)
            {
                return false;
            }

            boolean parameterMatch = Optional.ofNullable(observer.getParameterPredicate()).//
                    orElse(DumbPredicate.TRUE).test(event.getParameter());
            if (!parameterMatch)
            {
                return false;
            }

            Predicate exceptionPredicate = Optional.ofNullable(observer.getExceptionPredicate()).orElse(DumbPredicate.FALSE);
            boolean exceptionMatch = exception == null || exceptionPredicate.test(exception);
            if (!exceptionMatch)
            {
                return false;
            }

            boolean phaseMatch = observer.getPhase() == phase;
            if (!phaseMatch)
            {
                return false;
            }

            boolean added = eventsByObserver.addEventIfUnique(observer, event);
            if (!added)
            {
                return false;
            }

            return true;
        }
        catch (Exception ex)
        {
            LOG.error("test(): Error testing event={}, observer={}, exception={}. Returning false", //
                    event, observer, exception, ex);

            return false;
        }

    }

    /*
     * Run the observer on the given list of events
     */
    protected boolean consume(BaseObserver observer, List<ObservableEvent> events, Throwable exception, ConsumedEvents eventsByObserver, ObserverPhase phase)
    {
        boolean isRun = true;

        try
        {
            String mode = null;

            events = events.stream().distinct().collect(Collectors.toList());

            Long timeout = observer.getTimeout() == null ? //
                    JobManager.DEFAULT_TIMEOUT_MILLIS : //
                    PeriodTransformer.MILLIS.transform(observer.getTimeout());

            List<ObserverJob> jobs = new ArrayList<>();
            if (observer instanceof Observer && events.size() > 1) {
                for (ObservableEvent event: events) {
                    ObserverJob singleEventJob = new ObserverJob().//
                            setEvents(Arrays.asList(event)).//
                            setObserver(observer);
                    jobs.add(singleEventJob);
                }
            } else {
                ObserverJob job = new ObserverJob().//
                        setEvents(events).//
                        setObserver(observer);
                jobs.add(job);
            }

            if (observer.getMode() == Mode.BACKGROUND)
            {
                for (ObserverJob job: jobs) {
                    job.setTimeoutMillis(timeout);
                    jobManager.submitBackground(job);
                }
                mode = "background";

            } else if (observer.getMode() == Mode.FOREGROUND){
                for (ObserverJob job: jobs) {
                    jobManager.submitForeground(timeout, job);
                }
                mode = "foreground";
            } else {
                for (ObserverJob job: jobs) {
                    job.execute();
                }
                mode = "samethread";
            }

            String exClassName = exception == null ? null : exception.getClass().getName();
            LOG.trace("update(): isRun={}, events={}, observer={}, exception={}, mode={} ", isRun, events, observer, exClassName, mode);
            if (mode != null && mode != "samethread") {
                LOG.info("Submitted {} {} observer jobs to job manager which currently " +
                                "has {} background jobs, {} foreground jobs, and {} active threads", jobs.size(), mode,
                        jobManager.getBackgroundJobs() == null ? 0 : jobManager.getBackgroundJobs().size(),
                        jobManager.getForegroundJobs() == null ? 0 : jobManager.getForegroundJobs().size(),
                        jobManager.getNumberOfActiveThreads());
            }
        }
        catch (Exception ex)
        {
            if (observer.isThrowExceptions())
            {
                throw new ObserverException("Error calling update on " + observer + " with " + events, ex);
            }
            else
            {
                LOG.error("update(): Error calling update on {} with {}", observer, events, ex);
            }
        }

        return isRun;
    }

    @Override
    public void registerObserver(BaseObserver observer)
    {
        AssFact.BAD_ARGUMENT.isFalse(observer.getPhase() == ObserverPhase.PRE_COMMIT
                && observer.getMode() == Mode.BACKGROUND, observer.getClass().getSimpleName() + ": bad observer setup. Async observer " +
                "will not roll back the commit. If you don't need to rollback, use POST_COMMIT.");
        AssFact.BAD_ARGUMENT.isTrue(observers.add(observer), observer.getClass().getSimpleName() + ": dupicate observer found");
    }

    private ObservableEventsByPhase getPhaseCache()
    {
        ObservableEventsByPhase observableEventPhaseCache = eventPhaseCache.get();
        if (observableEventPhaseCache == null)
        {
            eventPhaseCache.put(observableEventPhaseCache = new ObservableEventsByPhase());
        }
        return observableEventPhaseCache;
    }

    @Override
    public ConsumedEvents consume(Collection<ObservableEvent<?, ?>> events, Throwable exception, ObserverPhase phase)
    {

        ConsumedEvents eventsByObserver = new ConsumedEvents();

        if (CollectionUtils.isNotEmpty(events))
        {
            // need to prevent java.util.ConcurrentModificationException
            // event added after this point will be ignore in this phase
            List<ObservableEvent> copyOfEvents = new ArrayList<ObservableEvent>(events);
            ObservableEventsByPhase phaseCache = getPhaseCache();
            // Handle event observers
            for (BaseObserver observer: observers) {

                List<ObservableEvent> eventsToRun = new ArrayList<>();
                for (ObservableEvent event: copyOfEvents) {
                    if (canRunEvent(observer, event, exception, eventsByObserver, phase) &&
                            !phaseCache.isInvoked(observer, phase, event)) {
                        eventsToRun.add(event);
                    }
                }

                if (CollectionUtils.isNotEmpty(eventsToRun)) {
                    eventsToRun.forEach(event -> phaseCache.addInvoked(observer, phase, event));
                    consume(observer, eventsToRun, exception, eventsByObserver, phase);
                    ThreadLocalRequestMetric.OBSERVERS_TRIGGERED.add(1);
                }

                LOG.debug("consume(): events={}, observer={}, consumedObservers={}", //
                        events, observer);
            }
        }

        return eventsByObserver;
    }

    public JobManager getJobManager()
    {
        return jobManager;
    }

    public void setJobManager(JobManager jobManager)
    {
        this.jobManager = jobManager;
    }

    public void setEventPhaseCache(SingletonCacheWrapper<ObservableEventsByPhase> eventPhaseCache)
    {
        this.eventPhaseCache = eventPhaseCache;
    }

    public SingletonCacheWrapper<ObservableEventsByPhase> getEventPhaseCache()
    {
        return eventPhaseCache;
    }

    public Cache<ObserverClass, Boolean> getObserverClassAcceptedCache() {
        return observerClassAcceptedCache;
    }

    public void setObserverClassAcceptedCache(Cache<ObserverClass, Boolean> observerClassAcceptedCache) {
        this.observerClassAcceptedCache = observerClassAcceptedCache;
    }

    @Override
    public List<JobManager> getJobManagers()
    {
        return Collections.singletonList(jobManager);
    }

    protected Set<BaseObserver> getObservers() {
        return observers;
    }
}
