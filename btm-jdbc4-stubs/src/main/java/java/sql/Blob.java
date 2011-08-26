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

public  interface   Blob
{
    public  void 	free()  throws  SQLException;
    public  java.io.InputStream 	getBinaryStream()   throws  SQLException;
    public  java.io.InputStream 	getBinaryStream(long pos, long length)  throws  SQLException;
    public  byte[] 	getBytes(long pos, int length)  throws  SQLException;
    public  long 	length()    throws  SQLException;
    public  long 	position(Blob pattern, long start)  throws  SQLException;
    public  long 	position(byte[] pattern, long start)    throws  SQLException;
    public  java.io.OutputStream 	setBinaryStream(long pos)   throws  SQLException;
    public  int 	setBytes(long pos, byte[] bytes)    throws  SQLException;
    public  int 	setBytes(long pos, byte[] bytes, int offset, int len)   throws  SQLException;
    public  void 	truncate(long len)  throws  SQLException;
    
}
