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


import com.splicemachine.encoding.Encoder;
import com.splicemachine.hash.Hash32;
import com.splicemachine.hash.HashFunctions;
import com.splicemachine.primitives.ByteComparator;
import com.splicemachine.primitives.Bytes;
import com.splicemachine.utils.ComparableComparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;

/**
 * Utility class for constructing {@link com.splicemachine.stats.frequency.FrequencyCounter} instances.
 *
 * This class will generally construct a {@code FrequencyCounter} which has been optimized for space and
 * update performance, and are <em>not</em> guaranteed to return exact results.
 *
 * Unless otherwise stated, all instances returned from the class are <em>not</em> thread safe, and
 * care should be taken to ensure that external synchronization is used when accessing data from
 * multiple threads. It should also be noted that cpu cache efficiency is taken into account (somewhat)
 * inside of these instances, and that CPU cache efficiency is generally improved by practicing a thread-isolation
 * model. As a result, it is recommended that returned types from this class be used in a thread-isolated
 * setting.
 *
 * @author Scott Fines
 * Date: 3/27/14
 */
public class FrequencyCounters {
    private static final Hash32 TABLE_HASH_FUNCTION = HashFunctions.murmur3(0);
    private static final float DEFAULT_LOAD_FACTOR = 0.9f;

    /**
     * @return A FrequencyCounter which is specially designed to be efficient for boolean primitive types.
     */
    public static BooleanFrequentElements booleanValues(long trueCount, long falseCount){
        return new SimpleBooleanFrequentElements(trueCount,falseCount);
    }

    /**
     * @return A FrequencyCounter which is specially designed to be efficient for boolean primitive types.
     */
    public static BooleanFrequencyCounter booleanCounter(){ return new SimpleBooleanFrequencyCounter(); }

    /**
     * @return a FrequencyCounter which is specially designed to be efficient for byte primitive types.
     */
		public static ByteFrequencyCounter byteCounter(){ return new EnumeratingByteFrequencyCounter(); }

    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter specifically designed to be efficient with double data types.
     */
		public static DoubleFrequencyCounter doubleCounter(int maxCounters){
        return new DoubleSpaceSaver(TABLE_HASH_FUNCTION,maxCounters);
		}

    /**
     *
     * @param initialSize the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return a FrequencyCounter specifically designed to be efficient with double data types.
     */
	public static DoubleFrequencyCounter doubleCounter(int maxCounters, int initialSize){
		if (maxCounters < initialSize) throw new IllegalArgumentException("maxCounters must be >= initialSize");
		return new DoubleSpaceSaver(TABLE_HASH_FUNCTION,maxCounters,initialSize, DEFAULT_LOAD_FACTOR);
	}

    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter specifically designed to be efficient with float data types.
     */
		public static FloatFrequencyCounter floatCounter(int maxCounters){
        return new FloatSpaceSaver(TABLE_HASH_FUNCTION,maxCounters);
		}

    /**
     * @param initialSize the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return a FrequencyCounter specifically designed to be efficient with float data types.
     */
	public static FloatFrequencyCounter floatCounter(int maxCounters, int initialSize){
		if (maxCounters < initialSize) throw new IllegalArgumentException("maxCounters must be >= initialSize");
		return new FloatSpaceSaver(TABLE_HASH_FUNCTION,maxCounters,initialSize, DEFAULT_LOAD_FACTOR);
	}

    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter specifically designed to be efficient with short data types.
     */
		public static ShortFrequencyCounter shortCounter(short maxCounters){
        return new ShortSpaceSaver(TABLE_HASH_FUNCTION,maxCounters);
		}

    /**
     * @param initialSize the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return a FrequencyCounter specifically designed to be efficient with short data types.
     */
	public static ShortFrequencyCounter shortCounter(short maxCounters, short initialSize){
		if (maxCounters < initialSize) throw new IllegalArgumentException("maxCounters must be >= initialSize");
		return new ShortSpaceSaver(TABLE_HASH_FUNCTION,maxCounters,initialSize, DEFAULT_LOAD_FACTOR);
	}

    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter specifically designed to be efficient with integer data types.
     */
	public static IntFrequencyCounter intCounter(int maxCounters){
		return new IntSpaceSaver(TABLE_HASH_FUNCTION,maxCounters);
	}

