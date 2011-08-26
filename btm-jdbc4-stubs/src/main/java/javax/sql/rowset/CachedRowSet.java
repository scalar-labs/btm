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

package javax.sql.rowset;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.Collection;
import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetMetaData;
import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncResolver;

public  interface   CachedRowSet    extends Joinable, RowSet
{
    public static final boolean COMMIT_ON_ACCEPT_CHANGES = true;

    public  void 	acceptChanges() throws SQLException;
    public  void 	acceptChanges(Connection con) throws SQLException;
    public  boolean 	columnUpdated(int idx) throws SQLException;
    public  boolean 	columnUpdated(String columnName) throws SQLException;
    public  void 	commit() throws SQLException;
    public  CachedRowSet 	createCopy() throws SQLException;
    public  CachedRowSet 	createCopyNoConstraints() throws SQLException;
    public  CachedRowSet 	createCopySchema() throws SQLException;
    public  RowSet 	createShared() throws SQLException;
    public  void 	execute(Connection conn) throws SQLException;
    public  int[] 	getKeyColumns() throws SQLException;
    public  ResultSet 	getOriginal() throws SQLException;
    public  ResultSet 	getOriginalRow() throws SQLException;
    public  int 	getPageSize() throws SQLException;
    public  RowSetWarning 	getRowSetWarnings() throws SQLException;
    public  boolean 	getShowDeleted() throws SQLException;
    public  SyncProvider 	getSyncProvider() throws SQLException;
    public  String 	getTableName() throws SQLException;
    public  boolean 	nextPage() throws SQLException;
    public  void 	populate(ResultSet data) throws SQLException;
    public  void 	populate(ResultSet rs, int startRow) throws SQLException;
    public  boolean 	previousPage() throws SQLException;
    public  void 	release() throws SQLException;
    public  void 	restoreOriginal() throws SQLException;
    public  void 	rollback() throws SQLException;
    public  void 	rollback(Savepoint s) throws SQLException;
    public  void 	rowSetPopulated(RowSetEvent event, int numRows) throws SQLException;
    public  void 	setKeyColumns(int[] keys) throws SQLException;
    public  void 	setMetaData(RowSetMetaData md) throws SQLException;
    public  void 	setOriginalRow() throws SQLException;
    public  void 	setPageSize(int size) throws SQLException;
    public  void 	setShowDeleted(boolean b) throws SQLException;
    public  void 	setSyncProvider(String provider) throws SQLException;
    public  void 	setTableName(String tabName) throws SQLException;
    public  int 	size() throws SQLException;
    public  Collection<?> 	toCollection() throws SQLException;
    public  Collection<?> 	toCollection(int column) throws SQLException;
    public  Collection<?> 	toCollection(String column) throws SQLException;
    public  void 	undoDelete() throws SQLException;
    public  void 	undoInsert() throws SQLException;
    public  void 	undoUpdate() throws SQLException;

}

