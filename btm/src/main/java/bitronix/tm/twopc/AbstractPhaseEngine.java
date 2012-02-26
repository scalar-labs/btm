/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.twopc;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.CollectionUtils;

import javax.transaction.xa.XAException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract phase execution engine.
 *
 * @author lorban
 */
public abstract class AbstractPhaseEngine {

    private final static Logger log = LoggerFactory.getLogger(AbstractPhaseEngine.class);

    private final Executor executor;

    protected AbstractPhaseEngine(Executor executor) {
        this.executor = executor;
    }

    /**
     * Execute the phase. Resources receive the phase command in position order (reversed or not). If there is more than
     * once resource in a position, command is sent in enlistment order (again reversed or not).
     * If {@link bitronix.tm.Configuration#isAsynchronous2Pc()} is true, all commands in a given position are sent
     * in parallel by using the detected {@link Executor} implementation.
     * @param resourceManager the {@link XAResourceManager} containing the enlisted resources to execute the phase on.
     * @param reverse true if jobs should be executed in reverse position / enlistment order, false for natural position / enlistment order.
     * @throws PhaseException if one or more resource threw an exception during phase execution.
     * @see bitronix.tm.twopc.executor.SyncExecutor
     * @see bitronix.tm.twopc.executor.AsyncExecutor
     */
    protected void executePhase(XAResourceManager resourceManager, boolean reverse) throws PhaseException {
        SortedSet<Integer> positions;
        if (reverse) {
            positions = resourceManager.getReverseOrderPositions();
            if (log.isDebugEnabled()) log.debug("executing phase on " + resourceManager.size() + " resource(s) enlisted in " + positions.size() + " position(s) in reverse position order");
        }
        else {
            positions = resourceManager.getNaturalOrderPositions();
            if (log.isDebugEnabled()) log.debug("executing phase on " + resourceManager.size() + " resource(s) enlisted in " + positions.size() + " position(s) in natural position order");
        }

        List<JobsExecutionReport> positionErrorReports = new ArrayList<JobsExecutionReport>();

        for (Integer positionKey : positions) {
            List<XAResourceHolderState> resources;
            if (reverse) {
                resources = resourceManager.getReverseOrderResourcesForPosition(positionKey);
            } else {
                resources = resourceManager.getNaturalOrderResourcesForPosition(positionKey);
            }

            if (log.isDebugEnabled()) log.debug("running " + resources.size() + " job(s) for position '" + positionKey + "'");
            JobsExecutionReport report = runJobsForPosition(resources);
            if (report.getExceptions().size() > 0) {
                if (log.isDebugEnabled()) log.debug(report.getExceptions().size() + " error(s) happened during execution of position '" + positionKey + "'");
                positionErrorReports.add(report);
                break;
            }
            if (log.isDebugEnabled()) log.debug("ran " + resources.size() + " job(s) for position '" + positionKey + "'");
        }

        if (positionErrorReports.size() > 0) {
            // merge all resources and exceptions lists
            List<Exception> exceptions = new ArrayList<Exception>();
            List<XAResourceHolderState> resources = new ArrayList<XAResourceHolderState>();

            for (JobsExecutionReport report : positionErrorReports) {
                exceptions.addAll(report.getExceptions());
                resources.addAll(report.getResources());
            }

            throw new PhaseException(exceptions, resources);
        }
    }

    private JobsExecutionReport runJobsForPosition(List<XAResourceHolderState> resources) {
        List<Job> jobs = new ArrayList<Job>();
        List<Exception> exceptions = new ArrayList<Exception>();
        List<XAResourceHolderState> errorResources = new ArrayList<XAResourceHolderState>();

        // start threads
        for (XAResourceHolderState resource : resources) {
            if (!isParticipating(resource)) {
                if (log.isDebugEnabled()) log.debug("skipping not participating resource " + resource);
                continue;
            }

            Job job = createJob(resource);
            Object future = executor.submit(job);
            job.setFuture(future);
            jobs.add(job);
        }

        // wait for threads to finish and check results
        for (Job job : jobs) {
            Object future = job.getFuture();
            while (!executor.isDone(future)) {
                executor.waitFor(future, 1000L);
            }

            XAException xaException = job.getXAException();
            RuntimeException runtimeException = job.getRuntimeException();

            if (xaException != null) {
                String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer().extractExtraXAExceptionDetails(xaException);
                if (log.isDebugEnabled()) log.debug("error executing " + job + ", errorCode=" + Decoder.decodeXAExceptionErrorCode(xaException) +
                        (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails));
                exceptions.add(xaException);
                errorResources.add(job.getResource());
            } else if (runtimeException != null) {
                if (log.isDebugEnabled()) log.debug("error executing " + job);
                exceptions.add(runtimeException);
                errorResources.add(job.getResource());
            }
        }

        if (log.isDebugEnabled()) log.debug("phase executed with " + exceptions.size() + " exception(s)");
        return new JobsExecutionReport(exceptions, errorResources);
    }

    /**
     * Determine if a resource is participating in the phase or not. A participating resource gets
     * a job created to execute the phase's command on it.
     * @param xaResourceHolderState the resource to check for its participation.
     * @return true if the resource must participate in the phase.
     */
    protected abstract boolean isParticipating(XAResourceHolderState xaResourceHolderState);

    /**
     * Create a {@link Job} that is going to execute the phase command on the given resource.
     * @param xaResourceHolderState the resource that is going to receive a command.
     * @return the {@link Job} that is going to execute the command.
     */
    protected abstract Job createJob(XAResourceHolderState xaResourceHolderState);

    /**
     * Log exceptions that happened during a phase failure.
     * @param ex the phase exception.
     */
    protected void logFailedResources(PhaseException ex) {
        List<Exception> exceptions = ex.getExceptions();
        List<XAResourceHolderState> resources = ex.getResourceStates();

        for (int i = 0; i < exceptions.size(); i++) {
            Exception e = exceptions.get(i);
            XAResourceHolderState holderState = resources.get(i);
            log.error("resource " + holderState.getUniqueName() + " failed on " + holderState.getXid(), e);
        }
    }

    protected static Set<String> collectResourcesUniqueNames(List<XAResourceHolderState> resources) {
        Set<String> uniqueNames = new HashSet<String>();

        for (XAResourceHolderState resourceHolderState : resources) {
            uniqueNames.add(resourceHolderState.getUniqueName());
        }

        return uniqueNames;
    }

    protected static List<XAResourceHolderState> collectNotInterestedResources(List<XAResourceHolderState> allResources, List<XAResourceHolderState> interestedResources) {
        List<XAResourceHolderState> result = new ArrayList<XAResourceHolderState>();

        for (XAResourceHolderState resourceHolderState : allResources) {
            if (!CollectionUtils.containsByIdentity(interestedResources, resourceHolderState))
                result.add(resourceHolderState);
        }

        return result;
    }

    private final static class JobsExecutionReport {
        private final List<Exception> exceptions;
        private final List<XAResourceHolderState> resources;

        private JobsExecutionReport(List<Exception> exceptions, List<XAResourceHolderState> resources) {
            this.exceptions = Collections.unmodifiableList(exceptions);
            this.resources = Collections.unmodifiableList(resources);
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

        public List<XAResourceHolderState> getResources() {
            return resources;
        }
    }

}
