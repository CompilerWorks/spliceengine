/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.splicemachine.derby.iapi.sql;

import com.splicemachine.db.iapi.error.StandardException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Set;

/**
 * @author Scott Fines
 *         Date: 1/8/16
 */
@ThreadSafe
public interface PropertyManager{

    boolean propertyExists(String propertyName) throws StandardException;

    Set<String> listProperties() throws StandardException;

    String getProperty(String propertyName) throws StandardException;

    void addProperty(String propertyName, String propertyValue) throws StandardException;

    void clearProperties() throws StandardException;
}
