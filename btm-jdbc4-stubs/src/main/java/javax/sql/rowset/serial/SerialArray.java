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

package javax.sql.rowset.serial;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public  class   SerialArray implements  Array, Cloneable, java.io.Serializable
{
    public  SerialArray(Array array) throws SerialException, SQLException {}
    public  SerialArray(Array array, Map<String,Class<?>> map) throws SerialException, SQLException {}

    public  void 	free() throws SQLException {}
    public  Object 	getArray() throws SerialException { return null; }
    public  Object 	getArray(long index, int count) throws SerialException { return null; }
    public  Object 	getArray(long index, int count, Map<String,Class<?>> map) throws SerialException { return null; }
    public  Object 	getArray(Map<String,Class<?>> map) throws SerialException { return null; }
    public  int 	getBaseType() throws SerialException { return 0; }
    public  String 	getBaseTypeName() throws SerialException { return null; }
    public  ResultSet 	getResultSet() throws SerialException { return null; }
    public  ResultSet 	getResultSet(long index, int count) throws SerialException { return null; }
    public  ResultSet 	getResultSet(long index, int count, Map<String,Class<?>> map) throws SerialException { return null; }
    public  ResultSet 	getResultSet(Map<String,Class<?>> map) throws SerialException { return null; }
}

