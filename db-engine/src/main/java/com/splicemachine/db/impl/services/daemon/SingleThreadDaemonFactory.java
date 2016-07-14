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

package com.splicemachine.db.impl.services.daemon;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.splicemachine.db.iapi.services.context.ContextService;
import com.splicemachine.db.iapi.services.daemon.DaemonFactory;
import com.splicemachine.db.iapi.services.daemon.DaemonService;
import com.splicemachine.db.iapi.services.monitor.Monitor;


public class SingleThreadDaemonFactory implements DaemonFactory
{
	private final ContextService contextService;
	
	public SingleThreadDaemonFactory()
	{
		contextService = ContextService.getFactory();
	}

	/*
	 * Daemon factory method
	 */

	/* make a daemon service with the default timer */
	public DaemonService createNewDaemon(String name)
	{
		BasicDaemon daemon = new BasicDaemon(contextService);

		final Thread daemonThread = Monitor.getMonitor().getDaemonThread(daemon, name, false);
		// DERBY-3745.  setContextClassLoader for thread to null to avoid
		// leaking class loaders.
		try {
            AccessController.doPrivileged(
             new PrivilegedAction() {
                public Object run()  {
                    daemonThread.setContextClassLoader(null);
                    return null;
                }
            });
        } catch (SecurityException se) {
            // ignore security exception.  Earlier versions of Derby, before the 
            // DERBY-3745 fix did not require setContextClassloader permissions.
            // We may leak class loaders if we are not able to set this, but 
            // cannot just fail.
        }


		daemonThread.start();
		return daemon;
	}
}

