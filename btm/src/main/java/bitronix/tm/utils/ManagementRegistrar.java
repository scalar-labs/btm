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
package bitronix.tm.utils;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * JMX facade.
 * <p/>
 * In case there is no JMX implementation available, calling methods of this class have no effect.
 * JMX registrations may be synchronous or asynchronous using a work-queue and worker thread.
 * The later enables higher throughput by avoiding the registration of very short lived instances.
 *
 * @author lorban, jkellerer
 */
public final class ManagementRegistrar {

    private final static Logger log = LoggerFactory.getLogger(ManagementRegistrar.class);
    private final static MBeanServer mbeanServer;

    static {
        boolean enableJmx = !TransactionManagerServices.getConfiguration().isDisableJmx();

        if (enableJmx) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        } else {
            mbeanServer = null;
        }
    }


    private final static Queue<ManagementCommand> commandQueue;

    static {
        Configuration configuration = TransactionManagerServices.getConfiguration();
        commandQueue = mbeanServer == null || configuration.isSynchronousJmxRegistration() ? null :
                new ConcurrentLinkedQueue<ManagementCommand>();


        // TODO: BEGIN - This part should use a system wide shared ScheduledExecutorService instead.
        if (commandQueue != null) {
            new Thread() {
                {
                    setName("bitronix-async-jmx-worker");
                    setDaemon(true);
                }

                @Override
                public void run() {
                    while (!isInterrupted()) {
                        try {
                            new ManagementCommandCallerTask().run();
                            sleep(250); // sampling interval
                        } catch (InterruptedException t) {
                            return;
                        } catch (Throwable t) {
                            log.error("An unexpected fatal error occurred in JMX asynchronous registration code.", t);
                        }
                    }
                }
            }.start();
        }
        // TODO: END
    }

    static {
        if (mbeanServer != null) {
            if (log.isDebugEnabled()) {
                log.debug("Enabled JMX with MBeanServer {}, registration is {}.",
                        mbeanServer, commandQueue == null ? "synchronous" : "asynchronous");
            }
        } else if (log.isDebugEnabled()) log.debug("JMX support is disabled.");
    }

    private ManagementRegistrar() {
    }

    /**
     * Replace characters considered illegal in a management object's name.
     *
     * @param name the name to work on.
     * @return a fully valid name where all invalid characters have been replaced with '_'.
     */
    public static String makeValidName(String name) {
        return name.replaceAll("[\\:\\,\\=,\\.]", "_");
    }

    /**
     * Register the specified management object.
     *
     * @param name the name of the object.
     * @param obj  the management object.
     */
    public static void register(String name, Object obj) {
        if (mbeanServer == null)
            return;

        executeCommand(new ManagementRegisterCommand(name, obj));
    }

    /**
     * Unregister the management object with the specified name.
     *
     * @param name the name of the object.
     */
    public static void unregister(String name) {
        if (mbeanServer == null)
            return;

        executeCommand(new ManagementUnregisterCommand(name));
    }

    private static void executeCommand(ManagementCommand command) {
        if (commandQueue == null)
            command.run();
        else
            commandQueue.add(command);
    }


    /**
     * Collects scheduled commands, removes
     */
    static class ManagementCommandCallerTask implements Runnable {
        @Override
        public void run() {
            final int initialCapacity = commandQueue.size() + 16;
            final Map<String, ManagementCommand> mappedCommands =
                    new LinkedHashMap<String, ManagementCommand>(initialCapacity);

            ManagementCommand command;
            while ((command = commandQueue.poll()) != null) {
                String name = command.getName();
                ManagementCommand previousCommand = mappedCommands.put(name, command);

                if (previousCommand instanceof ManagementRegisterCommand) {
                    // Avoid that we have unbound un-register commands in the work queue.
                    if (command instanceof ManagementUnregisterCommand)
                        mappedCommands.remove(name);
                }
            }

            if (!mappedCommands.isEmpty()) {
                for (ManagementCommand c : mappedCommands.values())
                    c.run();
            }
        }
    }


    /**
     * Registers the given instance within the JMX environment.
     */
    static class ManagementRegisterCommand extends ManagementCommand {

        final WeakReference<Object> instance;

        ManagementRegisterCommand(String name, Object instance) {
            super(name);
            // Using a WeakReference to avoid holding hard refs on instances that may already be obsolete.
            this.instance = new WeakReference<Object>(instance);
        }

        @Override
        protected void runCommand() throws Exception {
            Object object = instance.get();
            if (object != null)
                mbeanServer.registerMBean(object, new ObjectName(name));
        }
    }


    /**
     * Unregisters the given instance within the JMX environment.
     */
    static class ManagementUnregisterCommand extends ManagementCommand {
        ManagementUnregisterCommand(String name) {
            super(name);
        }

        @Override
        protected void runCommand() throws Exception {
            try {
                mbeanServer.unregisterMBean(new ObjectName(name));
            } catch (InstanceNotFoundException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to unregister the JMX instance of name '" +
                            name + "' as it doesn't exist.");
                }
            }
        }
    }


    /**
     * Base class for management related commands.
     */
    static abstract class ManagementCommand implements Runnable {

        final String name;

        protected ManagementCommand(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public final void run() {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Calling {} on object with name {}", getClass().getSimpleName(), name);
                }
                runCommand();
            } catch (Exception ex) {
                log.warn("Cannot execute " + getClass().getSimpleName() + " on object with name " + name, ex);
            }
        }

        protected abstract void runCommand() throws Exception;

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
