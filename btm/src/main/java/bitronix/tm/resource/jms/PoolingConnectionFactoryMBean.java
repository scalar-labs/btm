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
package bitronix.tm.resource.jms;

/**
 *
 * @author lorban
 */
public interface PoolingConnectionFactoryMBean {

    public int getMinPoolSize();
    public int getMaxPoolSize();
    public long getInPoolSize();
    public long getTotalPoolSize();
    public boolean isFailed();
    public void reset() throws Exception;
    public boolean isDisabled();
    public void setDisabled(boolean disabled);
    
}
