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

package com.splicemachine.db.impl.services.monitor;

import com.splicemachine.db.iapi.services.monitor.PersistentService;

/**
	A description of an instance of a module.
*/


class ModuleInstance {

	/*
	** Fields.
	*/

	/**
		The module instance
	*/
	protected Object instance;

	/**
		name of module, can be null
	*/
	protected String		identifier;

	/**
		the top-level service this module lives in, can be null or the service itself
	*/
	protected Object	topLevelService;

	/**
		the actual service to which I belong, could be null.
	*/
	protected Object	service;

    /** Flag that tells whether booting of the module has completed. */
    private boolean booted;

	/*
	** Constructor
	*/

	protected ModuleInstance(Object instance, String identifier,
			Object service, Object topLevelService)
	{
		super();
		this.instance = instance;
		this.identifier = identifier;
		this.topLevelService = topLevelService;
		this.service = service;

	}

	protected ModuleInstance(Object instance) {

		this(instance, null, null, null);
	}

	protected boolean isTypeAndName(PersistentService serviceType, 
		Class factoryInterface, String otherCanonicalName)
	{
		// see if the correct interface is implemented
		if (!factoryInterface.isInstance(instance))
			return false;

		if ((serviceType != null) && (otherCanonicalName != null))
			return serviceType.isSameService(identifier, otherCanonicalName);


		// see if the identifiers match
		if (otherCanonicalName != null) {
			if (identifier == null)
				return false;
			if (!otherCanonicalName.equals(identifier))
				return false;
		} else if (identifier != null) {
			return false;
		}

		return true;
	}

	protected String getIdentifier() {
		return identifier;
	}

	protected Object getTopLevelService() {
		return topLevelService;
	}

	protected Object getInstance() {
		return instance;
	}

    /**
     * Set a flag that indicates that booting of the module has completed.
     */
    synchronized void setBooted() {
        booted = true;
    }

    /**
     * Check whether booting of the module has completed.
     * @return {@code true} if the module has been booted, or {@code false}
     * otherwise
     */
    synchronized boolean isBooted() {
        return booted;
    }
}