    /**
     * @param initialSize the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return a FrequencyCounter specifically designed to be efficient with integer data types.
     */
	public static IntFrequencyCounter intCounter(int maxCounters, int initialSize){
		if (maxCounters < initialSize) throw new IllegalArgumentException("maxCounters must be >= initialSize");
		return new IntSpaceSaver(TABLE_HASH_FUNCTION,maxCounters,initialSize, DEFAULT_LOAD_FACTOR);
	}

    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter specifically designed to be efficient with long data types.
     */
	public static LongFrequencyCounter longCounter(int maxCounters){
		return new LongSpaceSaver(TABLE_HASH_FUNCTION,maxCounters);
	}

    /**
     * @param initialSize the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return a FrequencyCounter specifically designed to be efficient with long data types.
     */
	public static LongFrequencyCounter longCounter(int maxCounters, int initialSize){
    	if (maxCounters < initialSize) throw new IllegalArgumentException("maxCounters must be >= initialSize");
		return new LongSpaceSaver(TABLE_HASH_FUNCTION,maxCounters,initialSize, DEFAULT_LOAD_FACTOR);
	}
    /**
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter geared towards Comparable data types. This is equivalent to
     * {@link #counter(java.util.Comparator, int)}, but it uses the compareTo() method defined
     * in the comparable itself.
     */
    public static <T extends Comparable<T>> FrequencyCounter<T> counter(int maxCounters){
        return new ObjectSpaceSaver<>(ComparableComparator.<T>newComparator(),TABLE_HASH_FUNCTION,maxCounters);
    }

    /**
     * @param initialCounters the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter geared towards Comparable data types. This is equivalent to
     * {@link #counter(java.util.Comparator, int)}, but it uses the compareTo() method defined
     * in the comparable itself.
     */
    public static <T extends Comparable<T>> FrequencyCounter<T> counter(int maxCounters,int initialCounters){
    	if (maxCounters < initialCounters) throw new IllegalArgumentException("maxCounters must be >= initialCounters");
        return new ObjectSpaceSaver<>(ComparableComparator.<T>newComparator(),TABLE_HASH_FUNCTION,maxCounters,initialCounters, DEFAULT_LOAD_FACTOR);
    }

    /**
     * @param comparator A comparator to use when comparing two items. This is mainly used for comparison
     *                   queries when using the build {@code FrequentElements} instance, but may be used for
     *                   other things.
     * @param initialCounters the initial number of counters to keep. When the size of the data set is known
     *                    to be very large, setting this to {@code maxCounters} will prevent some table resizing,
     *                    and therefore there will be some slight memory improvements. However, setting this
     *                    too high may result in wasted memory when there are not many elements in the stream.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter for items which has a well-defined ordering.
     */
    public static <T> FrequencyCounter<T> counter(Comparator<T> comparator, int maxCounters,int initialCounters){
    	if (maxCounters < initialCounters) throw new IllegalArgumentException("maxCounters must be >= initialCounters");
    	return new ObjectSpaceSaver<>(comparator,TABLE_HASH_FUNCTION,maxCounters,initialCounters, DEFAULT_LOAD_FACTOR);
    }

