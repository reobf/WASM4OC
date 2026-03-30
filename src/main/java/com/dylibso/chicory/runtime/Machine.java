package com.dylibso.chicory.runtime;

import java.io.Serializable;

import com.dylibso.chicory.wasm.ChicoryException;

@FunctionalInterface
public interface Machine extends Serializable{

    long[] call(int funcId, long[] args) throws ChicoryException;
}
