package com.splicemachine.si.impl;

/**
 * Considering key-values from data tables, based on the column family and qualifier they can be classified as one of
 * these types.
 */
public enum CellType{

    /* Column "0" */
    COMMIT_TIMESTAMP,

    /* Column "1" (if empty value) */
    TOMBSTONE,

    /* Column "1" (if "0" value) */
    ANTI_TOMBSTONE,

    /* Column "7" */
    USER_DATA,

    /* Column "9" */
    FOREIGN_KEY_COUNTER,

    /* Column "z" */
    CHECKPOINT,

    /* Unrecognized column/column-value. */
    OTHER
}
