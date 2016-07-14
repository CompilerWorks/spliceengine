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

package com.splicemachine.db.impl.tools.ij;

import com.splicemachine.db.iapi.tools.i18n.LocalizedResource;

import java.sql.SQLException;
/**
 * Used for fatal IJ exceptions
 */

public class ijFatalException extends RuntimeException {

	private final static String FatalException = LocalizedResource.getMessage("IJ_FataExceTerm");
	private SQLException e;

	public ijFatalException() 
	{
		super(FatalException);
		e = null;
	}

	public ijFatalException(SQLException e) 
	{
		super(FatalException); 
		this.e = e;
	}

	public String getSQLState()
	{
		return e.getSQLState();
	}
	
	public String toString()
	{
		return LocalizedResource.getMessage("IJ_Fata01",e.getSQLState(),e.getMessage());
	}
}
