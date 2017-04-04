/*
 * Copyright (c) 2012 - 2017 Splice Machine, Inc.
 *
 * This file is part of Splice Machine.
 * Splice Machine is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3, or (at your option) any later version.
 * Splice Machine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with Splice Machine.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.splicemachine.extensions;

import com.splicemachine.db.catalog.UUID;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.dictionary.DataDescriptorGenerator;
import com.splicemachine.db.iapi.sql.dictionary.DataDictionary;
import com.splicemachine.db.iapi.store.access.TransactionController;
import com.splicemachine.db.impl.sql.catalog.Procedure;
import com.splicemachine.derby.impl.sql.catalog.SpliceDataDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

public class ExtensionManager implements Extension {
    public static final ExtensionManager INSTANCE = new ExtensionManager();
    
    private final List<Extension> extensions = new ArrayList<>();

    public ExtensionManager() {
        for (Extension dde : ServiceLoader.load(Extension.class))
            this.extensions.add(dde);
    }
    
    // TODO add setters for SConfiguration config, Snowflake snowflake, Connection internalConnection, DatabaseVersion spliceVersion

    @Override
    public void createDictionaryTables(SpliceDataDictionary sdd, Properties params, TransactionController tc, DataDescriptorGenerator ddg) throws StandardException {
        for (Extension extension : extensions)
            extension.createDictionaryTables(sdd, params, tc, ddg);
    }

    @Override
    public void addProcedures(Map<UUID,List<Procedure>> procedures, DataDictionary dictionary, TransactionController tc) throws StandardException {
        for (Extension extension : extensions)
            extension.addProcedures(procedures, dictionary, tc);
    }
}
