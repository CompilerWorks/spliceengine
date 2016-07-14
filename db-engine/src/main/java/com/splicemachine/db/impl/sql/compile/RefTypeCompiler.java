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

package com.splicemachine.db.impl.sql.compile;

import java.sql.Types;

import com.splicemachine.db.iapi.services.loader.ClassFactory;
import com.splicemachine.db.iapi.types.TypeId;
import com.splicemachine.db.iapi.types.DataTypeDescriptor;
import com.splicemachine.db.iapi.sql.compile.TypeCompiler;
import com.splicemachine.db.iapi.services.sanity.SanityManager;
import com.splicemachine.db.iapi.reference.ClassName;

/**
 * This class implements TypeCompiler for the SQL REF datatype.
 *
 */

public class RefTypeCompiler extends BaseTypeCompiler
{
	/** @see TypeCompiler#getCorrespondingPrimitiveTypeName */
	public String getCorrespondingPrimitiveTypeName()
	{
		if (SanityManager.DEBUG)
			SanityManager.THROWASSERT("getCorrespondingPrimitiveTypeName not implemented for SQLRef");
		return null;
	}

	/**
	 * @see TypeCompiler#getCastToCharWidth
	 */
	public int getCastToCharWidth(DataTypeDescriptor dts)
	{
		if (SanityManager.DEBUG)
			SanityManager.THROWASSERT( "getCastToCharWidth not implemented for SQLRef");
		return 0;
	}

	/** @see TypeCompiler#convertible */
	public boolean convertible(TypeId otherType, 
							   boolean forDataTypeFunction)
	{
        if (otherType.getJDBCTypeId() == Types.VARCHAR) {
            return true;
        }
		return false;
	}

	/**
	 * Tell whether this type is compatible with the given type.
	 *
	 * @see TypeCompiler#compatible */
	public boolean compatible(TypeId otherType)
	{
		return convertible(otherType,false);
	}

	/** @see TypeCompiler#storable */
	public boolean storable(TypeId otherType, ClassFactory cf)
	{
		return otherType.isRefTypeId();
	}

	/** @see TypeCompiler#interfaceName */
	public String interfaceName()
	{
		return ClassName.RefDataValue;
	}

	String nullMethodName()
	{
		return "getNullRef";
	}
}
