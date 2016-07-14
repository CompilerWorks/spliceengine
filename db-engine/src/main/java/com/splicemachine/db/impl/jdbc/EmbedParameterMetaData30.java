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

package com.splicemachine.db.impl.jdbc;

import com.splicemachine.db.iapi.sql.ParameterValueSet;
import com.splicemachine.db.iapi.types.DataTypeDescriptor;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

/**
 * This class implements the ParameterMetaData interface from JDBC3.0
 * It provides the parameter meta data for callable & prepared statements
 * But note that the bulk of it resides in its parent class.  The reason is
 * we want to provide the functionality to the JDKs before JDBC3.0.
 *
  <P><B>Supports</B>
   <UL>
   <LI> JDBC 3.0 - java.sql.ParameterMetaData introduced in JDBC3
   </UL>

 * @see java.sql.ParameterMetaData
 *
 */
class EmbedParameterMetaData30 extends EmbedParameterSetMetaData
    implements ParameterMetaData {

	//////////////////////////////////////////////////////////////
	//
	// CONSTRUCTORS
	//
	//////////////////////////////////////////////////////////////
    EmbedParameterMetaData30(ParameterValueSet pvs, DataTypeDescriptor[] types)  {
		super(pvs, types);
    }

//    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException{
        throw new UnsupportedOperationException();
    }

//    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException{
        throw new UnsupportedOperationException();
    }
}