    /**
     * @param comparator A comparator to use when comparing two items. This is mainly used for comparison
     *                   queries when using the build {@code FrequentElements} instance, but may be used for
     *                   other things.
     * @param maxCounters the maximum number of counters to use. This means that the maximum number of
     *                    {@code Top-K} elements which can possibly recorded (within the bounds of the error
     *                    inherint in the <em>SpaceSaver</em> algorithm) is {@code maxCounters}--elements
     *                    outside of this are guaranteed (within an error metric) not to be included
     *                    in the resulting FrequentElements instance.
     * @return A FrequencyCounter for items which has a well-defined ordering.
     */
    public static <T> FrequencyCounter<T> counter(Comparator<T> comparator, int maxCounters){
        return new ObjectSpaceSaver<>(comparator,TABLE_HASH_FUNCTION,maxCounters);
    }


    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.BooleanFrequentElements}
     * to and from data streams and/or byte arrays.
     */
    public static Encoder<BooleanFrequentElements> booleanEncoder(){
        return SimpleBooleanFrequencyCounter.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.ByteFrequentElements}
     * to and from data streams and/or byte arrays.
     */
    public static Encoder<ByteFrequentElements> byteEncoder(){ return SwitchingByteEncoder.INSTANCE; }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.BytesFrequentElements}
     * to and from output streams and/or byte arrays. This will use the default
     * {@link com.splicemachine.primitives.ByteComparator} for use in necessary byte[] comparisons.
     */
    public static Encoder<BytesFrequentElements> byteArrayEncoder(){
        return BytesFrequentElements.newEncoder(Bytes.basicByteComparator());
    }

    /**
     * @param byteComparator the Byte comparison algorithm to use when comparing two byte arrays.
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.BytesFrequentElements}
     * to and from output streams and/or byte arrays.
     */
    public static Encoder<BytesFrequentElements> byteArrayEncoder(ByteComparator byteComparator){
        return BytesFrequentElements.newEncoder(byteComparator);
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.ShortFrequentElements}
     * to and from output streams and/or byte arrays.
     */
    public static Encoder<ShortFrequentElements> shortEncoder(){
        return ShortFrequentElements.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.IntFrequentElements}
     * to and from output streams and/or byte arrays.
     */
    public static Encoder<IntFrequentElements> intEncoder(){
        return IntFrequentElements.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.LongFrequentElements}
     * to and from output streams and/or byte arrays.
     */
    public static Encoder<LongFrequentElements> longEncoder(){
        return LongFrequentElements.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.DoubleFrequentElements}
     * to and from output streams and/or byte arrays.
     */
    public static Encoder<DoubleFrequentElements> doubleEncoder(){
        return DoubleFrequentElements.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.FloatFrequentElements}
     * to and from byte streams and/or byte arrays.
     */
    public static Encoder<FloatFrequentElements> floatEncoder(){
        return FloatFrequentElements.EncoderDecoder.INSTANCE;
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.FrequentElements}
     * to and from byte streams and/or byte arrays. This is equivalent to
     * {@link #objectEncoder(com.splicemachine.encoding.Encoder, java.util.Comparator)}, but it uses
     * the compareTo() method defined in the Comparable itself.
     */
    public static <T extends Comparable<T>> Encoder<FrequentElements<T>> objectEncoder(Encoder<T> valueEncoder){
        return new ObjectFrequentElements.EncoderDecoder<>(valueEncoder,ComparableComparator.<T>newComparator());
    }

    /**
     * @return an Encoder for converting a {@link com.splicemachine.stats.frequency.FrequentElements}
     * to and from byte streams and/or byte arrays.
     */
    public static <T> Encoder<FrequentElements<T>> objectEncoder(Encoder<T> valueEncoder,
                                                                 Comparator<? super T> comparator){
        return new ObjectFrequentElements.EncoderDecoder<>(valueEncoder,comparator);
    }

    /* ***************************************************************************************************************/
    /*private helper methods and classes*/

    private static class SwitchingByteEncoder implements Encoder<ByteFrequentElements>{
        /*
         * This is a convenience class to quickly encode ByteFrequentElements instances
         * without needing special encoders for each different type.
         */
        private static final Encoder<ByteFrequentElements> INSTANCE = new SwitchingByteEncoder();

        @Override
        public void encode(ByteFrequentElements item, DataOutput dataInput) throws IOException {
           if(item instanceof ByteFrequencies){
               dataInput.writeByte(0x00);
               ByteFrequencies.EncoderDecoder.INSTANCE.encode((ByteFrequencies)item,dataInput);
           }else if(item instanceof ByteHeavyHitters){
               dataInput.writeByte(0x01);
               ByteHeavyHitters.EncoderDecoder.INSTANCE.encode((ByteHeavyHitters)item,dataInput);
           } else throw new IllegalArgumentException("Cannot encode ByteFrequentElements of type "+ item.getClass());
        }

        @Override
        public ByteFrequentElements decode(DataInput input) throws IOException {
            byte b = input.readByte();
            if(b == 0x00)
                return ByteFrequencies.EncoderDecoder.INSTANCE.decode(input);
            else if(b==0x01)
                return ByteHeavyHitters.EncoderDecoder.INSTANCE.decode(input);
            else throw new IllegalArgumentException("Unknown type: "+ b);
        }
    }
}
