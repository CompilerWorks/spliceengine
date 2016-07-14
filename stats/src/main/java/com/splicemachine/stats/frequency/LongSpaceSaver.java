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

package com.splicemachine.stats.frequency;

import com.google.common.base.Function;
import org.sparkproject.guava.collect.Collections2;
import org.sparkproject.guava.primitives.Longs;
import com.splicemachine.hash.Hash32;

import java.util.Collection;
import java.util.Comparator;

/**
 * @author Scott Fines
 *         Date: 1/30/15
 */
class LongSpaceSaver extends ObjectSpaceSaver<Long> implements LongFrequencyCounter {
    private static final Comparator<Long> comparator = new Comparator<Long>() {
        @Override public int compare(Long o1, Long o2) { return o1.compareTo(o2); }
    };
    private static final Function<? super FrequencyEstimate<Long>,LongFrequencyEstimate> castFunction =
            new Function<FrequencyEstimate<Long>, LongFrequencyEstimate>() {
        @Override public LongFrequencyEstimate apply(FrequencyEstimate<Long> input) { return (LongFrequencyEstimate)input; }
    };

    public LongSpaceSaver(Hash32 hashFunction, int maxSize) {
        super(comparator, hashFunction, maxSize);
    }

    public LongSpaceSaver( Hash32 hashFunction, int maxSize, int initialSize, float loadFactor) {
        super(comparator, hashFunction, maxSize, initialSize, loadFactor);
    }

    /*************************************************************************************************************/
    /*Accessors*/
    @Override
    public LongFrequentElements heavyHitters(float support) {
        Collection<FrequencyEstimate<Long>> estimates = super.heavyItems(support);
        return LongFrequentElements.heavyHitters(support,totalCount(),Collections2.transform(estimates,castFunction));
    }

    @Override
    public LongFrequentElements frequentElements(int k) {
        Collection<FrequencyEstimate<Long>> estimates = super.topKElements(k);
        return LongFrequentElements.topK(k,totalCount(),Collections2.transform(estimates,castFunction));
    }

    /************************************************************************************************************/
    /*Modifiers*/
    @Override
    public void update(Long item) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.longValue(),1l);
    }

    @Override
    public void update(Long item, long count) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.longValue(),count);
    }

    @Override public void update(long item) { update(item,1l); }

    @Override
    public void update(long item, long count) {
        ((LongEntry)holderEntry).setValue(item);
        doUpdate(count);
    }

    /*****************************************************************************************************************/
    /*Overridden methods*/
    @Override
    protected Entry newEntry() {
        return new LongEntry();
    }

    @Override
    protected void setValue(Entry holderEntry, Entry entry) {
        ((LongEntry)entry).value = ((LongEntry)holderEntry).value;
    }

    private class LongEntry extends Entry implements LongFrequencyEstimate{
        long value;
        @Override public Long getValue() { return value; }
        @Override public long value() { return value; }

        @Override
        public void set(Long item) {
            this.value = item;
        }

        @Override
        public boolean equals(Entry o) {
            return ((LongEntry)o).value==value;
        }

        @Override
        public int compareTo(LongFrequencyEstimate o) {
            return Longs.compare(value,o.value());
        }

        public void setValue(long item){
            this.value = item;
            this.hashCode = 0;
        }

        @Override
        protected int computeHash() {
            int hash = hashFunction.hash(value);
            if(hash==0)
                hash = 1;
            return hash;
        }
    }
}
