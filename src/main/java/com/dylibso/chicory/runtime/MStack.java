package com.dylibso.chicory.runtime;

import java.io.Serializable;

import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.ValueType;

public class MStack implements Serializable{
    public static final int MIN_CAPACITY = 8;

    private int count;
    public long[] elements;
    public ValType[] types;
    
    public MStack() {
        this.elements = new long[MIN_CAPACITY];
        this.types = new ValType[MIN_CAPACITY];
    }

    private void increaseCapacity() {
        final int newCapacity = elements.length << 1;

        final long[] array = new long[newCapacity];
        System.arraycopy(elements, 0, array, 0, elements.length);

        elements = array;
     

        final ValType[] arrayx = new ValType[newCapacity];
        System.arraycopy(types, 0, arrayx, 0, types.length);

        types = arrayx;
    }

    // internal use only!
    public long[] array() {
        return elements;
    }
    public void push(long v) {
    	// do not really care about the difference between non-ref types
    	// use i32 to mark it as non-ref type
    	push(v,ValType.I32);
    }
    
    public void push(long v,ValType type/*for all non-ref type, just pass i32*/) {
        elements[count] = v;
        types[count] = type;
        count++;

        if (count == elements.length) {
            increaseCapacity();
        }
    }

    public long pop() {
        count--;
        return elements[count];
    }
    public ValType peekType() {
        return types[count - 1];
    }
    public long peek() {
        return elements[count - 1];
    }

    public int size() {
        return count;
    }
}
