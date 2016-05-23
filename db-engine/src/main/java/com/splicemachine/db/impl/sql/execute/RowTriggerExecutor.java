/*

   Derby - Class org.apache.derby.impl.sql.execute.RowTriggerExecutor

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
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.dictionary.TriggerDescriptor;
import com.splicemachine.db.iapi.sql.conn.LanguageConnectionContext;
import com.splicemachine.db.iapi.sql.Activation;

/**
 * A row trigger executor is an object that executes a row trigger.  It is instantiated at execution time.
 * There is one per row trigger.
 */
public class RowTriggerExecutor extends GenericTriggerExecutor {
    /**
     * Constructor
     *
     * @param tec        the execution context
     * @param triggerd   the trigger descriptor
     * @param activation the activation
     * @param lcc        the lcc
     */
    RowTriggerExecutor(TriggerExecutionContext tec, TriggerDescriptor triggerd, Activation activation, LanguageConnectionContext lcc) {
        super(tec, triggerd, activation, lcc);
    }

    /**
     * Fire the trigger based on the event.
     *
     * @param event             the trigger event
     * @param rs                the triggering result set
     * @param colsReadFromTable columns required from the trigger table by the triggering sql
     */
    @Override
    void fireTrigger(TriggerEvent event, CursorResultSet rs, int[] colsReadFromTable) throws StandardException {
        tec.setTrigger(triggerd);
        tec.setCurrentTriggerEvent(event);

        try {
            tec.setTriggeringResultSet(rs);

            /*
                This is the key to handling autoincrement values that might
                be seen by insert triggers. For an AFTER ROW trigger, update
                the autoincrement counters before executing the SPS for the
                trigger.
            */
            if (event.isAfter()) {
                tec.updateAICounters();
            }

            executeSPS(getAction());

                /*
                  For BEFORE ROW triggers, update the ai values after the SPS
                  has been executed. This way the SPS will see ai values from
                  the previous row.
                */
            if (event.isBefore()) {
                tec.updateAICounters();
            }
        } finally {
            clearSPS();
            tec.clearTrigger();
        }
    }

}
