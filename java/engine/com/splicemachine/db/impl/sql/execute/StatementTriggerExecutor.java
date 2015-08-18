/*

   Derby - Class org.apache.derby.impl.sql.execute.StatementTriggerExecutor

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package com.splicemachine.db.impl.sql.execute;

import com.splicemachine.db.iapi.sql.execute.CursorResultSet;
import com.splicemachine.db.iapi.sql.dictionary.TriggerDescriptor;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;

import com.splicemachine.db.iapi.sql.Activation;

/**
 * A statement trigger executor is an object that executes a statement trigger.  It is instantiated at execution
 * time.  There is one per statement trigger.
 */
public class StatementTriggerExecutor extends GenericTriggerExecutor {

    /**
     * Constructor
     *
     * @param tec        the execution context
     * @param triggerd   the trigger descriptor
     * @param activation the activation
     * @param lcc        the lcc
     */
    StatementTriggerExecutor(TriggerExecutionContext tec,
                             TriggerDescriptor triggerd,
                             Activation activation,
                             LanguageConnectionContext lcc) {
        super(tec, triggerd, activation, lcc);
    }

    /**
     * Fire the trigger based on the event.
     *
     * @param event             the trigger event
     * @param rs                the triggering result set
     * @param colsReadFromTable columns required from the trigger table by the triggering sql
     * @throws StandardException on error or general trigger exception
     */
    @Override
    void fireTrigger(TriggerEvent event,
                     CursorResultSet rs,
                     int[] colsReadFromTable) throws StandardException {
        tec.setTrigger(triggerd);
        tec.setCurrentTriggerEvent(event);
        tec.setTriggeringResultSet(rs);

        try {
            executeSPS(getAction());
        } finally {
            clearSPS();
            tec.clearTrigger();
        }
    }
}
