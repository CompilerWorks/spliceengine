package com.splicemachine.stats.frequency;

import com.google.common.base.Function;
import org.sparkproject.guava.collect.Collections2;
import org.sparkproject.guava.primitives.Shorts;
import com.splicemachine.hash.Hash32;

import java.util.Collection;
import java.util.Comparator;

/**
 * @author Scott Fines
 *         Date: 2/18/15
 */
class ShortSpaceSaver extends ObjectSpaceSaver<Short> implements ShortFrequencyCounter{
    private static final Comparator<Short> comparator = new Comparator<Short>() {
        @Override
        public int compare(Short o1, Short o2) {
            return o1.compareTo(o2);
        }
    };
    private static final Function<? super FrequencyEstimate<Short>,ShortFrequencyEstimate> castFunction =
            new Function<FrequencyEstimate<Short>, ShortFrequencyEstimate>() {
                @Override public ShortFrequencyEstimate apply(FrequencyEstimate<Short> input) { return (ShortFrequencyEstimate)input; }
            };

    public ShortSpaceSaver(Hash32 hashFunction, int maxSize) {
        super(comparator, hashFunction, maxSize);
    }

    public ShortSpaceSaver( Hash32 hashFunction, int maxSize, int initialSize, float loadFactor) {
        super(comparator, hashFunction, maxSize, initialSize, loadFactor);
    }

    /*************************************************************************************************************/
    /*Accessors*/
    @Override
    public ShortFrequentElements heavyHitters(float support) {
        Collection<FrequencyEstimate<Short>> estimates = super.heavyItems(support);
        return ShortFrequentElements.heavyHitters(support,totalCount(),Collections2.transform(estimates, castFunction));
    }

    @Override
    public ShortFrequentElements frequentElements(int k) {
        Collection<FrequencyEstimate<Short>> estimates = super.topKElements(k);
        return ShortFrequentElements.topK(k,totalCount(),Collections2.transform(estimates,castFunction));
    }

    /************************************************************************************************************/
    /*Modifiers*/
    @Override
    public void update(Short item) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.shortValue(),1l);
    }

    @Override
    public void update(Short item, long count) {
        assert item!=null: "Cannot estimate frequency of null elements!";
        update(item.shortValue(),count);
    }

    @Override public void update(short item) { update(item,1l); }

    @Override
    public void update(short item, long count) {
        ((ShortEntry)holderEntry).setValue(item);
        doUpdate(count);
    }

    /*****************************************************************************************************************/
    /*Overridden methods*/
    @Override
    protected Entry newEntry() {
        return new ShortEntry();
    }

    @Override
    protected void setValue(Entry holderEntry, Entry entry) {
        ((ShortEntry)entry).value = ((ShortEntry)holderEntry).value;
    }

    private class ShortEntry extends Entry implements ShortFrequencyEstimate{
        short value;
        @Override public Short getValue() { return value; }
        @Override public short value() { return value; }

        @Override
        public void set(Short item) {
            this.value = item;
        }

        @Override
        public boolean equals(Entry o) {
            return ((ShortEntry)o).value==value;
        }

        @Override
        public int compareTo(ShortFrequencyEstimate o) {
            return Shorts.compare(value, o.value());
        }

        public void setValue(short item){
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
