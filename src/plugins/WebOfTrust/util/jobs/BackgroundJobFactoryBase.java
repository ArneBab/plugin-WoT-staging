/* This code is part of WoT, a plugin for Freenet. It is distributed
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL. */
package plugins.WebOfTrust.util.jobs;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Base class that provides all boilerplate necessary to implement the {@link BackgroundJobFactory}
 * interface.<br><br>
 * 
 * ATTENTION: This internally uses a {@link WeakHashMap}. As Java HashMaps never shrink, you must
 * not allow arbitrary strangers who are connected by network to cause creation of jobs using
 * this factory. They could cause denial of service by making the HashMap grow very large.
 *
 * @author bertm
 */
public abstract class BackgroundJobFactoryBase implements BackgroundJobFactory {
    /** Set of all live (i.e. not garbage collected) background jobs created by this instance. */
    private final WeakHashMap<BackgroundJob, Object> aliveJobSet =
            new WeakHashMap<BackgroundJob, Object>();

    @Override
    @Deprecated
    public final void terminateAll() {
        synchronized(aliveJobSet) {
            for (BackgroundJob bg : aliveJobSet.keySet()) {
                bg.terminate();
            }
        }
    }

    @Override
    @Deprecated
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
    @Deprecated
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
        // TODO: To get rid of the deprecation of terminateAll(), allTerminated() and
        // waitForTerminationOfAll(), do this:
        // - Use a different synchronization object than aliveJobSet here and in all the other places
        //   which use it. (This is needed because we will null it, see below)
        // - Make terminate() move the value of aliveJobSet to a different member variable, lets
        //   call it terminatingJobSet, and set aliveJobSet to null.
        // - Make registerNewJob() fail if aliveJobSet is null. Maybe throwing InterruptedException
        //   would also make sense, as this is a shutdown condition.
        // - Make waitForTerminationOfAll() wait for the content of terminatingJobSet

        synchronized(aliveJobSet) {
            aliveJobSet.put(job, null);
        }
    }
}
