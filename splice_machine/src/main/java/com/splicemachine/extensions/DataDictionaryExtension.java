package com.splicemachine.extensions;

import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.dictionary.DataDescriptorGenerator;
import com.splicemachine.db.iapi.store.access.TransactionController;
import com.splicemachine.derby.impl.sql.catalog.SpliceDataDictionary;

import java.util.Properties;

public interface DataDictionaryExtension {
	void createDictionaryTables(SpliceDataDictionary sdd, Properties params, TransactionController tc, DataDescriptorGenerator ddg) throws StandardException;
}
