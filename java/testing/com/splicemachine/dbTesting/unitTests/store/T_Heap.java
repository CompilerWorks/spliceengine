/*

   Derby - Class com.splicemachine.dbTesting.unitTests.store.T_Heap

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.splicemachine.dbTesting.unitTests.store;

// impl imports are the preferred way to create unit tests.
import com.splicemachine.dbTesting.unitTests.harness.T_Generic;
import com.splicemachine.dbTesting.unitTests.harness.T_Fail;

import java.util.Properties;

import com.splicemachine.db.iapi.services.context.ContextService;

import com.splicemachine.db.iapi.services.monitor.Monitor;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.store.access.AccessFactory;
import com.splicemachine.db.iapi.store.access.TransactionController;

import com.splicemachine.db.iapi.reference.Property;

public class T_Heap extends T_Generic
{
	private static final String testService = "heapTest";
	/*
	** Methods required by T_Generic
	*/

	public String getModuleToTestProtocolName() {
		return AccessFactory.MODULE;
	}

	/**
		@exception T_Fail test failed.
	*/
	protected void runTests() throws T_Fail
	{
		AccessFactory store = null;
		TransactionController tc = null;
		boolean pass = false;

        out.println("executing heap test");

		// don't automatic boot this service if it gets left around
		if (startParams == null) {
			startParams = new Properties();
		}
		startParams.put(Property.NO_AUTO_BOOT, Boolean.TRUE.toString());
		// remove the service directory to ensure a clean run
		startParams.put(Property.DELETE_ON_CREATE, Boolean.TRUE.toString());

		// see if we are testing encryption
		startParams = T_Util.setEncryptionParam(startParams);

		try {
			store = (AccessFactory) Monitor.createPersistentService(getModuleToTestProtocolName(),
			testService, startParams);
		} catch (StandardException mse) {
			throw T_Fail.exceptionFail(mse);
		}

		if (store == null) {
			throw T_Fail.testFailMsg(getModuleToTestProtocolName() + " service not started.");
		}
		REPORT("(unitTestMain) Testing " + testService);

		try {

            tc = store.getTransaction(
                    ContextService.getFactory().getCurrentContextManager());

            if (t_001(tc))
			{
				pass = true;
			}

			tc.commit();
			tc.destroy();
		}
		catch (StandardException e)
		{
            System.out.println("got an exception.");
			String  msg = e.getMessage();
			if (msg == null)
				msg = e.getClass().getName();
			REPORT(msg);
			throw T_Fail.exceptionFail(e);
		}

		if (!pass)
			throw T_Fail.testFailMsg("T_Heap test failed");
	}

    /*
     * Test Qualifiers.
     */
    protected boolean t_001(TransactionController tc)
        throws StandardException, T_Fail
    {
        REPORT("Starting t_001");

        T_QualifierTest q_test = 
            new T_QualifierTest(
                "heap",         // create a heap
                null,           // properties
                false,          // not temporary
                out,
                T_QualifierTest.ORDER_NONE);         // unordered data

        boolean test_result = q_test.t_testqual(tc);

        if (!test_result)
            throw T_Fail.testFailMsg("T_Heap.t_001 failed");

        REPORT("Ending t_001");

        return(test_result);
    }
}
