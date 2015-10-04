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
package bitronix.tm.resource.jdbc;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;

import java.sql.Connection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class ConnectionCustomizerTest {
    private ConnectionCustomizer customizer;

    @Before public void setup(){
        customizer = mock(ConnectionCustomizer.class);
    }

    @Test public void testCustomizerCall() throws Exception{
        String name = "pds";
        PoolingDataSource pds = new PoolingDataSource();
        pds.setUniqueName(name);
        pds.setXaDataSource(new MockitoXADataSource());
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(1);
        pds.setXaDataSource(new MockitoXADataSource());
        pds.addConnectionCustomizer(customizer);
        pds.init();

        Connection connection = pds.getConnection(); // onAcquire, onLease
        connection.close(); // onRelease

        connection = pds.getConnection(); // onLease
        connection.close(); // onRelease

        pds.close(); // onDestroy

        InOrder callOrder = inOrder(customizer);
        callOrder.verify(customizer).onAcquire(any(Connection.class), eq(name));
        callOrder.verify(customizer).onLease(any(Connection.class), eq(name));
        callOrder.verify(customizer).onRelease(any(Connection.class), eq(name));
        callOrder.verify(customizer).onLease(any(Connection.class), eq(name));
        callOrder.verify(customizer).onRelease(any(Connection.class), eq(name));
        callOrder.verify(customizer).onDestroy(any(Connection.class), eq(name));
    }
}
