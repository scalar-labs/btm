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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

public  interface   CallableStatement   extends PreparedStatement
{
    public  Array 	getArray(int parameterIndex)    throws  SQLException;
    public  Array 	getArray(String parameterName)  throws  SQLException;
    public  BigDecimal 	getBigDecimal(int parameterIndex)   throws  SQLException;
    public  BigDecimal 	getBigDecimal(int parameterIndex, int scale)    throws  SQLException;
    public  BigDecimal 	getBigDecimal(String parameterName) throws  SQLException;
    public  Blob 	getBlob(int parameterIndex) throws  SQLException;
    public  Blob 	getBlob(String parameterName)   throws  SQLException;
    public  boolean 	getBoolean(int parameterIndex)  throws  SQLException;
    public  boolean 	getBoolean(String parameterName)    throws  SQLException;
    public  byte 	getByte(int parameterIndex) throws  SQLException;
    public  byte 	getByte(String parameterName)   throws  SQLException;
    public  byte[] 	getBytes(int parameterIndex)    throws  SQLException;
    public  byte[] 	getBytes(String parameterName)  throws  SQLException;
    public  Reader 	getCharacterStream(int parameterIndex)  throws  SQLException;
    public  Reader 	getCharacterStream(String parameterName)    throws  SQLException;
    public  Clob 	getClob(int parameterIndex) throws  SQLException;
    public  Clob 	getClob(String parameterName)   throws  SQLException;
    public  Date 	getDate(int parameterIndex) throws  SQLException;
    public  Date 	getDate(int parameterIndex, Calendar cal)   throws  SQLException;
    public  Date 	getDate(String parameterName)   throws  SQLException;
    public  Date 	getDate(String parameterName, Calendar cal) throws  SQLException;
    public  double 	getDouble(int parameterIndex)   throws  SQLException;
    public  double 	getDouble(String parameterName) throws  SQLException;
    public  float 	getFloat(int parameterIndex)    throws  SQLException;
    public  float 	getFloat(String parameterName)  throws  SQLException;
    public  int 	getInt(int parameterIndex)  throws  SQLException;
    public  int 	getInt(String parameterName)    throws  SQLException;
    public  long 	getLong(int parameterIndex) throws  SQLException;
    public  long 	getLong(String parameterName)   throws  SQLException;
    public  Reader 	getNCharacterStream(int parameterIndex) throws  SQLException;
    public  Reader 	getNCharacterStream(String parameterName)   throws  SQLException;
    public  NClob 	getNClob(int parameterIndex)    throws  SQLException;
    public  NClob 	getNClob(String parameterName)  throws  SQLException;
    public  String 	getNString(int parameterIndex)  throws  SQLException;
    public  String 	getNString(String parameterName)    throws  SQLException;
    public  Object 	getObject(int parameterIndex)   throws  SQLException;
    public  Object 	getObject(int parameterIndex, Map<String,Class<?>> map) throws  SQLException;
    public  Object 	getObject(String parameterName) throws  SQLException;
    public  Object 	getObject(String parameterName, Map<String,Class<?>> map)   throws  SQLException;
    public  Ref 	getRef(int parameterIndex)  throws  SQLException;
    public  Ref 	getRef(String parameterName)    throws  SQLException;
    public  RowId 	getRowId(int parameterIndex)    throws  SQLException;
    public  RowId 	getRowId(String parameterName)  throws  SQLException;
    public  short 	getShort(int parameterIndex)    throws  SQLException;
    public  short 	getShort(String parameterName)  throws  SQLException;
    public  SQLXML 	getSQLXML(int parameterIndex)   throws  SQLException;
    public  SQLXML 	getSQLXML(String parameterName) throws  SQLException;
    public  String 	getString(int parameterIndex)   throws  SQLException;
    public  String 	getString(String parameterName) throws  SQLException;
    public  Time 	getTime(int parameterIndex) throws  SQLException;
    public  Time 	getTime(int parameterIndex, Calendar cal)   throws  SQLException;
    public  Time 	getTime(String parameterName)   throws  SQLException;
    public  Time 	getTime(String parameterName, Calendar cal) throws  SQLException;
    public  Timestamp 	getTimestamp(int parameterIndex)    throws  SQLException;
    public  Timestamp 	getTimestamp(int parameterIndex, Calendar cal)  throws  SQLException;
    public  Timestamp 	getTimestamp(String parameterName)  throws  SQLException;
    public  Timestamp 	getTimestamp(String parameterName, Calendar cal)    throws  SQLException;
    public  URL 	getURL(int parameterIndex)  throws  SQLException;
    public  URL 	getURL(String parameterName)    throws  SQLException;
    public  void 	registerOutParameter(int parameterIndex, int sqlType)   throws  SQLException;
    public  void 	registerOutParameter(int parameterIndex, int sqlType, int scale)    throws  SQLException;
    public  void 	registerOutParameter(int parameterIndex, int sqlType, String typeName)  throws  SQLException;
    public  void 	registerOutParameter(String parameterName, int sqlType) throws  SQLException;
    public  void 	registerOutParameter(String parameterName, int sqlType, int scale)  throws  SQLException;
    public  void 	registerOutParameter(String parameterName, int sqlType, String typeName)    throws  SQLException;
    public  void 	setAsciiStream(String parameterName, InputStream x) throws  SQLException;
    public  void 	setAsciiStream(String parameterName, InputStream x, int length) throws  SQLException;
    public  void 	setAsciiStream(String parameterName, InputStream x, long length)    throws  SQLException;
    public  void 	setBigDecimal(String parameterName, BigDecimal x)   throws  SQLException;
    public  void 	setBinaryStream(String parameterName, InputStream x)    throws  SQLException;
    public  void 	setBinaryStream(String parameterName, InputStream x, int length)    throws  SQLException;
    public  void 	setBinaryStream(String parameterName, InputStream x, long length)   throws  SQLException;
    public  void 	setBlob(String parameterName, Blob x)   throws  SQLException;
    public  void 	setBlob(String parameterName, InputStream inputStream)  throws  SQLException;
    public  void 	setBlob(String parameterName, InputStream inputStream, long length) throws  SQLException;
    public  void 	setBoolean(String parameterName, boolean x) throws  SQLException;
    public  void 	setByte(String parameterName, byte x)   throws  SQLException;
    public  void 	setBytes(String parameterName, byte[] x)    throws  SQLException;
    public  void 	setCharacterStream(String parameterName, Reader reader) throws  SQLException;
    public  void 	setCharacterStream(String parameterName, Reader reader, int length) throws  SQLException;
    public  void 	setCharacterStream(String parameterName, Reader reader, long length)    throws  SQLException;
    public  void 	setClob(String parameterName, Clob x)   throws  SQLException;
    public  void 	setClob(String parameterName, Reader reader)    throws  SQLException;
    public  void 	setClob(String parameterName, Reader reader, long length)   throws  SQLException;
    public  void 	setDate(String parameterName, Date x)   throws  SQLException;
    public  void 	setDate(String parameterName, Date x, Calendar cal) throws  SQLException;
    public  void 	setDouble(String parameterName, double x)   throws  SQLException;
    public  void 	setFloat(String parameterName, float x) throws  SQLException;
    public  void 	setInt(String parameterName, int x) throws  SQLException;
    public  void 	setLong(String parameterName, long x)   throws  SQLException;
    public  void 	setNCharacterStream(String parameterName, Reader value) throws  SQLException;
    public  void 	setNCharacterStream(String parameterName, Reader value, long length)    throws  SQLException;
    public  void 	setNClob(String parameterName, NClob value) throws  SQLException;
    public  void 	setNClob(String parameterName, Reader reader)   throws  SQLException;
    public  void 	setNClob(String parameterName, Reader reader, long length)  throws  SQLException;
    public  void 	setNString(String parameterName, String value)  throws  SQLException;
    public  void 	setNull(String parameterName, int sqlType)  throws  SQLException;
    public  void 	setNull(String parameterName, int sqlType, String typeName) throws  SQLException;
    public  void 	setObject(String parameterName, Object x)   throws  SQLException;
    public  void 	setObject(String parameterName, Object x, int targetSqlType)    throws  SQLException;
    public  void 	setObject(String parameterName, Object x, int targetSqlType, int scale) throws  SQLException;
    public  void 	setRowId(String parameterName, RowId x) throws  SQLException;
    public  void 	setShort(String parameterName, short x) throws  SQLException;
    public  void 	setSQLXML(String parameterName, SQLXML xmlObject)   throws  SQLException;
    public  void 	setString(String parameterName, String x)   throws  SQLException;
    public  void 	setTime(String parameterName, Time x)   throws  SQLException;
    public  void 	setTime(String parameterName, Time x, Calendar cal) throws  SQLException;
    public  void 	setTimestamp(String parameterName, Timestamp x) throws  SQLException;
    public  void 	setTimestamp(String parameterName, Timestamp x, Calendar cal)   throws  SQLException;
    public  void 	setURL(String parameterName, URL val)   throws  SQLException;
    public  boolean 	wasNull()   throws  SQLException;
}
