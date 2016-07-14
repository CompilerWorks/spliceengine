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

package com.splicemachine.collections.hashtable;

import java.util.Map;

/**
 * @author Scott Fines
 *         Date: 10/8/14
 */
public interface HashTable<K,V> extends Map<K,V> {


    /**
     * Remove the entry associated with the specified key.
     *
     * @param key the key to remove the value for
     * @param forceRemoveValue if true, then forcibly clean the underlying data structure;
     *                         otherwise, the implementation may decide not to dereference the
     *                         stored objects, which may cause excessive memory usage when a
     *                         large number of deletions occur.
     * @return the value previously associated with the specified key, or {@code null} if
     * no entry with that key exists.
     */
    V remove(K key, boolean forceRemoveValue);

    /**
     * Some implementations may lazily remove entries, which would leave an
     * element behind in the hashtable. This can cause confusion with memory (and
     * potentially memory problems if a large number of deletions occur). By specifying
     * {@code forceRemoveValues = true}, the implementation should forcibly remove entries,
     * and thus improve overall memory usage (generally at a performance penalty).
     *
     * @param forceRemoveValues
     */
    void clear(boolean forceRemoveValues);

    /**
     * @return the ratio of filled entries to available entries. Mostly useful for debugging
     * and other monitoring.
     */
    float load();
}
