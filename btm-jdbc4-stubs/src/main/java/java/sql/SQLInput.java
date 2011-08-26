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

public  interface   SQLInput
{
    public  Array 	readArray() throws  SQLException;
    public  java.io.InputStream 	readAsciiStream()   throws  SQLException;
    public  java.math.BigDecimal 	readBigDecimal()    throws  SQLException;
    public  java.io.InputStream 	readBinaryStream()  throws  SQLException;
    public  Blob 	readBlob()  throws  SQLException;
    public  boolean 	readBoolean()   throws  SQLException;
    public  byte 	readByte()  throws  SQLException;
    public  byte[] 	readBytes() throws  SQLException;
    public  java.io.Reader 	readCharacterStream()   throws  SQLException;
    public  Clob 	readClob()  throws  SQLException;
    public  Date 	readDate()  throws  SQLException;
    public  double 	readDouble()    throws  SQLException;
    public  float 	readFloat() throws  SQLException;
    public  int 	readInt()   throws  SQLException;
    public  long 	readLong()  throws  SQLException;
    public  NClob 	readNClob() throws  SQLException;
    public  String 	readNString()   throws  SQLException;
    public  Object 	readObject()    throws  SQLException;
    public  Ref 	readRef()   throws  SQLException;
    public  RowId 	readRowId() throws  SQLException;
    public  short 	readShort() throws  SQLException;
    public  SQLXML 	readSQLXML()    throws  SQLException;
    public  String 	readString()    throws  SQLException;
    public  Time 	readTime()  throws  SQLException;
    public  Timestamp 	readTimestamp() throws  SQLException;
    public  java.net.URL 	readURL()   throws  SQLException;
    public  boolean 	wasNull()   throws  SQLException;
}
