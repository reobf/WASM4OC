package com.dylibso.chicory.wasm.types;

import java.io.Serializable;

public class TagType implements Serializable{
    private final byte attribute;
    private final int typeIdx;

    public TagType(byte attribute, int typeIdx) {
        this.attribute = attribute;
        this.typeIdx = typeIdx;
    }

    public byte attribute() {
        return attribute;
    }

    public int typeIdx() {
        return typeIdx;
    }
}
