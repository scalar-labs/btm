/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

import bitronix.tm.TransactionManagerServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Smoke test for ManagementRegistrar.
 *
 * @author Juergen_Kellerer, 2011-08-24
 */
public class ManagementRegistrarTest {

    public static interface TestBeanMBean {
        String getName();
    }

    public static class TestBean implements TestBeanMBean {

        String name;

        public TestBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    final String objectName = "bitronix.somename:type=TestBean";
    final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    @Before
    public void assertJMXDefaultsAreAsyncAndEnabled() throws Exception {
        assertFalse(TransactionManagerServices.getConfiguration().isDisableJmx());
        assertFalse(TransactionManagerServices.getConfiguration().isSynchronousJmxRegistration());
    }

    @After
    public void tearDown() throws Exception {
        ManagementRegistrar.unregister(objectName);
        ManagementRegistrar.normalizeAndRunQueuedCommands();
    }

    @Test
    public void testCanRegister() throws Exception {
        final int iterations = 100000;
        final List<TestBean> beans = new ArrayList<TestBean>(iterations);

        for (int i = 0; i < iterations; i++) {
            if (i > 0)
                ManagementRegistrar.unregister(objectName);

            TestBean testBean = new TestBean("#" + i);
            beans.add(testBean); // holding a hard reference to ensure the instances are not GCed.
            ManagementRegistrar.register(objectName, testBean);
        }

        ManagementRegistrar.normalizeAndRunQueuedCommands();
        assertEquals(beans.get(beans.size() - 1).getName(), mBeanServer.getAttribute(new ObjectName(objectName), "Name"));
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testCanUnregister() throws Exception {
        TestBean testBean = new TestBean("1");
        ManagementRegistrar.register(objectName, testBean);
        ManagementRegistrar.unregister(objectName);
        ManagementRegistrar.normalizeAndRunQueuedCommands();

        mBeanServer.getAttribute(new ObjectName(objectName), "Name");
    }
}
