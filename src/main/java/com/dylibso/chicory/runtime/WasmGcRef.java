package com.dylibso.chicory.runtime;

import java.io.Serializable;

/**
 * Marker interface for WasmGC heap objects (structs and arrays).
 */
public interface WasmGcRef extends  Serializable{
    int typeIdx();
}
