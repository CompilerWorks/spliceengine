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

package com.splicemachine.db.iapi.services.info;

import java.security.AccessControlException;
import java.security.AccessController;

import com.splicemachine.db.mbeans.VersionMBean;
import com.splicemachine.db.security.SystemPermission;

/**
 * This implementation of VersionMBean instruments a
 * ProductVersionHolder object. The MBean interface is implemented with 
 * callbacks to the wrapped object which gives detailed version information.
 *
 * @see com.splicemachine.db.iapi.services.info.ProductVersionHolder
 */
public class Version implements VersionMBean {
    
    private final ProductVersionHolder versionInfo;
    
    /**
     * Permission target name for the object the version
     * information applies to.
     */
    private final String permissionName;
    
    public Version(ProductVersionHolder pvh, String permissionName) {
        versionInfo = pvh;
        this.permissionName = permissionName;
    }
    
    /*
    ** Security checks(non-Javadoc)
    */
    
    /**
     * Ensure caller has permission to monitor Derby.
     */
    private void checkMonitor() {

        try {
            if (System.getSecurityManager() != null)
                AccessController.checkPermission(
                        new SystemPermission(permissionName,
                                SystemPermission.MONITOR));
        } catch (AccessControlException e) {
            // Need to throw a simplified version as AccessControlException
            // will have a reference to Derby's SystemPermission which most likely
            // will not be available on the client.
            throw new SecurityException(e.getMessage());
        }
    }
    
    // ------------------------- MBEAN ATTRIBUTES  ----------------------------
    
    public String getProductName(){
        checkMonitor();
        return versionInfo.getProductName();
    }
    
     public String getProductTechnologyName(){
         checkMonitor();
        return versionInfo.getProductTechnologyName();
    }
    
    public String getProductVendorName(){
        checkMonitor();
        return versionInfo.getProductVendorName();
    }
    
    public String getVersionString() {
        checkMonitor();
        return versionInfo.getVersionBuildString(true);
    }
    public int getMajorVersion(){
        checkMonitor();
        return versionInfo.getMajorVersion();
    }
    
    public int getMinorVersion(){
        checkMonitor();
        return versionInfo.getMinorVersion();
    }
    
    public int getMaintenanceVersion(){
        checkMonitor();
        return versionInfo.getMaintVersion();
    }
    
    public String getBuildNumber(){
        checkMonitor();
        return versionInfo.getBuildNumber();
    }
    
    public boolean isBeta(){
        checkMonitor();
        return versionInfo.isBeta();
    }
    
    public boolean isAlpha(){
        checkMonitor();
        return versionInfo.isAlpha();
    }
  
}
