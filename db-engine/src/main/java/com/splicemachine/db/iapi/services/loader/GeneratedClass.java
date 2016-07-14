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

package com.splicemachine.db.iapi.services.loader;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.services.context.Context;

/**
	A meta-class that represents a generated class.
	(Similar to java.lang.Class).
*/

public interface GeneratedClass {

	/**
		Return the name of the generated class.
	*/
	public String getName();

	/**
		Return a new object that is an instance of the represented
		class. The object will have been initialised by the no-arg
		constructor of the represneted class.
		(Similar to java.lang.Class.newInstance).

		@exception 	StandardException	Standard Derby error policy

	*/
	public Object newInstance(Context context)
		throws StandardException;

	/**
		Obtain a handle to the method with the given name
		that takes no arguments.

		@exception 	StandardException	Standard Derby error policy
	*/
	public GeneratedMethod getMethod(String simpleName)
		throws StandardException;

	/**
		Return the class reload version that this class was built at.
	*/
	public int getClassLoaderVersion();
}

