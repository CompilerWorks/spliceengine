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
import org.sparkproject.guava.primitives.Ints;
import com.splicemachine.hash.Hash32;

import java.util.Collection;
import java.util.Comparator;

/**
 * @author Scott Fines
 *         Date: 2/18/15
 */
class IntSpaceSaver extends ObjectSpaceSaver<Integer> implements IntFrequencyCounter {
    private static final Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    };
    private static final Function<? super FrequencyEstimate<Integer>,IntFrequencyEstimate> castFunction =
            new Function<FrequencyEstimate<Integer>, IntFrequencyEstimate>() {
                @Override public IntFrequencyEstimate apply(FrequencyEstimate<Integer> input) { return (IntFrequencyEstimate)input; }
            };

    public IntSpaceSaver(Hash32 hashFunction, int maxSize) {
        super(comparator, hashFunction, maxSize);
    }

    public IntSpaceSaver( Hash32 hashFunction, int maxSize, int initialSize, float loadFactor) {
        super(comparator, hashFunction, maxSize, initialSize, loadFactor);
    }

    /*************************************************************************************************************/
    /*Accessors*/
    @Override
    public IntFrequentElements heavyHitters(float support) {
        Collection<FrequencyEstimate<Integer>> estimates = super.heavyItems(support);
        return IntFrequentElements.heavyHitters(support,totalCount(),Collections2.transform(estimates, castFunction));
    }

    @Override
    public IntFrequentElements frequentElements(int k) {
        Collection<FrequencyEstimate<Integer>> estimates = super.topKElements(k);
        return IntFrequentElements.topK(k,totalCount(),Collections2.transform(estimates,castFunction));
    }

    /************************************************************************************************************/
    /*Modifiers*/
    @Override
    public void update(Integer item) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.intValue(),1l);
    }

    @Override
    public void update(Integer item, long count) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.intValue(),count);
    }

    @Override public void update(int item) { update(item,1l); }

    @Override
    public void update(int item, long count) {
        ((IntegerEntry)holderEntry).setValue(item);
        doUpdate(count);
    }

    /*****************************************************************************************************************/
    /*Overridden methods*/
    @Override
    protected Entry newEntry() {
        return new IntegerEntry();
    }

    @Override
    protected void setValue(ObjectSpaceSaver.Entry holderEntry, ObjectSpaceSaver.Entry entry) {
        ((IntegerEntry)entry).value = ((IntegerEntry)holderEntry).value;
    }

    private class IntegerEntry extends Entry implements IntFrequencyEstimate{
        int value;
        @Override public Integer getValue() { return value; }
        @Override public int value() { return value; }

        @Override
        public void set(Integer item) {
            this.value = item;
        }

        @Override
        public boolean equals(Entry o) {
            return ((IntegerEntry)o).value==value;
        }

        @Override
        public int compareTo(IntFrequencyEstimate o) {
            return Ints.compare(value, o.value());
        }

        public void setValue(int item){
            this.value = item;
            this.hashCode = 0; //reset the hash code for this entry
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
