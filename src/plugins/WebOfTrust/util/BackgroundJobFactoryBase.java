package plugins.WebOfTrust.util;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Base class that provides all boilerplate necessary to implement the {@link BackgroundJobFactory}
 * interface.
 *
 * @author bertm
 */
public abstract class BackgroundJobFactoryBase implements BackgroundJobFactory {
    /** Set of all live (i.e. not garbage collected) background jobs created by this instance. */
    private final WeakHashMap<BackgroundJob, Object> aliveJobSet =
            new WeakHashMap<BackgroundJob, Object>();

    @Override
    public final void terminateAll() {
        synchronized(aliveJobSet) {
            for (BackgroundJob bg : aliveJobSet.keySet()) {
                bg.terminate();
            }
        }
    }

    @Override
    public final boolean allTerminated() {
        synchronized(aliveJobSet) {
            for (BackgroundJob bg : aliveJobSet.keySet()) {
                if (!bg.isTerminated()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public final void waitForTerminationOfAll(long timeoutMillis) throws InterruptedException {
        ArrayList<BackgroundJob> jobs;
        synchronized(aliveJobSet) {
            jobs = new ArrayList<BackgroundJob>(aliveJobSet.keySet());
        }
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (BackgroundJob job : jobs) {
            job.waitForTermination(timeoutMillis);
            timeoutMillis = deadline - System.currentTimeMillis();
            if (timeoutMillis <= 0) {
                return;
            }
        }
    }

    /**
     * Registers a background job with this factory. Implementations must ensure to invoke this
     * method for each background job they construct.
     */
    protected final void registerNewJob(BackgroundJob job) {
        synchronized(aliveJobSet) {
            aliveJobSet.put(job, null);
        }
    }
}