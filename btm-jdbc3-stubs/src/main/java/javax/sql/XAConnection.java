/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.sql;

import java.sql.SQLException;
import javax.transaction.xa.XAResource;

/**
 * An interface that is used to support the participation of a database
 * connection in distributed transactions. An XAConnection returns an XAResource
 * object which can be used by a Transaction Manager to manage the XAConnection
 * as part of a distributed transaction.
 * <p>
 * It is not intended that this interface is used directly by application code.
 * It is designed for use by system components which implement distributed
 * transaction support.
 */
public interface XAConnection extends PooledConnection {

    /**
     * Gets an XAResource which a transaction manager can use to manage the
     * participation of this XAConnection in a distributed transaction.
     * 
     * @return an XAResource object
     * @throws SQLException
     *             if an error occurs in accessing the database.
     */
    public XAResource getXAResource() throws SQLException;
}
