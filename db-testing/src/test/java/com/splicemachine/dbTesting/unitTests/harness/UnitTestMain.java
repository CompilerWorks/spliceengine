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

package com.splicemachine.dbTesting.unitTests.harness;

import com.splicemachine.db.iapi.services.monitor.Monitor;

import java.util.Properties;

/** 
	A very simple class to boot up a system based upon a configuration file
	passed in as the first argument. The optional second argument is taken
	as a boolean. If the argument is missing or false, the configuration
	is started, otherwise the configuration is created.
	

    Usage: java com.splicemachine.dbTesting.unitTests.harness.UnitTestMain config-file [true]
**/


public class UnitTestMain  { 

	public static void main(String args[]) {


		Properties bootProperties = new Properties();

		// request that a unit test manager service is started
		bootProperties.put("derby.service.unitTestManager", UnitTestManager.MODULE);

		Monitor.startMonitor(bootProperties, System.err);
	}
}
