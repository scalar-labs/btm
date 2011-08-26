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

public  interface   SQLOutput
{
    public  void 	writeArray(Array x) throws  SQLException;
    public  void 	writeAsciiStream(java.io.InputStream x) throws  SQLException;
    public  void 	writeBigDecimal(java.math.BigDecimal x)   throws  SQLException;
    public  void 	writeBinaryStream(java.io.InputStream x)    throws  SQLException;
    public  void 	writeBlob(Blob x)   throws  SQLException;
    public  void 	writeBoolean(boolean x) throws  SQLException;
    public  void 	writeByte(byte x)   throws  SQLException;
    public  void 	writeBytes(byte[] x)    throws  SQLException;
    public  void 	writeCharacterStream(java.io.Reader x)  throws  SQLException;
    public  void 	writeClob(Clob x)   throws  SQLException;
    public  void 	writeDate(Date x)   throws  SQLException;
    public  void 	writeDouble(double x)   throws  SQLException;
    public  void 	writeFloat(float x) throws  SQLException;
    public  void 	writeInt(int x) throws  SQLException;
    public  void 	writeLong(long x)   throws  SQLException;
    public  void 	writeNClob(NClob x) throws  SQLException;
    public  void 	writeNString(String x)  throws  SQLException;
    public  void 	writeObject(SQLData x)  throws  SQLException;
    public  void 	writeRef(Ref x) throws  SQLException;
    public  void 	writeRowId(RowId x) throws  SQLException;
    public  void 	writeShort(short x) throws  SQLException;
    public  void 	writeSQLXML(SQLXML x)   throws  SQLException;
    public  void 	writeString(String x)   throws  SQLException;
    public  void 	writeStruct(Struct x)   throws  SQLException;
    public  void 	writeTime(Time x)   throws  SQLException;
    public  void 	writeTimestamp(Timestamp x) throws  SQLException;
    public  void 	writeURL(java.net.URL x) throws  SQLException;
}
