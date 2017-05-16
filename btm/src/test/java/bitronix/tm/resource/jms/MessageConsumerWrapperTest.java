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

package bitronix.tm.resource.jms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.jms.MessageConsumer;

import junit.framework.TestCase;

public class MessageConsumerWrapperTest extends TestCase {

    private final DualSessionWrapper dualSessionWrapper = mock(DualSessionWrapper.class);
    private final MessageConsumer messageConsumer = mock(MessageConsumer.class);
    private final PoolingConnectionFactory poolingConnectionFactory = mock(PoolingConnectionFactory.class);
    private final MessageConsumerWrapper messageConsumerWrapper = new MessageConsumerWrapper(messageConsumer, dualSessionWrapper, poolingConnectionFactory);

    public void testMessageConsumerClosed() throws Exception {
        messageConsumerWrapper.close();
        verify(messageConsumer, times(1)).close();
    }

    public void testGetMessageSelector() throws Exception {
        messageConsumerWrapper.getMessageSelector();
        verify(messageConsumer, times(1)).getMessageSelector();
    }

    public void testGetMessageListener() throws Exception {
        messageConsumerWrapper.getMessageListener();
        verify(messageConsumer, times(1)).getMessageListener();
    }
}