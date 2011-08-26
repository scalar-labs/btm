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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public  interface   RowSetMetaData  extends ResultSetMetaData
{
    public  void 	setAutoIncrement(int columnIndex, boolean property) throws SQLException;
    public  void 	setCaseSensitive(int columnIndex, boolean property) throws SQLException;
    public  void 	setCatalogName(int columnIndex, String catalogName) throws SQLException;
    public  void 	setColumnCount(int columnCount) throws SQLException;
    public  void 	setColumnDisplaySize(int columnIndex, int size) throws SQLException;
    public  void 	setColumnLabel(int columnIndex, String label) throws SQLException;
    public  void 	setColumnName(int columnIndex, String columnName) throws SQLException;
    public  void 	setColumnType(int columnIndex, int SQLType) throws SQLException;
    public  void 	setColumnTypeName(int columnIndex, String typeName) throws SQLException;
    public  void 	setCurrency(int columnIndex, boolean property) throws SQLException;
    public  void 	setNullable(int columnIndex, int property) throws SQLException;
    public  void 	setPrecision(int columnIndex, int precision) throws SQLException;
    public  void 	setScale(int columnIndex, int scale) throws SQLException;
    public  void 	setSchemaName(int columnIndex, String schemaName) throws SQLException;
    public  void 	setSearchable(int columnIndex, boolean property) throws SQLException;
    public  void 	setSigned(int columnIndex, boolean property) throws SQLException;
    public  void 	setTableName(int columnIndex, String tableName) throws SQLException;
    
}

