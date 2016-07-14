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

package com.splicemachine.db.catalog.types;
import com.splicemachine.db.iapi.services.io.StoredFormatIds;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.sql.Types;

public class DecimalTypeIdImpl extends BaseTypeIdImpl
{
	/**
	 * Public niladic constructor. Needed for Serializable interface to work.
	 *
	 */
	public	DecimalTypeIdImpl() { super(); }

	/* this class is needed because writeexternal for this class stores
	   extra information; when the object is sent over the wire the niladic
	   constructor is first called and then we call the readExternal method. 
	   the readExternal needs to know the formatId atleast for decimal types
	   to read the extra information.
	*/
	public DecimalTypeIdImpl(boolean isNumeric) 
	{
		super(StoredFormatIds.DECIMAL_TYPE_ID_IMPL);
        if (isNumeric)
            setNumericType();
	}
	
	/**
	 * Read this object from a stream of stored objects.
	 *
	 * @param in read this.
	 *
	 * @exception IOException					thrown on error
	 * @exception ClassNotFoundException		thrown on error
	 */
	public void readExternal( ObjectInput in )
		 throws IOException, ClassNotFoundException
	{
		boolean isNumeric = in.readBoolean();

		super.readExternal(in);

		if (isNumeric)
		{
			setNumericType();
		}

	}

	/**
	 * Write this object to a stream of stored objects.
	 *
	 * @param out write bytes here.
	 *
	 * @exception IOException		thrown on error
	 */
	public void writeExternal( ObjectOutput out )
		 throws IOException
	{
		out.writeBoolean(getJDBCTypeId() == Types.NUMERIC);

		super.writeExternal(out);
	}

	private void setNumericType()
	{
		unqualifiedName = "NUMERIC";
		JDBCTypeId = Types.NUMERIC;
	}
}
