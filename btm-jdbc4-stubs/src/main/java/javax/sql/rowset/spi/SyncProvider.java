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

package javax.sql.rowset.spi;

import java.sql.SQLException;
import javax.sql.RowSet;
import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;

public  abstract    class   SyncProvider
{
    public static int DATASOURCE_DB_LOCK = 4;
    public static int DATASOURCE_NO_LOCK = 1;
    public static int DATASOURCE_ROW_LOCK = 2;
    public static int DATASOURCE_TABLE_LOCK = 3;
    public static int GRADE_CHECK_ALL_AT_COMMIT = 3;
    public static int GRADE_CHECK_MODIFIED_AT_COMMIT = 2;
    public static int GRADE_LOCK_WHEN_LOADED = 5;
    public static int GRADE_LOCK_WHEN_MODIFIED = 4;
    public static int GRADE_NONE = 1;
    public static int NONUPDATABLE_VIEW_SYNC = 6;
    public static int UPDATABLE_VIEW_SYNC = 5;

    public  SyncProvider() {}

    public  abstract  int 	getDataSourceLock() throws SyncProviderException;
    public  abstract  int 	getProviderGrade();
    public  abstract  String 	getProviderID();
    public  abstract  RowSetReader 	getRowSetReader();
    public  abstract  RowSetWriter 	getRowSetWriter();
    public  abstract  String 	getVendor();
    public  abstract  String 	getVersion();
    public  abstract  void 	setDataSourceLock(int datasource_lock) throws SyncProviderException;
    public  abstract  int 	supportsUpdatableView();
}

