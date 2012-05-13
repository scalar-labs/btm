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
package bitronix.tm;

import junit.framework.TestCase;

import java.util.Properties;

/**
 *
 * @author lorban
 */
public class ConfigurationTest extends TestCase {

    public void testGetString() throws Exception {
        Properties props = new Properties();
        props.setProperty("1", "one");
        props.setProperty("2", "two");
        System.setProperty("3", "three");
        props.setProperty("4", "four");
        System.setProperty("4", "four-sys");
        props.setProperty("12", "${1} ${2}");
        props.setProperty("13", "${1} ${3}");
        props.setProperty("14", "${1} ${}");
        props.setProperty("15", "${1} ${tatata");
        props.setProperty("16", "${1} ${4}");
        props.setProperty("17", "x$");
        props.setProperty("18", "x${");

        assertEquals("one", Configuration.getString(props, "1", null));
        assertEquals("two", Configuration.getString(props, "2", null));
        assertEquals("three", Configuration.getString(props, "3", null));
        assertEquals("one two", Configuration.getString(props, "12", null));
        assertEquals("one three", Configuration.getString(props, "13", null));
        assertEquals("one four-sys", Configuration.getString(props, "16", null));

        try {
            Configuration.getString(props, "14", null);
            fail("expected IllegalArgumentException: property ref cannot refer to an empty name: ${}");
        } catch (IllegalArgumentException ex) {
            assertEquals("property ref cannot refer to an empty name: ${}", ex.getMessage());
        }

        try {
            Configuration.getString(props, "15", null);
            fail("expected IllegalArgumentException: unclosed property ref: ${tatata");
        } catch (IllegalArgumentException ex) {
            assertEquals("unclosed property ref: ${tatata", ex.getMessage());
        }

        assertEquals("x$", Configuration.getString(props, "17", null));

        try {
            Configuration.getString(props, "18", null);
            fail("expected IllegalArgumentException: unclosed property ref: ${");
        } catch (IllegalArgumentException ex) {
            assertEquals("unclosed property ref: ${", ex.getMessage());
        }
    }

    public void testGetIntBoolean() {
        Properties props = new Properties();
        props.setProperty("one", "1");
        props.setProperty("two", "2");
        System.setProperty("three", "3");
        System.setProperty("vrai", "true");
        props.setProperty("faux", "false");

        assertEquals(1, Configuration.getInt(props, "one", -1));
        assertEquals(2, Configuration.getInt(props, "two", -1));
        assertEquals(3, Configuration.getInt(props, "three", -1));
        assertEquals(10, Configuration.getInt(props, "ten", 10));

        assertEquals(true, Configuration.getBoolean(props, "vrai", false));
        assertEquals(false, Configuration.getBoolean(props, "faux", true));
        assertEquals(true, Configuration.getBoolean(props, "wrong", true));
    }

    public void testToString() {
        final String expectation = "a Configuration with [allowMultipleLrc=false, asynchronous2Pc=false," +
                " backgroundRecoveryInterval=1, backgroundRecoveryIntervalSeconds=60, currentNodeOnlyRecovery=true," +
                " debugZeroResourceTransaction=false, defaultTransactionTimeout=60, disableJmx=false," +
                " exceptionAnalyzer=null, filterLogStatus=false," +
                " forceBatchingEnabled=true, forcedWriteEnabled=true, gracefulShutdownInterval=10," +
                " jndiTransactionSynchronizationRegistryName=java:comp/TransactionSynchronizationRegistry," +
                " jndiUserTransactionName=java:comp/UserTransaction, journal=disk," +
                " logPart1Filename=target/btm1.tlog, logPart2Filename=target/btm2.tlog, maxLogSizeInMb=2," +
                " resourceConfigurationFilename=null, serverId=null, skipCorruptedLogs=false," +
                " warnAboutZeroResourceTransaction=true]";

        assertEquals(expectation, new Configuration().toString());
    }

}
