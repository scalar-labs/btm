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
package bitronix.tm.utils;

import bitronix.tm.TransactionManagerServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
