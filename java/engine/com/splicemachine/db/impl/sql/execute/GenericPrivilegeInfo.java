/*

   Derby - Class org.apache.derby.impl.sql.execute.GenericPrivilegeInfo

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

package com.splicemachine.db.impl.sql.execute;

import com.splicemachine.db.iapi.sql.Activation;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.store.access.TransactionController;
import com.splicemachine.db.iapi.sql.depend.DependencyManager;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.sql.dictionary.DataDescriptorGenerator;
import com.splicemachine.db.iapi.sql.dictionary.PermDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.SchemaDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.PrivilegedSQLObject;
import com.splicemachine.db.iapi.sql.dictionary.TupleDescriptor;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.catalog.UUID;

import java.util.Iterator;
import java.util.List;

public class GenericPrivilegeInfo extends PrivilegeInfo
{
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTANTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // STATE
    //
    ///////////////////////////////////////////////////////////////////////////////////

    private PrivilegedSQLObject _tupleDescriptor;
    private String              _privilege;
    private boolean             _restrict;

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Construct from the object which is protected by privileges.
     *
     * @param tupleDescriptor The object which is being protected
     * @param privilege Kind of privilege (e.g., PermDescriptor.USAGE_PRIV)
     * @param restrict True if this is a REVOKE RESTRICT action
     */
	public GenericPrivilegeInfo( PrivilegedSQLObject tupleDescriptor, String privilege, boolean restrict )
	{
		_tupleDescriptor = tupleDescriptor;
        _privilege = privilege;
        _restrict = restrict;
	}
	
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // PrivilegeInfo BEHAVIOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

	/**
	 *	This is the guts of the Execution-time logic for GRANT/REVOKE generic privileges.
	 *
	 * @param activation
	 * @param grant true if grant, false if revoke
	 * @param grantees a list of authorization ids (strings)
	 *
	 * @exception StandardException		Thrown on failure
	 */
	public void executeGrantRevoke( Activation activation,
									boolean grant,
									List grantees)
		throws StandardException
	{
		// Check that the current user has permission to grant the privileges.
		LanguageConnectionContext lcc = activation.getLanguageConnectionContext();
		DataDictionary dd = lcc.getDataDictionary();
        String currentUser = lcc.getCurrentUserId(activation);
		TransactionController tc = lcc.getTransactionExecute();
        SchemaDescriptor sd = _tupleDescriptor.getSchemaDescriptor();
        UUID objectID = _tupleDescriptor.getUUID();
        String objectTypeName = _tupleDescriptor.getObjectTypeName();

		// Check that the current user has permission to grant the privileges.
		checkOwnership( currentUser, (TupleDescriptor) _tupleDescriptor, sd, dd );
		
		DataDescriptorGenerator ddg = dd.getDataDescriptorGenerator();

		PermDescriptor permDesc = ddg.newPermDescriptor
            ( null, objectTypeName, objectID, _privilege, currentUser, null, false );

		dd.startWriting(lcc);
		for( Iterator itr = grantees.iterator(); itr.hasNext();)
		{
			// Keep track to see if any privileges are revoked by a revoke 
			// statement. If a privilege is not revoked, we need to raise a
			// warning.
			boolean privileges_revoked = false;
			String grantee = (String) itr.next();
			if (dd.addRemovePermissionsDescriptor( grant, permDesc, grantee, tc)) 
			{
                //
                // We fall in here if we are performing REVOKE.
                //
				privileges_revoked = true;	
                int invalidationType = _restrict ? DependencyManager.REVOKE_PRIVILEGE_RESTRICT : DependencyManager.REVOKE_PRIVILEGE;

				dd.getDependencyManager().invalidateFor( permDesc, invalidationType, lcc );

				// Now invalidate all GPSs refering to the object.
				dd.getDependencyManager().invalidateFor(_tupleDescriptor, invalidationType, lcc );
			}
			
			addWarningIfPrivilegeNotRevoked(activation, grant, privileges_revoked, grantee);
		}
	} // end of executeGrantRevoke


}
