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

package com.splicemachine.db.impl.tools.ij;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.util.Date;

import com.splicemachine.db.iapi.tools.i18n.LocalizedOutput;

/**
 * mtTester grabs test and runs them forever.
 * The spawner of tester is responsible for 
 * killing it.
 */
public class mtTester implements Runnable
{
	private mtTestSuite	suite;
	private String		name;
	private LocalizedOutput	log;
	private LocalizedOutput	out;
	private boolean		stop = false;
	private boolean   testOK = false;
							
	public mtTester(String name, mtTestSuite suite, LocalizedOutput out, LocalizedOutput log)
	{ 
		this.name = name;
		this.suite = suite;
		this.log = log;
		this.out = out;
		log.println("...initialized "+ name + " at " + new Date());
	}

	/**
	** Run until killed or until there is a problem.
	** If we get other than 'connection closed' we'll
	** signal that we recieved a fatal error before
	** quittiing; otherwise, we are silent.
	*/
	public void run()
	{
		int numIterations = 0;

		try 
		{
			mtTestCase testCase;
			BufferedInputStream	in;

			// loop until we get an error or
			// are killed.	
			while (!stop)
			{
				numIterations++;
				testCase = suite.grabTestCase();
				try 
				{
					in = testCase.initialize(suite.getRoot());
				} catch (FileNotFoundException e) 
				{
					System.out.println(e);
					return;
				}
				catch (IOException e)
				{
					System.out.println(e);
					return;
				}
	
				log.println(name + ": "+ testCase.getName() + " " + new Date());
				testCase.runMe(log, out, in);
			}
		}	
		catch (ijFatalException e)
		{

			/*
			** If we got connection closed (XJ010), we'll
			** assume that we were deliberately killed
			** via a Thread.stop() and it was caught by
			** jbms.  Otherwise, we'll print out an
			** error message.
			*/
			if (e.getSQLState() == null || !(e.getSQLState().equals("XJ010")))
			{
				log.println(name + ": TERMINATING due to unexpected error:\n"+e);
				throw new ThreadDeath();
			}
		}
		if (stop)
		{
			log.println(name + ": stopping on request after " + numIterations +
						" iterations");
			testOK = true;
		}
	}

	public void stop()
	{
		stop = true;
	}
	public boolean noFailure()
	{
		return testOK;
	}
}
