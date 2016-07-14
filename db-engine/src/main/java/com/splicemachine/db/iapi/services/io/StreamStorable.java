/*
 * Apache Derby is a subproject of the Apache DB project, and is licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use these files
 * except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Splice Machine, Inc. has modified this file.
 *
 * All Splice Machine modifications are Copyright 2012 - 2016 Splice Machine, Inc.,
 * and are licensed to you under the License; you may not use this file except in
 * compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.splicemachine.db.iapi.services.io;
import com.splicemachine.db.iapi.error.StandardException;
import java.io.InputStream;

/**
 * Streaming interface for a data value. The format of
 * the stream is data type dependent and represents the
 * on-disk format of the value. That is it is different
 * to the value an application will see through JDBC
 * with methods like getBinaryStream and getAsciiStream.
 * 
 * <BR>
 * If the value is NULL (DataValueDescriptor.isNull returns
 * true then these methods should not be used to get the value.

  @see Formatable
 */
public interface StreamStorable
{
	/**
	  Return the on-disk stream state of the object.
	  
	**/
	public InputStream returnStream();

	/**
	  sets the on-disk stream state for the object.
	**/
	public void setStream(InputStream newStream);

	/**
     * Set the value by reading the stream and
     * converting it to an object form.
     * 
		@exception StandardException on error
	**/
	public void loadStream() throws StandardException;
}
