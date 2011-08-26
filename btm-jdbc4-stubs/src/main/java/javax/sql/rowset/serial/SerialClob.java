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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public  class   SerialClob implements Clob, Cloneable, java.io.Serializable
{
    public  SerialClob(char[] ch) throws SQLException, SerialException {}
    public  SerialClob(Clob clob) throws SQLException, SerialException {}

    public  void 	free() throws SQLException {}
    public  InputStream 	getAsciiStream() throws SQLException, SerialException { return null; }
    public  Reader 	getCharacterStream() throws SerialException { return null; }
    public  Reader 	getCharacterStream(long pos, long length) throws SQLException { return null; }
    public  String 	getSubString(long pos, int length) throws SerialException { return null; }
    public  long 	length() throws SerialException { return 0L; }
    public  long 	position(Clob searchStr, long start) throws SQLException, SerialException { return 0L; }
    public  long 	position(String searchStr, long start) throws SQLException, SerialException { return 0L; }
    public  OutputStream 	setAsciiStream(long pos) throws SQLException, SerialException  { return null; }
    public  Writer 	setCharacterStream(long pos) throws SQLException, SerialException  { return null; }
    public  int 	setString(long pos, String str) throws SerialException { return 0; }
    public  int 	setString(long pos, String str, int offset, int length) throws SerialException { return 0; }
    public  void 	truncate(long length) throws SerialException {}
}

