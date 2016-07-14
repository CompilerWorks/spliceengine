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

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.reference.SQLState;
import com.splicemachine.db.iapi.sql.compile.CompilerContext;
import com.splicemachine.db.iapi.sql.dictionary.ConglomerateDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.ConstraintDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.sql.dictionary.SchemaDescriptor;
import com.splicemachine.db.iapi.sql.dictionary.TableDescriptor;
import com.splicemachine.db.iapi.sql.execute.ConstantAction;

/**
 * A DropIndexNode is the root of a QueryTree that represents a DROP INDEX
 * statement.
 *
 */

public class DropIndexNode extends DDLStatementNode
{
	private ConglomerateDescriptor	cd;
	private TableDescriptor			td;

	public String statementToString()
	{
		return "DROP INDEX";
	}

	/**
	 * Bind this DropIndexNode.  This means looking up the index,
	 * verifying it exists and getting the conglomerate number.
	 *
	 *
	 * @exception StandardException		Thrown on error
	 */
	public void bindStatement() throws StandardException
	{
		CompilerContext			cc = getCompilerContext();
		DataDictionary			dd = getDataDictionary();
		SchemaDescriptor		sd;

		sd = getSchemaDescriptor();

		if (sd.getUUID() != null) 
			cd = dd.getConglomerateDescriptor(getRelativeName(), sd, false);

		if (cd == null)
		{
			throw StandardException.newException(SQLState.LANG_INDEX_NOT_FOUND, getFullName());
		}

		/* Get the table descriptor */
		td = getTableDescriptor(cd.getTableID());

		/* Drop index is not allowed on an index backing a constraint -
		 * user must drop the constraint, which will drop the index.
		 * Drop constraint drops the constraint before the index,
		 * so it's okay to drop a backing index if we can't find its
		 * ConstraintDescriptor.
		 */
		if (cd.isConstraint())
		{
			ConstraintDescriptor conDesc;
			String constraintName;

			conDesc = dd.getConstraintDescriptor(td, cd.getUUID());
			if (conDesc != null)
			{
				constraintName = conDesc.getConstraintName();
				throw StandardException.newException(SQLState.LANG_CANT_DROP_BACKING_INDEX, 
										getFullName(), constraintName);
			}
		}

		/* Statement is dependent on the TableDescriptor and ConglomerateDescriptor */
		cc.createDependency(td);
		cc.createDependency(cd);
	}

	// inherit generate() method from DDLStatementNode

	/**
	 * Create the Constant information that will drive the guts of Execution.
	 *
	 * @exception StandardException		Thrown on failure
	 */
	public ConstantAction	makeConstantAction() throws StandardException
	{
		return	getGenericConstantActionFactory().getDropIndexConstantAction( getFullName(),
											 getRelativeName(),
											 getRelativeName(),
											 getSchemaDescriptor().getSchemaName(),
											 td.getUUID(),
											 td.getHeapConglomerateId());
	}
}
