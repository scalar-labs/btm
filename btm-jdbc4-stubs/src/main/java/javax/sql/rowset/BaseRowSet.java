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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import javax.sql.RowSetListener;

public  abstract    class   BaseRowSet  implements  Cloneable, java.io.Serializable
{
    public static final int ASCII_STREAM_PARAM = 2;
    public static final int BINARY_STREAM_PARAM = 1;
    public static final int UNICODE_STREAM_PARAM = 0;

    protected java.io.InputStream binaryStream;
    protected java.io.InputStream unicodeStream;
    protected java.io.InputStream asciiStream;
    protected java.io.Reader charStream;

    public BaseRowSet()    {}

    public  void 	addRowSetListener(RowSetListener listener) {}
    public  void 	clearParameters() throws SQLException {}
    public  String 	getCommand() { return null; }
    public  int 	getConcurrency() throws SQLException { return 0; }
    public  String 	getDataSourceName() { return null; }
    public  boolean 	getEscapeProcessing() throws SQLException { return false; }
    public  int 	getFetchDirection() throws SQLException  { return 0; }
    public  int 	getFetchSize() throws SQLException  { return 0; }
    public  int 	getMaxFieldSize() throws SQLException  { return 0; }
    public  int 	getMaxRows() throws SQLException  { return 0; }
    public  Object[] 	getParams() throws SQLException { return null; }
    public  String 	getPassword() { return null; }
    public  int 	getQueryTimeout() throws SQLException  { return 0; }
    public  boolean 	getShowDeleted() throws SQLException { return false; }
    public  int 	getTransactionIsolation() { return 0; }
    public  int 	getType() throws SQLException  { return 0; }
    public  Map<String,Class<?>> 	getTypeMap() { return null; }
    public  String 	getUrl() throws SQLException { return null; }
    public  String 	getUsername() { return null; }
    protected  void 	initParams() {}
    public  boolean 	isReadOnly() { return false; }
    protected  void 	notifyCursorMoved() throws SQLException {}
    protected  void 	notifyRowChanged() throws SQLException {}
    protected  void 	notifyRowSetChanged() throws SQLException {}
    public  void 	removeRowSetListener(RowSetListener listener) {}
    public  void 	setArray(int parameterIndex, Array array) throws SQLException {}
    public  void 	setAsciiStream(int parameterIndex, InputStream x) throws SQLException {}
    public  void 	setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {}
    public  void 	setAsciiStream(String parameterName, InputStream x) throws SQLException {}
    public  void 	setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {}
    public  void 	setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {}
    public  void 	setBigDecimal(String parameterName, BigDecimal x) throws SQLException {}
    public  void 	setBinaryStream(int parameterIndex, InputStream x) throws SQLException {}
    public  void 	setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {}
    public  void 	setBinaryStream(String parameterName, InputStream x) throws SQLException {}
    public  void 	setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {}
    public  void 	setBlob(int parameterIndex, Blob x) throws SQLException {}
    public  void 	setBlob(int parameterIndex, InputStream inputStream) throws SQLException {}
    public  void 	setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {}
    public  void 	setBlob(String parameterName, Blob x) throws SQLException {}
    public  void 	setBlob(String parameterName, InputStream inputStream) throws SQLException {}
    public  void 	setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {}
    public  void 	setBoolean(int parameterIndex, boolean x) throws SQLException {}
    public  void 	setBoolean(String parameterName, boolean x) throws SQLException {}
    public  void 	setByte(int parameterIndex, byte x) throws SQLException {}
    public  void 	setByte(String parameterName, byte x) throws SQLException {}
    public  void 	setBytes(int parameterIndex, byte[] x) throws SQLException {}
    public  void 	setBytes(String parameterName, byte[] x) throws SQLException {}
    public  void 	setCharacterStream(int parameterIndex, Reader reader) throws SQLException {}
    public  void 	setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {}
    public  void 	setCharacterStream(String parameterName, Reader reader) throws SQLException {}
    public  void 	setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {}
    public  void 	setClob(int parameterIndex, Clob x) throws SQLException {}
    public  void 	setClob(int parameterIndex, Reader reader) throws SQLException {}
    public  void 	setClob(int parameterIndex, Reader reader, long length) throws SQLException {}
    public  void 	setClob(String parameterName, Clob x) throws SQLException {}
    public  void 	setClob(String parameterName, Reader reader) throws SQLException {}
    public  void 	setClob(String parameterName, Reader reader, long length) throws SQLException {}
    public  void 	setCommand(String cmd) throws SQLException {}
    public  void 	setConcurrency(int concurrency) throws SQLException {}
    public  void 	setDataSourceName(String name) throws SQLException {}
    public  void 	setDate(int parameterIndex, Date x) throws SQLException {}
    public  void 	setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {}
    public  void 	setDate(String parameterName, Date x) throws SQLException {}
    public  void 	setDate(String parameterName, Date x, Calendar cal) throws SQLException {}
    public  void 	setDouble(int parameterIndex, double x) throws SQLException {}
    public  void 	setDouble(String parameterName, double x) throws SQLException {}
    public  void 	setEscapeProcessing(boolean enable) throws SQLException {}
    public  void 	setFetchDirection(int direction) throws SQLException {}
    public  void 	setFetchSize(int rows) throws SQLException {}
    public  void 	setFloat(int parameterIndex, float x) throws SQLException {}
    public  void 	setFloat(String parameterName, float x) throws SQLException {}
    public  void 	setInt(int parameterIndex, int x) throws SQLException {}
    public  void 	setInt(String parameterName, int x) throws SQLException {}
    public  void 	setLong(int parameterIndex, long x) throws SQLException {}
    public  void 	setLong(String parameterName, long x) throws SQLException {}
    public  void 	setMaxFieldSize(int max) throws SQLException {}
    public  void 	setMaxRows(int max) throws SQLException {}
    public  void 	setNCharacterStream(int parameterIndex, Reader value) throws SQLException {}
    public  void 	setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {}
    public  void 	setNCharacterStream(String parameterName, Reader value) throws SQLException {}
    public  void 	setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {}
    public  void 	setNClob(int parameterIndex, NClob value) throws SQLException {}
    public  void 	setNClob(int parameterIndex, Reader reader) throws SQLException {}
    public  void 	setNClob(int parameterIndex, Reader reader, long length) throws SQLException {}
    public  void 	setNClob(String parameterName, NClob value) throws SQLException {}
    public  void 	setNClob(String parameterName, Reader reader) throws SQLException {}
    public  void 	setNClob(String parameterName, Reader reader, long length) throws SQLException {}
    public  void 	setNString(int parameterIndex, String value) throws SQLException {}
    public  void 	setNString(String parameterName, String value) throws SQLException {}
    public  void 	setNull(int parameterIndex, int sqlType) throws SQLException {}
    public  void 	setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {}
    public  void 	setNull(String parameterName, int sqlType) throws SQLException {}
    public  void 	setNull(String parameterName, int sqlType, String typeName) throws SQLException {}
    public  void 	setObject(int parameterIndex, Object x) throws SQLException {}
    public  void 	setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {}
    public  void 	setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {}
    public  void 	setObject(String parameterName, Object x) throws SQLException {}
    public  void 	setObject(String parameterName, Object x, int targetSqlType) throws SQLException {}
    public  void 	setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {}
    public  void 	setPassword(String pass) {}
    public  void 	setQueryTimeout(int seconds) throws SQLException {}
    public  void 	setReadOnly(boolean value) {}
    public  void 	setRef(int parameterIndex, Ref ref) throws SQLException {}
    public  void 	setRowId(int parameterIndex, RowId x) throws SQLException {}
    public  void 	setRowId(String parameterName, RowId x) throws SQLException {}
    public  void 	setShort(int parameterIndex, short x) throws SQLException {}
    public  void 	setShort(String parameterName, short x) throws SQLException {}
    public  void 	setShowDeleted(boolean value) throws SQLException {}
    public  void 	setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {}
    public  void 	setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {}
    public  void 	setString(int parameterIndex, String x) throws SQLException {}
    public  void 	setString(String parameterName, String x) throws SQLException {}
    public  void 	setTime(int parameterIndex, Time x) throws SQLException {}
    public  void 	setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {}
    public  void 	setTime(String parameterName, Time x) throws SQLException {}
    public  void 	setTime(String parameterName, Time x, Calendar cal) throws SQLException {}
    public  void 	setTimestamp(int parameterIndex, Timestamp x) throws SQLException {}
    public  void 	setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {}
    public  void 	setTimestamp(String parameterName, Timestamp x) throws SQLException {}
    public  void 	setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {}
    public  void 	setTransactionIsolation(int level) throws SQLException {}
    public  void 	setType(int type) throws SQLException {}
    public  void 	setTypeMap(Map<String,Class<?>> map) {}
    public  void 	setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {}
    public  void 	setURL(int parameterIndex, URL x) throws SQLException {}
    public  void 	setUrl(String url) throws SQLException {}
    public  void 	setUsername(String name) {}
}

