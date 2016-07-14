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

import com.splicemachine.db.iapi.services.stream.HeaderPrintWriter;

// For testing
//import java.io.OutputStreamWriter;

class BasicUnitTest implements UnitTest
{
	String traceMessage;
	int testType;
	int testDuration;
	boolean result;
	Error exception;

	BasicUnitTest(String traceMessage,
				  int testType,
				  int testDuration,
				  boolean result,
				  Error exception){
		this.traceMessage = traceMessage;
		this.testType = testType;
		this.testDuration = testDuration;
		this.result = result;
		this.exception = exception;
	}

	public String toString(){
		return ("testType: "+testType+" testDuration: "+
			testDuration+" traceMessage: "+traceMessage+
			" result: "+result+" exception: "+exception);
	}


	public boolean Execute (HeaderPrintWriter output) {
	
		output.printlnWithHeader(toString());
		if (exception != null)
			throw exception;
	
		return result;
	}



	public int UnitTestDuration(){
		return testDuration;
	}

	public int UnitTestType(){
		return testType;
	}

	private void executeCatch(HeaderPrintWriter output){
		 try{
			 Execute(output);
		 }
		 catch (Error e){
			 System.out.println("Caught exception:"+ e);
		 }
	}

/*

	public static void main(String[] Args){

		OutputStreamWriter osw = new OutputStreamWriter(System.out);
		BasicGetLogHeader glh = new BasicGetLogHeader(
				true, true, "hi" );
		BasicHeaderPrintWriter hpw = new BasicHeaderPrintWriter(osw,glh);
 
		 
		BasicUnitTest t1 = 
			  new BasicUnitTest("hi Eric",1,1,true,null);
				  
		t1.executeCatch(hpw);

		BasicUnitTest t2 = 
			 new BasicUnitTest("hi my dear boy",1,1,true,null);

		t2.executeCatch(hpw);

		BasicUnitTest t3 = 
			 new BasicUnitTest("hi my dear boy",1,1,true,
				new Error("bogus Error"));

		t3.executeCatch(hpw);

		

	}
	
*/
}

