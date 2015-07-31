/*

   Derby - Class org.apache.derby.impl.sql.compile.IsNullNode

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

package com.splicemachine.db.impl.sql.compile;

import com.splicemachine.db.iapi.reference.ClassName;

import com.splicemachine.db.iapi.services.compiler.MethodBuilder;
import com.splicemachine.db.iapi.services.sanity.SanityManager;

import com.splicemachine.db.iapi.error.StandardException;

import com.splicemachine.db.iapi.sql.compile.C_NodeTypes;
import com.splicemachine.db.iapi.sql.compile.Optimizable;

import com.splicemachine.db.iapi.store.access.ScanController;

import com.splicemachine.db.iapi.types.DataTypeDescriptor;
import com.splicemachine.db.iapi.types.DataValueDescriptor;
import com.splicemachine.db.iapi.types.TypeId;

import com.splicemachine.db.iapi.types.Orderable;

import java.sql.Types;

/**
 * This node represents either a unary 
 * IS NULL or IS NOT NULL comparison operator
 *
 */

public final class IsNullNode extends UnaryComparisonOperatorNode  {

	private DataValueDescriptor nullValue;

	@Override
	public void setNodeType(int nodeType) {
		String operator;
		String methodName;

		if (nodeType == C_NodeTypes.IS_NULL_NODE) {
			/* By convention, the method name for the is null operator is "isNull" */
			operator = "is null";
			methodName = "isNullOp";
		} else {
			if (SanityManager.DEBUG) {
				if (nodeType != C_NodeTypes.IS_NOT_NULL_NODE) {
					SanityManager.THROWASSERT( "Unexpected nodeType = " + nodeType);
				}
			}
			/* By convention, the method name for the is not null operator is 
			 * "isNotNull" 
			 */
			operator = "is not null";
			methodName = "isNotNull";
		}
		setOperator(operator);
		setMethodName(methodName);
		super.setNodeType(nodeType);
	}

	/**
	 * Negate the comparison.
	 *
	 * @param operand	The operand of the operator
	 *
	 * @return UnaryOperatorNode	The negated expression
	 *
	 * @exception StandardException		Thrown on error
	 */
	@Override
	UnaryOperatorNode getNegation(ValueNode operand) throws StandardException {

		if (SanityManager.DEBUG) {
			SanityManager.ASSERT(getTypeServices() != null, "dataTypeServices is expected to be non-null");
		}

		if (isNullNode()) {
			setNodeType(C_NodeTypes.IS_NOT_NULL_NODE);
		} else {
			if (SanityManager.DEBUG) {
				if (! isNotNullNode()) {
					SanityManager.THROWASSERT( "Unexpected nodeType = " + getNodeType());
				}
			}
			setNodeType(C_NodeTypes.IS_NULL_NODE);
		}
		return this;
	}

	@Override
	void bindParameter() throws StandardException {
		/*
		** If IS [NOT] NULL has a ? operand, we assume
		** its type is varchar with the implementation-defined maximum length
		** for a varchar.
		** Also, for IS [NOT] NULL, it doesn't matter what is VARCHAR's 
		** collation (since for NULL check, no collation sensitive processing
		** is required) and hence we will not worry about the collation setting
		*/

		operand.setType(new DataTypeDescriptor(TypeId.getBuiltInTypeId(Types.VARCHAR), true));
	}

	/* RelationalOperator interface */

	@Override
	public boolean usefulStartKey(Optimizable optTable) {
		// IS NULL is start/stop key, IS NOT NULL is not
		return (isNullNode());
	}

	@Override
	public boolean usefulStopKey(Optimizable optTable) {
		// IS NULL is start/stop key, IS NOT NULL is not
		return (isNullNode());
	}

	@Override
	public int getStartOperator(Optimizable optTable) {
		assert isNullNode(): "getNodeType() not expected to return "+ getNodeType();
		return ScanController.GE;
	}

	@Override
	public int getStopOperator(Optimizable optTable) {
		assert isNullNode(): "getNodeType() not expected to return "+ getNodeType();
		return ScanController.GT;
	}

	@Override
	public void generateOperator(MethodBuilder mb, Optimizable optTable) {
		mb.push(Orderable.ORDER_OP_EQUALS);
	}

	@Override
	public void generateNegate(MethodBuilder mb, Optimizable optTable) {
		mb.push(isNotNullNode());
	}

	@Override
	public int getOperator() {
		int operator;
		if (isNullNode()) {
			operator = IS_NULL_RELOP;
		} else {
			assert isNotNullNode():"Unexpected nodeType = "+ getNodeType();
			operator = IS_NOT_NULL_RELOP;
		}

		return operator;
	}

	@Override
	public boolean compareWithKnownConstant(Optimizable optTable, boolean considerParameters) { return true; }

	@Override
	public DataValueDescriptor getCompareValue(Optimizable optTable) throws StandardException {
		if (nullValue == null) {
			nullValue = operand.getTypeServices().getNull();
		}

		return nullValue;
	}

	@Override
	public boolean equalsComparisonWithConstantExpression(Optimizable optTable) {
		boolean retval = false;

		// Always return false for NOT NULL
		if (isNotNullNode()) {
			return false;
		}

		/*
		** Is the operand a column in the given table?
		*/
		if (operand instanceof ColumnReference) {
			int tabNum = ((ColumnReference) operand).getTableNumber();
			if (optTable.hasTableNumber() && (optTable.getTableNumber() == tabNum)) {
				retval = true;
			}
		}

		return retval;
	}

	@Override
	public RelationalOperator getTransitiveSearchClause(ColumnReference otherCR) throws StandardException {
		return (RelationalOperator) getNodeFactory().getNode( getNodeType(), otherCR, getContextManager());
	}

	/**
	 * null operators are defined on DataValueDescriptor.
	 * Overrides method in UnaryOperatorNode for code generation purposes.
	 */
	@Override
	public String getReceiverInterfaceName() { return ClassName.DataValueDescriptor; }

	/** IS NULL is like =, so should have the same selectivity */
	@Override
	public double selectivity(Optimizable optTable)  {
		if (isNullNode()) {
			return 0.1d;
		} else {
			assert isNotNullNode(): "Unexpected nodeType = "+ getNodeType();
			/* IS NOT NULL is like <>, so should have same selectivity */
			return 0.9d;
		}
	}

	private boolean isNullNode() { return getNodeType() == C_NodeTypes.IS_NULL_NODE; }

	private boolean isNotNullNode() { return getNodeType() == C_NodeTypes.IS_NOT_NULL_NODE; }

	@Override
	public boolean isRelationalOperator()
	{
		return true;
	}

	@Override
	public boolean optimizableEqualityNode(Optimizable optTable,
										   int columnNumber, 
										   boolean isNullOkay) {
		if (!isNullNode() || !isNullOkay)
			return false;
		
		ColumnReference cr = getColumnOperand(optTable, columnNumber);
		return cr!=null;
	}

    @Override
    public long getCardinalityEstimate() {
        return 2l;
    }


}
