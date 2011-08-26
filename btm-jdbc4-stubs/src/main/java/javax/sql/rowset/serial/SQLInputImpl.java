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
import java.sql.Struct;
import java.sql.ResultSet;
import java.sql.SQLData;
import java.sql.SQLInput;
import java.util.Calendar;
import java.util.Map;

public  class   SQLInputImpl implements  SQLInput
{
    public  SQLInputImpl(Object[] attributes, Map<String,Class<?>> map) throws SQLException {}

    public  Array 	readArray() throws SQLException { return null; }
    public  InputStream 	readAsciiStream() throws SQLException { return null; }
    public  BigDecimal 	readBigDecimal() throws SQLException { return null; }
    public  InputStream 	readBinaryStream() throws SQLException { return null; }
    public  Blob 	readBlob() throws SQLException { return null; }
    public  boolean 	readBoolean() throws SQLException { return false; }
    public  byte 	readByte() throws SQLException { return (byte) 0; }
    public  byte[] 	readBytes() throws SQLException { return null; }
    public  Reader 	readCharacterStream() throws SQLException { return null; }
    public  Clob 	readClob() throws SQLException { return null; }
    public  Date 	readDate() throws SQLException { return null; }
    public  double 	readDouble() throws SQLException { return 0.0; }
    public  float 	readFloat() throws SQLException { return (float) 0.0; }
    public  int 	readInt() throws SQLException { return 0; }
    public  long 	readLong() throws SQLException { return 0L; }
    public  NClob 	readNClob() throws SQLException { return null; }
    public  String 	readNString() throws SQLException { return null; }
    public  Object 	readObject() throws SQLException { return null; }
    public  Ref 	readRef() throws SQLException { return null; }
    public  RowId 	readRowId() throws SQLException { return null; }
    public  short 	readShort() throws SQLException { return (short) 0; }
    public  SQLXML 	readSQLXML() throws SQLException { return null; }
    public  String 	readString() throws SQLException { return null; }
    public  Time 	readTime() throws SQLException { return null; }
    public  Timestamp 	readTimestamp() throws SQLException { return null; }
    public  URL 	readURL() throws SQLException { return null; }
    public  boolean 	wasNull() throws SQLException { return false; }
}
