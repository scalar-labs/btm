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
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public  class   SerialBlob implements  Blob, Cloneable, java.io.Serializable
{
    public  SerialBlob(Blob blob) throws SerialException {}
    public  SerialBlob(byte[] b) throws SerialException {}

    public  void 	free() throws SQLException {}
    public  InputStream 	getBinaryStream() throws SerialException { return null; }
    public  InputStream 	getBinaryStream(long pos, long length) throws SQLException { return null; }
    public  byte[] 	getBytes(long pos, int length) throws SerialException { return null; }
    public  long 	length() throws SerialException { return 0L; }
    public  long 	position(Blob pattern, long start) throws SerialException, SQLException { return 0L; }
    public  long 	position(byte[] pattern, long start) throws SerialException, SQLException { return 0L; }
    public  OutputStream 	setBinaryStream(long pos) throws SerialException, SQLException { return null; }
    public  int 	setBytes(long pos, byte[] bytes) throws SerialException, SQLException { return 0; }
    public  int 	setBytes(long pos, byte[] bytes, int offset, int length) throws SerialException, SQLException { return 0; }
    public  void 	truncate(long length) throws SerialException {}
}

