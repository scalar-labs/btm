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

import java.sql.SQLException;
import javax.sql.RowSetMetaData;

public  class   RowSetMetaDataImpl  implements  RowSetMetaData
{
    public RowSetMetaDataImpl() {}

    public  String 	getCatalogName(int columnIndex) throws SQLException { return null; }
    public  String 	getColumnClassName(int columnIndex) throws SQLException { return null; }
    public  int 	getColumnCount() throws SQLException { return 0; }
    public  int 	getColumnDisplaySize(int columnIndex) throws SQLException { return 0; }
    public  String 	getColumnLabel(int columnIndex) throws SQLException { return null; }
    public  String 	getColumnName(int columnIndex) throws SQLException { return null; }
    public  int 	getColumnType(int columnIndex) throws SQLException { return 0; }
    public  String 	getColumnTypeName(int columnIndex) throws SQLException { return null; }
    public  int 	getPrecision(int columnIndex) throws SQLException { return 0; }
    public  int 	getScale(int columnIndex) throws SQLException { return 0; }
    public  String 	getSchemaName(int columnIndex) throws SQLException { return null; }
    public  String 	getTableName(int columnIndex) throws SQLException { return null; }
    public  boolean 	isAutoIncrement(int columnIndex) throws SQLException { return false; }
    public  boolean 	isCaseSensitive(int columnIndex) throws SQLException { return false; }
    public  boolean 	isCurrency(int columnIndex) throws SQLException { return false; }
    public  boolean 	isDefinitelyWritable(int columnIndex) throws SQLException { return false; }
    public  int 	isNullable(int columnIndex) throws SQLException { return 0; }
    public  boolean 	isReadOnly(int columnIndex) throws SQLException { return false; }
    public  boolean 	isSearchable(int columnIndex) throws SQLException { return false; }
    public  boolean 	isSigned(int columnIndex) throws SQLException { return false; }
    public  boolean 	isWrapperFor(Class<?> interfaces) throws SQLException { return false; }
    public  boolean 	isWritable(int columnIndex) throws SQLException { return false; }
    public  void 	setAutoIncrement(int columnIndex, boolean property) throws SQLException {}
    public  void 	setCaseSensitive(int columnIndex, boolean property) throws SQLException {}
    public  void 	setCatalogName(int columnIndex, String catalogName) throws SQLException {}
    public  void 	setColumnCount(int columnCount) throws SQLException {}
    public  void 	setColumnDisplaySize(int columnIndex, int size) throws SQLException {}
    public  void 	setColumnLabel(int columnIndex, String label) throws SQLException {}
    public  void 	setColumnName(int columnIndex, String columnName) throws SQLException {}
    public  void 	setColumnType(int columnIndex, int SQLType) throws SQLException {}
    public  void 	setColumnTypeName(int columnIndex, String typeName) throws SQLException {}
    public  void 	setCurrency(int columnIndex, boolean property) throws SQLException {}
    public  void 	setNullable(int columnIndex, int property) throws SQLException {}
    public  void 	setPrecision(int columnIndex, int precision) throws SQLException {}
    public  void 	setScale(int columnIndex, int scale) throws SQLException {}
    public  void 	setSchemaName(int columnIndex, String schemaName) throws SQLException {}
    public  void 	setSearchable(int columnIndex, boolean property) throws SQLException {}
    public  void 	setSigned(int columnIndex, boolean property) throws SQLException {}
    public  void 	setTableName(int columnIndex, String tableName) throws SQLException {}
    public  <T> T unwrap(Class<T> iface)      throws SQLException { return null; }

}

