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

import com.splicemachine.db.iapi.services.compiler.MethodBuilder;
import com.splicemachine.db.iapi.services.compiler.LocalField;
import com.splicemachine.db.iapi.error.StandardException;

/**
 * A MaterializeSubqueryNode is used to replace the nodes for a subquery, to facilitate
 * code generation for materialization if possible.  See beetle 4373 for details.
 *
 */
class MaterializeSubqueryNode extends ResultSetNode
{

	private LocalField lf;

	public MaterializeSubqueryNode(LocalField lf)
	{
		this.lf = lf;
	}

	public void generate(ActivationClassBuilder acb,
						 MethodBuilder mb)
		throws StandardException
	{
		// Simply return reference to field, which has been populated in the
		// execute() method (per SubqueryNode logic)
		mb.getField(lf);
	}

	void decrementLevel(int decrement)
	{
	}
}
