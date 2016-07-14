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

package com.splicemachine.db.impl.services.reflect;

import com.splicemachine.db.iapi.services.loader.GeneratedByteCode;
import com.splicemachine.db.iapi.services.loader.GeneratedClass;
import com.splicemachine.db.iapi.services.loader.ClassFactory;

import com.splicemachine.db.iapi.services.context.Context;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.reference.SQLState;

import com.splicemachine.db.iapi.services.loader.ClassInfo;


public abstract class LoadedGeneratedClass
	implements GeneratedClass
{

	/*
	** Fields
	*/

	private final ClassInfo	ci;
	private final int classLoaderVersion;

	/*
	**	Constructor
	*/

	public LoadedGeneratedClass(ClassFactory cf, Class jvmClass) {
		ci = new ClassInfo(jvmClass);
		classLoaderVersion = cf.getClassLoaderVersion();
	}

	/*
	** Public methods from Generated Class
	*/

	public String getName() {
		return ci.getClassName();
	}

	public Object newInstance(Context context) throws StandardException	{

		Throwable t;
		try {
			GeneratedByteCode ni =  (GeneratedByteCode) ci.getNewInstance();
			ni.initFromContext(context);
			ni.setGC(this);
			ni.postConstructor();
			return ni;

		} catch (InstantiationException ie) {
			t = ie;
		} catch (IllegalAccessException iae) {
			t = iae;
		} catch (java.lang.reflect.InvocationTargetException ite) {
			t = ite;
		} catch (LinkageError le) {
			t = le;
		}

		throw StandardException.newException(SQLState.GENERATED_CLASS_INSTANCE_ERROR, t, getName());
	}

	public final int getClassLoaderVersion() {
		return classLoaderVersion;
	}

	/*
	** Methods for subclass
	*/
	protected Class getJVMClass() {
		return ci.getClassObject();
	}
}
