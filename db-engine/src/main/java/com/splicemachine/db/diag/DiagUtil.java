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

package com.splicemachine.db.diag;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.reference.SQLState;
import com.splicemachine.db.iapi.services.context.ContextService;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;

/**
 * Utility methods for the package of diagnostic vtis.
 */
abstract    class   DiagUtil
{
    /**
     * Raise an exception if we are running with SQL authorization turned on
     * but the current user isn't the database owner. This method is used
     * to restrict access to VTIs which disclose sensitive information.
     * See DERBY-5395.
     */
    static void    checkAccess()   throws StandardException
    {
        LanguageConnectionContext lcc = (LanguageConnectionContext)
            ContextService.getContextOrNull(LanguageConnectionContext.CONTEXT_ID);
        DataDictionary  dd = lcc.getDataDictionary();

        if ( dd.usesSqlAuthorization() )
        {
            String  databaseOwner = dd.getAuthorizationDatabaseOwner();
            String  currentUser = lcc.getStatementContext().getSQLSessionContext().getCurrentUser();

            if ( !databaseOwner.equals( currentUser ) )
            {
                throw StandardException.newException( SQLState.DBO_ONLY );
            }
        }
    }

}


