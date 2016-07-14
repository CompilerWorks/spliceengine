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

import com.splicemachine.db.iapi.types.TypeId;

import com.splicemachine.db.iapi.error.StandardException;

import com.splicemachine.db.iapi.services.compiler.MethodBuilder;

import com.splicemachine.db.iapi.util.ReuseFactory;
import com.splicemachine.db.iapi.services.classfile.VMOpcode;

import java.sql.Types;

public class BitConstantNode extends ConstantNode
{

	private int bitLength;


	/**
	 * Initializer for a BitConstantNode.
	 *
	 * @param arg1	A Bit containing the value of the constant OR The TypeId for the type of the node
	 *
	 * @exception StandardException
	 */

	public void init(
					Object arg1)
		throws StandardException
	{
		super.init(
					arg1,
					Boolean.TRUE,
					ReuseFactory.getInteger(0));
	}

	public void init(
					Object arg1, Object arg2)
		throws StandardException
	{
		String a1 = (String) arg1;

		byte[] nv = com.splicemachine.db.iapi.util.StringUtil.fromHexString(a1, 0, a1.length());

		Integer bitLengthO = (Integer) arg2;
		bitLength = bitLengthO.intValue();

		init(
			TypeId.getBuiltInTypeId(Types.BINARY),
			Boolean.FALSE,
			bitLengthO);

		com.splicemachine.db.iapi.types.BitDataValue dvd = getDataValueFactory().getBitDataValue(nv);

		dvd.setWidth(bitLength, 0, false);

		setValue(dvd);
	}


	/**
	 * Return an Object representing the bind time value of this
	 * expression tree.  If the expression tree does not evaluate to
	 * a constant at bind time then we return null.
	 * This is useful for bind time resolution of VTIs.
	 * RESOLVE: What do we do for primitives?
	 *
	 * @return	An Object representing the bind time value of this expression tree.
	 *			(null if not a bind time constant.)
	 *
	 * @exception StandardException		Thrown on error
	 */
	Object getConstantValueAsObject()
		throws StandardException
	{
		return value.getBytes();
	}

	/**
	 * This generates the proper constant.  It is implemented
	 * by every specific constant node (e.g. IntConstantNode).
	 *
	 * @param acb	The ExpressionClassBuilder for the class being built
	 * @param mb	The method the code to place the code
	 * @exception StandardException		Thrown on error
	 */
	void generateConstant(ExpressionClassBuilder acb, MethodBuilder mb)
		throws StandardException
	{
		byte[] bytes = value.getBytes();

		String hexLiteral = com.splicemachine.db.iapi.util.StringUtil.toHexString(bytes, 0, bytes.length);

		mb.push(hexLiteral);
		mb.push(0);
		mb.push(hexLiteral.length());

		mb.callMethod(VMOpcode.INVOKESTATIC, "com.splicemachine.db.iapi.util.StringUtil", "fromHexString",
						"byte[]", 3);
	}
}
