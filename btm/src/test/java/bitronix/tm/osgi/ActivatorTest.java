/*
 * Copyright (C) 2016 Roland Hauser, <sourcepond@gmail.com>
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
package bitronix.tm.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

/**
 * @author rolandhauser
 *
 */
public class ActivatorTest {
	private static File CONFIG_AREA = new File("src/test/resources");
	private final BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
	private final ServiceRegistration tmRegistration = mock(ServiceRegistration.class);
	private final ServiceRegistration utRegistration = mock(ServiceRegistration.class);
	private final BundleContext context = mock(BundleContext.class);
	private final Activator activator = new Activator();

	@Before
	public void setup() {
		when(context.getProperty("osgi.configuration.area")).thenReturn(CONFIG_AREA.toURI().toString());
		when(context.registerService(TransactionManager.class.getName(), tm, null)).thenReturn(tmRegistration);
		when(context.registerService(UserTransaction.class.getName(), tm, null)).thenReturn(utRegistration);
	}

	@After
	public void tearDown() {
		tm.shutdown();
		System.getProperties().remove("bitronix.tm.osgi.disableActivator");
	}
	
	@Test
	public void startStop() throws Exception {
		activator.start(context);
		assertEquals(new File(CONFIG_AREA, "bitronix-default-config.properties").getAbsolutePath(),
				System.getProperty("bitronix.tm.configuration"));
		assertNull(System.getProperty("bitronix.tm.resource.configuration"));
		verify(context).registerService(TransactionManager.class.getName(), tm, null);
		verify(context).registerService(UserTransaction.class.getName(), tm, null);
		
		activator.stop(context);
		verify(tmRegistration).unregister();
		verify(utRegistration).unregister();
	}
	
	@Test
	public void startStopWhenActivatorIsDisabled() throws Exception {
		System.setProperty("bitronix.tm.osgi.disableActivator", String.valueOf("true"));
		activator.start(context);
		
		verify(context, never()).registerService(TransactionManager.class.getName(), tm, null);
		verify(context, never()).registerService(UserTransaction.class.getName(), tm, null);
		
		activator.stop(context);
		verify(tmRegistration, never()).unregister();
		verify(utRegistration, never()).unregister();
	}
}
