/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.resource.common;

import java.util.Date;
import java.util.List;

/**
 * Any pooled connection class must implement the {@link XAStatefulHolder} interface. It defines all the services
 * that must be implemented by the connection as well as the pooling lifecycle states.
 * Instances of this interface have to create and manage {@link XAResourceHolder}s.
 *
 * @author Ludovic Orban
 */
public interface XAStatefulHolder {

    enum State {
        /**
         * The state in which the resource is when it is closed and unusable.
         */
        CLOSED,

        /**
         * The state in which the resource is when it is available in the pool.
         */
        IN_POOL,

        /**
         * The state in which the resource is when it out of the pool but accessible by the application.
         */
        ACCESSIBLE,

        /**
         * The state in which the resource is when it out of the pool but not accessible by the application.
         */
        NOT_ACCESSIBLE
    };

    /**
     * Get the current resource state.
     * <p>This method is thread-safe.</p>
     * @return the current resource state.
     */
    public State getState();

    /**
     * Set the current resource state.
     * <p>This method is thread-safe.</p>
     * @param state the current resource state.
     */
    public void setState(State state);

    /**
     * Register an implementation of {@link StateChangeListener}.
     * @param listener the {@link StateChangeListener} implementation to register.
     */
    public void addStateChangeEventListener(StateChangeListener listener);

    /**
     * Unregister an implementation of {@link StateChangeListener}.
     * @param listener the {@link StateChangeListener} implementation to unregister.
     */
    public void removeStateChangeEventListener(StateChangeListener listener);

    /**
     * Get the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
     * {@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
     * <p>This method is thread-safe.</p>
     * @return the list of {@link XAResourceHolder}s created by this
     *         {@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
     */
    public List<XAResourceHolder> getXAResourceHolders();

    /**
     * Create a disposable handler used to drive a pooled instance of
     * {@link bitronix.tm.resource.common.XAStatefulHolder}.
     * <p>This method is thread-safe.</p>
     * @return a resource-specific disaposable connection object.
     * @throws Exception a resource-specific exception thrown when the disaposable connection cannot be created.
     */
    public Object getConnectionHandle() throws Exception;

    /**
     * Close the physical connection that this {@link bitronix.tm.resource.common.XAStatefulHolder} represents.
     * @throws Exception a resource-specific exception thrown when there is an error closing the physical connection.
     */
    public void close() throws Exception;

    /**
     * Get the date at which this object was last released to the pool. This is required to check if it is eligible
     * for discard when the containing pool needs to shrink.
     * @return the date at which this object was last released to the pool or null if it never left the pool.
     */
    public Date getLastReleaseDate();

    /**
     * Get the date at which this object was created in the pool.
     * @return the date at which this object was created in the pool.
     */
    public Date getCreationDate();
}
