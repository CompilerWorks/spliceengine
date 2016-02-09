package com.splicemachine.derby.impl.sql.execute.actions;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.Activation;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.dictionary.*;
import com.splicemachine.db.iapi.sql.execute.ConstantAction;
import com.splicemachine.db.iapi.store.access.TransactionController;
import com.splicemachine.db.impl.sql.execute.GenericPrivilegeInfo;
import com.splicemachine.db.impl.sql.execute.PrivilegeInfo;
import com.splicemachine.ddl.DDLMessage;
import com.splicemachine.derby.ddl.DDLUtils;
import com.splicemachine.derby.impl.store.access.SpliceTransactionManager;
import com.splicemachine.protobuf.ProtoUtil;

import java.util.List;

public class GrantRevokeConstantOperation implements ConstantAction {
	private boolean grant;
	private PrivilegeInfo privileges;
	private List grantees;
	public GrantRevokeConstantOperation( boolean grant, PrivilegeInfo privileges, List grantees) {
		this.grant = grant;
		this.privileges = privileges;
		this.grantees = grantees;
	}

	public	String	toString() {
		return grant ? "GRANT" : "REVOKE";
	}


	/**
	 *	This is the guts of the Execution-time logic for GRANT/REVOKE
	 *
	 *	See ConstantAction#executeConstantAction
	 *
	 * @exception StandardException		Thrown on failure
	 */
	public void executeConstantAction( Activation activation ) throws StandardException {
        LanguageConnectionContext lcc = activation.getLanguageConnectionContext();
        TransactionController tc = lcc.getTransactionExecute();

		List <PermissionsDescriptor> permissionsDescriptors = privileges.executeGrantRevoke( activation, grant, grantees);
		for (PermissionsDescriptor permissionsDescriptor : permissionsDescriptors) {
            DDLMessage.DDLChange ddlChange = createDDLChange(((SpliceTransactionManager) tc).getActiveStateTxn().getTxnId(), permissionsDescriptor);
            tc.prepareDataDictionaryChange(DDLUtils.notifyMetadataChange(ddlChange));
        }
	}

    private DDLMessage.DDLChange  createDDLChange(long txnId, PermissionsDescriptor permissionsDescriptor) {
        if (permissionsDescriptor instanceof TablePermsDescriptor) {
            TablePermsDescriptor tablePermsDescriptor = (TablePermsDescriptor) permissionsDescriptor;
            return ProtoUtil.createRevokeTablePrivilege(txnId, tablePermsDescriptor);
        }
        else if (permissionsDescriptor instanceof ColPermsDescriptor) {
            ColPermsDescriptor colPermsDescriptor = (ColPermsDescriptor) permissionsDescriptor;
            return ProtoUtil.createRevokeColumnPrivilege(txnId, colPermsDescriptor);
        }
        else if (permissionsDescriptor instanceof RoutinePermsDescriptor) {
            RoutinePermsDescriptor routinePermsDescriptor = (RoutinePermsDescriptor)permissionsDescriptor;
            return ProtoUtil.createRevokeRoutinePrivilege(txnId, routinePermsDescriptor);
        }
        else if (permissionsDescriptor instanceof PermDescriptor) {
            PermDescriptor permDescriptor = (PermDescriptor)permissionsDescriptor;
            boolean restrict = ((GenericPrivilegeInfo)privileges).isRestrict();
            return ProtoUtil.createRevokeGenericPrivilege(txnId, permDescriptor, restrict);
        }

        throw new RuntimeException("Unsupported permission descriptor type");
    }
}

