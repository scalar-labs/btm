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

public  interface   ResultSet   extends Wrapper
{
    int CLOSE_CURSORS_AT_COMMIT = 2;
    int CONCUR_READ_ONLY = 1007;
    int CONCUR_UPDATABLE = 1008;
    int FETCH_FORWARD = 1000;
    int FETCH_REVERSE = 1001;
    int FETCH_UNKNOWN = 1002;
    int HOLD_CURSORS_OVER_COMMIT = 1;
    int TYPE_FORWARD_ONLY = 1003;
    int TYPE_SCROLL_INSENSITIVE = 1004;
    int TYPE_SCROLL_SENSITIVE = 1005;

    public  boolean 	absolute(int row)   throws  SQLException;
    public  void 	afterLast() throws  SQLException;
    public  void 	beforeFirst()   throws  SQLException;
    public  void 	cancelRowUpdates()  throws  SQLException;
    public  void 	clearWarnings() throws  SQLException;
    public  void 	close() throws  SQLException;
    public  void 	deleteRow() throws  SQLException;
    public  int 	findColumn(String columnLabel)  throws  SQLException;
    public  boolean 	first() throws  SQLException;
    public  Array 	getArray(int columnIndex)   throws  SQLException;
    public  Array 	getArray(String columnLabel)    throws  SQLException;
    public  InputStream 	getAsciiStream(int columnIndex) throws  SQLException;
    public  InputStream 	getAsciiStream(String columnLabel)  throws  SQLException;
    public  BigDecimal 	getBigDecimal(int columnIndex)  throws  SQLException;
    public  BigDecimal 	getBigDecimal(int columnIndex, int scale)   throws  SQLException;
    public  BigDecimal 	getBigDecimal(String columnLabel)   throws  SQLException;
    public  BigDecimal 	getBigDecimal(String columnLabel, int scale)    throws  SQLException;
    public  InputStream 	getBinaryStream(int columnIndex)    throws  SQLException;
    public  InputStream 	getBinaryStream(String columnLabel) throws  SQLException;
    public  Blob 	getBlob(int columnIndex)    throws  SQLException;
    public  Blob 	getBlob(String columnLabel) throws  SQLException;
    public  boolean 	getBoolean(int columnIndex) throws  SQLException;
    public  boolean 	getBoolean(String columnLabel)  throws  SQLException;
    public  byte 	getByte(int columnIndex)    throws  SQLException;
    public  byte 	getByte(String columnLabel) throws  SQLException;
    public  byte[] 	getBytes(int columnIndex)   throws  SQLException;
    public  byte[] 	getBytes(String columnLabel)    throws  SQLException;
    public  Reader 	getCharacterStream(int columnIndex) throws  SQLException;
    public  Reader 	getCharacterStream(String columnLabel)  throws  SQLException;
    public  Clob 	getClob(int columnIndex)    throws  SQLException;
    public  Clob 	getClob(String columnLabel) throws  SQLException;
    public  int 	getConcurrency()    throws  SQLException;
    public  String 	getCursorName() throws  SQLException;
    public  Date 	getDate(int columnIndex)    throws  SQLException;
    public  Date 	getDate(int columnIndex, Calendar cal)  throws  SQLException;
    public  Date 	getDate(String columnLabel) throws  SQLException;
    public  Date 	getDate(String columnLabel, Calendar cal)   throws  SQLException;
    public  double 	getDouble(int columnIndex)  throws  SQLException;
    public  double 	getDouble(String columnLabel)   throws  SQLException;
    public  int 	getFetchDirection() throws  SQLException;
    public  int 	getFetchSize()  throws  SQLException;
    public  float 	getFloat(int columnIndex)   throws  SQLException;
    public  float 	getFloat(String columnLabel)    throws  SQLException;
    public  int 	getHoldability()    throws  SQLException;
    public  int 	getInt(int columnIndex) throws  SQLException;
    public  int 	getInt(String columnLabel)  throws  SQLException;
    public  long 	getLong(int columnIndex)    throws  SQLException;
    public  long 	getLong(String columnLabel) throws  SQLException;
    public  ResultSetMetaData 	getMetaData()   throws  SQLException;
    public  Reader 	getNCharacterStream(int columnIndex)    throws  SQLException;
    public  Reader 	getNCharacterStream(String columnLabel) throws  SQLException;
    public  NClob 	getNClob(int columnIndex)   throws  SQLException;
    public  NClob 	getNClob(String columnLabel)    throws  SQLException;
    public  String 	getNString(int columnIndex) throws  SQLException;
    public  String 	getNString(String columnLabel)  throws  SQLException;
    public  Object 	getObject(int columnIndex)  throws  SQLException;
    public  Object 	getObject(int columnIndex, Map<String,Class<?>> map)    throws  SQLException;
    public  Object 	getObject(String columnLabel)   throws  SQLException;
    public  Object 	getObject(String columnLabel, Map<String,Class<?>> map) throws  SQLException;
    public  Ref 	getRef(int columnIndex) throws  SQLException;
    public  Ref 	getRef(String columnLabel)  throws  SQLException;
    public  int 	getRow()    throws  SQLException;
    public  RowId 	getRowId(int columnIndex)   throws  SQLException;
    public  RowId 	getRowId(String columnLabel)    throws  SQLException;
    public  short 	getShort(int columnIndex)   throws  SQLException;
    public  short 	getShort(String columnLabel)    throws  SQLException;
    public  SQLXML 	getSQLXML(int columnIndex)  throws  SQLException;
    public  SQLXML 	getSQLXML(String columnLabel)   throws  SQLException;
    public  Statement 	getStatement()  throws  SQLException;
    public  String 	getString(int columnIndex)  throws  SQLException;
    public  String 	getString(String columnLabel)   throws  SQLException;
    public  Time 	getTime(int columnIndex)    throws  SQLException;
    public  Time 	getTime(int columnIndex, Calendar cal)  throws  SQLException;
    public  Time 	getTime(String columnLabel) throws  SQLException;
    public  Time 	getTime(String columnLabel, Calendar cal)   throws  SQLException;
    public  Timestamp 	getTimestamp(int columnIndex)   throws  SQLException;
    public  Timestamp 	getTimestamp(int columnIndex, Calendar cal) throws  SQLException;
    public  Timestamp 	getTimestamp(String columnLabel)    throws  SQLException;
    public  Timestamp 	getTimestamp(String columnLabel, Calendar cal)  throws  SQLException;
    public  int 	getType()   throws  SQLException;
    public  InputStream 	getUnicodeStream(int columnIndex)   throws  SQLException;
    public  InputStream 	getUnicodeStream(String columnLabel)    throws  SQLException;
    public  URL 	getURL(int columnIndex) throws  SQLException;
    public  URL 	getURL(String columnLabel)  throws  SQLException;
    public  SQLWarning 	getWarnings()   throws  SQLException;
    public  void 	insertRow() throws  SQLException;
    public  boolean 	isAfterLast()   throws  SQLException;
    public  boolean 	isBeforeFirst() throws  SQLException;
    public  boolean 	isClosed()  throws  SQLException;
    public  boolean 	isFirst()   throws  SQLException;
    public  boolean 	isLast()    throws  SQLException;
    public  boolean 	last()  throws  SQLException;
    public  void 	moveToCurrentRow()  throws  SQLException;
    public  void 	moveToInsertRow()   throws  SQLException;
    public  boolean 	next()  throws  SQLException;
    public  boolean 	previous()  throws  SQLException;
    public  void 	refreshRow()    throws  SQLException;
    public  boolean 	relative(int rows)  throws  SQLException;
    public  boolean 	rowDeleted()    throws  SQLException;
    public  boolean 	rowInserted()   throws  SQLException;
    public  boolean 	rowUpdated()    throws  SQLException;
    public  void 	setFetchDirection(int direction)    throws  SQLException;
    public  void 	setFetchSize(int rows)  throws  SQLException;
    public  void 	updateArray(int columnIndex, Array x)   throws  SQLException;
    public  void 	updateArray(String columnLabel, Array x)    throws  SQLException;
    public  void 	updateAsciiStream(int columnIndex, InputStream x)   throws  SQLException;
    public  void 	updateAsciiStream(int columnIndex, InputStream x, int length)   throws  SQLException;
    public  void 	updateAsciiStream(int columnIndex, InputStream x, long length)  throws  SQLException;
    public  void 	updateAsciiStream(String columnLabel, InputStream x)    throws  SQLException;
    public  void 	updateAsciiStream(String columnLabel, InputStream x, int length)    throws  SQLException;
    public  void 	updateAsciiStream(String columnLabel, InputStream x, long length)   throws  SQLException;
    public  void 	updateBigDecimal(int columnIndex, BigDecimal x) throws  SQLException;
    public  void 	updateBigDecimal(String columnLabel, BigDecimal x)  throws  SQLException;
    public  void 	updateBinaryStream(int columnIndex, InputStream x)  throws  SQLException;
    public  void 	updateBinaryStream(int columnIndex, InputStream x, int length)  throws  SQLException;
    public  void 	updateBinaryStream(int columnIndex, InputStream x, long length) throws  SQLException;
    public  void 	updateBinaryStream(String columnLabel, InputStream x)   throws  SQLException;
    public  void 	updateBinaryStream(String columnLabel, InputStream x, int length)   throws  SQLException;
    public  void 	updateBinaryStream(String columnLabel, InputStream x, long length)  throws  SQLException;
    public  void 	updateBlob(int columnIndex, Blob x) throws  SQLException;
    public  void 	updateBlob(int columnIndex, InputStream inputStream)    throws  SQLException;
    public  void 	updateBlob(int columnIndex, InputStream inputStream, long length)   throws  SQLException;
    public  void 	updateBlob(String columnLabel, Blob x)  throws  SQLException;
    public  void 	updateBlob(String columnLabel, InputStream inputStream) throws  SQLException;
    public  void 	updateBlob(String columnLabel, InputStream inputStream, long length)    throws  SQLException;
    public  void 	updateBoolean(int columnIndex, boolean x)   throws  SQLException;
    public  void 	updateBoolean(String columnLabel, boolean x)    throws  SQLException;
    public  void 	updateByte(int columnIndex, byte x) throws  SQLException;
    public  void 	updateByte(String columnLabel, byte x)  throws  SQLException;
    public  void 	updateBytes(int columnIndex, byte[] x)  throws  SQLException;
    public  void 	updateBytes(String columnLabel, byte[] x)   throws  SQLException;
    public  void 	updateCharacterStream(int columnIndex, Reader x)    throws  SQLException;
    public  void 	updateCharacterStream(int columnIndex, Reader x, int length)    throws  SQLException;
    public  void 	updateCharacterStream(int columnIndex, Reader x, long length)   throws  SQLException;
    public  void 	updateCharacterStream(String columnLabel, Reader reader)    throws  SQLException;
    public  void 	updateCharacterStream(String columnLabel, Reader reader, int length)    throws  SQLException;
    public  void 	updateCharacterStream(String columnLabel, Reader reader, long length)   throws  SQLException;
    public  void 	updateClob(int columnIndex, Clob x) throws  SQLException;
    public  void 	updateClob(int columnIndex, Reader reader)  throws  SQLException;
    public  void 	updateClob(int columnIndex, Reader reader, long length) throws  SQLException;
    public  void 	updateClob(String columnLabel, Clob x)  throws  SQLException;
    public  void 	updateClob(String columnLabel, Reader reader)   throws  SQLException;
    public  void 	updateClob(String columnLabel, Reader reader, long length)  throws  SQLException;
    public  void 	updateDate(int columnIndex, Date x) throws  SQLException;
    public  void 	updateDate(String columnLabel, Date x)  throws  SQLException;
    public  void 	updateDouble(int columnIndex, double x) throws  SQLException;
    public  void 	updateDouble(String columnLabel, double x)  throws  SQLException;
    public  void 	updateFloat(int columnIndex, float x)   throws  SQLException;
    public  void 	updateFloat(String columnLabel, float x)    throws  SQLException;
    public  void 	updateInt(int columnIndex, int x)   throws  SQLException;
    public  void 	updateInt(String columnLabel, int x)    throws  SQLException;
    public  void 	updateLong(int columnIndex, long x) throws  SQLException;
    public  void 	updateLong(String columnLabel, long x)  throws  SQLException;
    public  void 	updateNCharacterStream(int columnIndex, Reader x)   throws  SQLException;
    public  void 	updateNCharacterStream(int columnIndex, Reader x, long length)  throws  SQLException;
    public  void 	updateNCharacterStream(String columnLabel, Reader reader)   throws  SQLException;
    public  void 	updateNCharacterStream(String columnLabel, Reader reader, long length)  throws  SQLException;
    public  void 	updateNClob(int columnIndex, NClob nClob)   throws  SQLException;
    public  void 	updateNClob(int columnIndex, Reader reader) throws  SQLException;
    public  void 	updateNClob(int columnIndex, Reader reader, long length)    throws  SQLException;
    public  void 	updateNClob(String columnLabel, NClob nClob)    throws  SQLException;
    public  void 	updateNClob(String columnLabel, Reader reader)  throws  SQLException;
    public  void 	updateNClob(String columnLabel, Reader reader, long length) throws  SQLException;
    public  void 	updateNString(int columnIndex, String nString)  throws  SQLException;
    public  void 	updateNString(String columnLabel, String nString)   throws  SQLException;
    public  void 	updateNull(int columnIndex) throws  SQLException;
    public  void 	updateNull(String columnLabel)  throws  SQLException;
    public  void 	updateObject(int columnIndex, Object x) throws  SQLException;
    public  void 	updateObject(int columnIndex, Object x, int scaleOrLength)  throws  SQLException;
    public  void 	updateObject(String columnLabel, Object x)  throws  SQLException;
    public  void 	updateObject(String columnLabel, Object x, int scaleOrLength)   throws  SQLException;
    public  void 	updateRef(int columnIndex, Ref x)   throws  SQLException;
    public  void 	updateRef(String columnLabel, Ref x)    throws  SQLException;
    public  void 	updateRow() throws  SQLException;
    public  void 	updateRowId(int columnIndex, RowId x)   throws  SQLException;
    public  void 	updateRowId(String columnLabel, RowId x)    throws  SQLException;
    public  void 	updateShort(int columnIndex, short x)   throws  SQLException;
    public  void 	updateShort(String columnLabel, short x)    throws  SQLException;
    public  void 	updateSQLXML(int columnIndex, SQLXML xmlObject) throws  SQLException;
    public  void 	updateSQLXML(String columnLabel, SQLXML xmlObject)  throws  SQLException;
    public  void 	updateString(int columnIndex, String x) throws  SQLException;
    public  void 	updateString(String columnLabel, String x)  throws  SQLException;
    public  void 	updateTime(int columnIndex, Time x) throws  SQLException;
    public  void 	updateTime(String columnLabel, Time x)  throws  SQLException;
    public  void 	updateTimestamp(int columnIndex, Timestamp x)   throws  SQLException;
    public  void 	updateTimestamp(String columnLabel, Timestamp x)    throws  SQLException;
    public  boolean 	wasNull()   throws  SQLException;
}
