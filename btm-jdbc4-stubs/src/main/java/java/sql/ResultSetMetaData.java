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

package java.sql;

public  interface   ResultSetMetaData   extends Wrapper
{
    int columnNoNulls = 0;
    int columnNullable = 1;
    int columnNullableUnknown = 2;

    public  String 	getCatalogName(int column)  throws  SQLException;
    public  String 	getColumnClassName(int column)  throws  SQLException;
    public  int 	getColumnCount()    throws  SQLException;
    public  int 	getColumnDisplaySize(int column)    throws  SQLException;
    public  String 	getColumnLabel(int column)  throws  SQLException;
    public  String 	getColumnName(int column)   throws  SQLException;
    public  int 	getColumnType(int column)   throws  SQLException;
    public  String 	getColumnTypeName(int column)   throws  SQLException;
    public  int 	getPrecision(int column)    throws  SQLException;
    public  int 	getScale(int column)    throws  SQLException;
    public  String 	getSchemaName(int column)   throws  SQLException;
    public  String 	getTableName(int column)    throws  SQLException;
    public  boolean 	isAutoIncrement(int column) throws  SQLException;
    public  boolean 	isCaseSensitive(int column) throws  SQLException;
    public  boolean 	isCurrency(int column)  throws  SQLException;
    public  boolean 	isDefinitelyWritable(int column)    throws  SQLException;
    public  int 	isNullable(int column)  throws  SQLException;
    public  boolean 	isReadOnly(int column)  throws  SQLException;
    public  boolean 	isSearchable(int column)    throws  SQLException;
    public  boolean 	isSigned(int column)    throws  SQLException;
    public  boolean 	isWritable(int column)  throws  SQLException;
     
}
