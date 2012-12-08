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
package bitronix.tm.resource;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.transaction.xa.XAResource;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the fundamental functionality of the newly implemented ResourceRegistrar.
 *
 * @author Juergen_Kellerer, 2011-08-24
 */
@Ignore
public class ResourceRegistrarTest {

    ExecutorService executorService;
    XAResourceProducer producer;

    private XAResourceProducer createMockProducer(String uniqueName) throws RecoveryException {
        XAResourceProducer producer;
        producer = mock(XAResourceProducer.class);
        when(producer.getUniqueName()).thenReturn(uniqueName);

        ResourceBean resourceBean = mock(ResourceBean.class);
        when(resourceBean.getUniqueName()).thenReturn(uniqueName);

        XAResourceHolder resourceHolder = mock(XAResourceHolder.class);
        when(resourceHolder.getResourceBean()).thenReturn(resourceBean);

        XAResource xaResource = mock(XAResource.class);
        when(resourceHolder.getXAResource()).thenReturn(xaResource);

        when(producer.startRecovery()).thenReturn(new XAResourceHolderState(resourceHolder, resourceBean));
        return producer;
    }

    private Future registerBlockingProducer(final XAResourceProducer producer, final CountDownLatch border) throws RecoveryException {
        final XAResourceHolderState resourceHolderState = producer.startRecovery();
        when(producer.startRecovery()).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                border.await();
                return resourceHolderState;
            }
        });

        return executorService.submit(new Callable<Object>() {
            public Object call() throws Exception {
                ResourceRegistrar.register(producer);
                return null;
            }
        });
    }

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool();
        producer = createMockProducer("xa-rp");
        ResourceRegistrar.register(producer);
        TransactionManagerServices.getTransactionManager();
    }

    @After
    public void tearDown() throws Exception {
        TransactionManagerServices.getTransactionManager().shutdown();
        executorService.shutdown();
        for (String name : ResourceRegistrar.getResourcesUniqueNames())
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
    }


    @Test
    public void testGet() throws Exception {
        assertSame(producer, ResourceRegistrar.get("xa-rp"));
        assertNull(ResourceRegistrar.get("non-existing"));
        assertNull(ResourceRegistrar.get(null));
    }

    @Test
    public void testGetDoesNotReturnUninitializedProducers() throws Exception {
        CountDownLatch border = new CountDownLatch(1);
        Future future = registerBlockingProducer(createMockProducer("uninitialized"), border);

        assertNull(ResourceRegistrar.get("uninitialized"));
        border.countDown();
        future.get();
        assertNotNull(ResourceRegistrar.get("uninitialized"));
    }

    @Test
    public void testGetResourcesUniqueNames() throws Exception {
        assertArrayEquals(new Object[]{"xa-rp"}, ResourceRegistrar.getResourcesUniqueNames().toArray());
    }

    @Test
    public void testGetResourcesUniqueNamesDoesNotReturnUninitializedProducers() throws Exception {
        CountDownLatch border = new CountDownLatch(1);
        Future future = registerBlockingProducer(createMockProducer("uninitialized"), border);

        assertArrayEquals(new Object[]{"xa-rp"}, ResourceRegistrar.getResourcesUniqueNames().toArray());

        border.countDown();
        future.get();
        assertArrayEquals(new Object[]{"xa-rp", "uninitialized"}, ResourceRegistrar.getResourcesUniqueNames().toArray());
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterSameRPTwice() throws Exception {
        ResourceRegistrar.register(createMockProducer("xa-rp"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotRegisterNonASCIIUniqueName() throws Exception {
        ResourceRegistrar.register(createMockProducer("äöü"));
    }

    @Test
    public void testNonRecoverableProducersAreNotRegistered() throws Exception {
        final XAResourceProducer producer = createMockProducer("non-recoverable");
        when(producer.startRecovery()).thenThrow(new RecoveryException("recovery not possible"));

        try {
            ResourceRegistrar.register(producer);
            fail("expecting RecoveryException");
        } catch (RecoveryException e) {
            assertNull(ResourceRegistrar.get("non-recoverable"));
        }
    }

    @Test
    public void testUnregister() throws Exception {
        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        ResourceRegistrar.unregister(createMockProducer("xa-rp"));
        assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());
    }

    @Test
    public void testFindXAResourceHolderDelegatesAndDoesNotCallUninitialized() throws Exception {
        final XAResource resource = mock(XAResource.class);
        final XAResourceProducer uninitializedProducer = createMockProducer("uninitialized");

        CountDownLatch border = new CountDownLatch(1);
        Future future = registerBlockingProducer(uninitializedProducer, border);

        ResourceRegistrar.findXAResourceHolder(resource);
        verify(producer, times(1)).findXAResourceHolder(resource);
        verify(uninitializedProducer, times(0)).findXAResourceHolder(resource);

        border.countDown();
        future.get();

        ResourceRegistrar.findXAResourceHolder(resource);
        verify(producer, times(2)).findXAResourceHolder(resource);
        verify(uninitializedProducer, times(1)).findXAResourceHolder(resource);
    }
}
