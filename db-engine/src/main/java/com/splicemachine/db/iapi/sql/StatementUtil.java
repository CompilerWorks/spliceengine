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

package com.splicemachine.db.iapi.sql;

import com.splicemachine.db.iapi.services.i18n.MessageService;
import com.splicemachine.db.iapi.reference.SQLState;

/**
 * Utilities for dealing with statements.
 *
 */
public class StatementUtil
{
	private StatementUtil(){};	// Do not instantiate

	public static String typeName(int typeNumber)
	{
		String retval;

		switch (typeNumber)
		{
		  case StatementType.INSERT:
		  case StatementType.BULK_INSERT_REPLACE:
		  case StatementType.UPDATE:
		  case StatementType.DELETE:
		  case StatementType.ENABLED:
		  case StatementType.DISABLED:
			retval = TypeNames[typeNumber];
			break;

		  default:
			retval = MessageService.getTextMessage(SQLState.LANG_UNKNOWN);
			break;
		}

		return retval;
	}

	private static final String[] TypeNames = 
				{ 
					"",
					"INSERT",
					"INSERT",
					"UPDATE",
					"DELETE",
					"ENABLED",
					"DISABLED"
				};
}
