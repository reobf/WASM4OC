package com.dylibso.chicory.runtime;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import com.dylibso.chicory.runtime.Instance.InstanceAwareObjectInputStream;

public class WasmException extends RuntimeException {
    private final int tagIdx;
    private final long[] args;
    transient private  Instance instance;
	private void readObject(ObjectInputStream in)
	        throws IOException, ClassNotFoundException {
		if (in instanceof InstanceAwareObjectInputStream) {
	        this.instance = ((InstanceAwareObjectInputStream) in).getIns();
	    }
	    in.defaultReadObject();
	}
    public WasmException(Instance instance, int tagIdx, long[] args) {
        this.instance = instance;
        this.tagIdx = tagIdx;
        this.args = args.clone();
        this.setStackTrace(new StackTraceElement[0]);
    }

    public Instance instance() {
        return instance;
    }

    public int tagIdx() {
        return tagIdx;
    }

    public long[] args() {
        return args;
    }
}
