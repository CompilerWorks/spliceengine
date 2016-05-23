/*

   Derby - Class com.splicemachine.db.iapi.types.DataType

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.splicemachine.db.iapi.types;

import com.splicemachine.db.iapi.reference.SQLState;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.i18n.MessageService;
import com.splicemachine.db.iapi.services.sanity.SanityManager;
import org.joda.time.DateTime;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.RowId;
import java.util.Calendar;
import com.splicemachine.db.iapi.types.DataValueFactoryImpl.Format;

/**
 *
 * NullValueData provides a standard way for data types and number types
 * to handle null value and provides an optimization path to check
 * null and give the compiler the best chance to inline isNull()
 *
 * This requires users of this class update isNull on any change that
 * would impact wether the class was considered to be Null (even beyond
 * the class handle being null)
 *
 */
public abstract class NullValueData
{


	protected boolean isNull = true;

	public boolean isNull()
	{
		return isNull;
	}

	public final boolean getIsNull()
	{
		return isNull;
	}

	public final void setIsNull(boolean value)
	{
		isNull = value;
	}
}
