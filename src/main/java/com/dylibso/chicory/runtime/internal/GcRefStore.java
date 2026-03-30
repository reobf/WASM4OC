package com.dylibso.chicory.runtime.internal;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.WasmArray;
import com.dylibso.chicory.runtime.WasmGcRef;
import com.dylibso.chicory.runtime.WasmStruct;
import com.dylibso.chicory.runtime.Instance.InstanceAwareObjectInputStream;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.MStack;
import com.dylibso.chicory.runtime.StackFrame;
import com.dylibso.chicory.wasm.types.FieldType;
import com.dylibso.chicory.wasm.types.StorageType;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Store for GC-managed references keyed by auto-assigned integers.
 *
 * <p>Uses epoch-based deferred collection: refs are never swept during wasm
 * execution. Collection only happens at <em>safe points</em> — between
 * top-level calls — when the wasm operand stack and all call frames are
 * empty. At that point the only roots are globals and tables.
 */
public class GcRefStore implements Serializable{

    /**
     * GC ref IDs start at this offset to avoid collisions with externref
     * values that get internalized via any.convert_extern. Since internalized
     * externrefs and GC refs both live in the ANY hierarchy, they share the
     * same integer representation space.
     */
    public static final int ID_OFFSET = 0x10000;

    private static final int SWEEP_INTERVAL = 16;
	private void readObject(ObjectInputStream in)
	        throws IOException, ClassNotFoundException {
		if (in instanceof InstanceAwareObjectInputStream) {
	        this.instance = ((InstanceAwareObjectInputStream) in).getIns();
	    }
	    in.defaultReadObject();
	}
    transient private Instance instance;
    private final Map<Integer, WasmGcRef> map = new HashMap<>();
    private int nextId = ID_OFFSET;
    private int allocsSinceLastSweep;
    private boolean sweepRequested;

    public GcRefStore(Instance instance) {
        this.instance = instance;
    }

    /** Inserts a value with an automatically assigned key. */
    public int put(WasmGcRef value) {
        int id = nextId++;
        map.put(id, value);
        allocsSinceLastSweep++;
        if (allocsSinceLastSweep >= SWEEP_INTERVAL) {
            sweepRequested = true;
        }
        
        // do not sweep the new ref, as it's not yet put to MStack
        safePoint(id);
     
        return id;
    }

    /** Retrieves a value by key, or null if missing. */
    public WasmGcRef get(int key) {
        return map.get(key);
    }

    /** Called at safe points (between top-level calls). 
     * @param id */
    public void safePoint(int... id) {
        if (sweepRequested) {
            sweep(id);
            sweepRequested = false;
            allocsSinceLastSweep = 0;
        }
    }

    /** Checks whether a raw reference value is a GC ref ID. */
    public static boolean isGcRefId(long val) {
        return val >= ID_OFFSET && val != Value.REF_NULL_VALUE && !Value.isI31(val);
    }

    private void sweep(int... idp) {
        Set<Integer> reachable = new HashSet<>();
        for(int ii:idp) {reachable.add(ii);}
        // 1. Scan globals
        int globalCount = instance.globalCount();
        for (int i = 0; i < globalCount; i++) {
            var g = instance.global(i);
            
            if (g != null) {
                markIfGcRef(g.getType(),g.getValueLow(), reachable);
            }
        }

        // 2. Scan tables
        int tableCount = instance.tableCount();
        for (int i = 0; i < tableCount; i++) {
            var table = instance.table(i);
            if (table != null) {
                for (int j = 0; j < table.size(); j++) {
                    markIfGcRef(table.elementType(),table.ref(j), reachable);
                }
            }
        }
        
      //	 3. scan frame
       for(StackFrame stack: ((InterpreterMachine)instance.getMachine()).callStack) {
    	 
	    	   for(int i=0;i<stack.localTypes.length;i++) {
	    		   
		    	   var type=stack.localType(i);
		    	   long val=stack.locals[ stack.localIndexOf(i)];
		    	   if(type.isReference()) {
		    		   markIfGcRef(type,val, reachable);
		    		   
		    	   }
		    	   
	    	   }
    	   
       }
        
       // 4. scan stack
       MStack stack = ((InterpreterMachine)instance.getMachine()).stack;
       for(int i=0;i<stack.elements.length;i++) {
    	   var val=stack.elements[i];
    	   var tp=stack.types[i];
	    	  if(tp!=null&&tp.isReference()) {
		    		   markIfGcRef(tp,val, reachable);
		    }
		}       
      
        // 3. Remove unreachable entries
        map.keySet().removeIf(id -> {
        		//System.out.println(map.get(id));
       return 	!reachable.contains(id);
        	
        });
    }

    private void markIfGcRef(ValType valType, long val, Set<Integer> reachable) {
        if(!valType.isReference())return;
    		if (!isGcRefId(val)) {
            return;
        }
        int id = (int) val;
        if (!reachable.add(id)) {
            return; // already visited — prevents infinite loops in cyclic structures
        }
        WasmGcRef ref = map.get(id);
        if (ref == null) {
            return;
        }
        // Recursively trace nested refs
        if (ref instanceof WasmStruct) {
            var s = (WasmStruct) ref;
            var fieldTypes = instance.module().typeSection()
            	    .getSubType(s.typeIdx()).compType().structType().fieldTypes();
            for(int ix=0;ix<fieldTypes.length;ix++) {
            StorageType 	storageType=fieldTypes[ix].storageType();
            if (storageType.packedType() != null) continue;
            ValType atype = storageType.valType();
	         
            if(atype != null && atype.isReference()) {
	                markIfGcRef(atype,s.field(ix), reachable);
            }
            }
        } else if (ref instanceof WasmArray) {
            var a = (WasmArray) ref;
            var storageType = instance.module().typeSection()
            	    .getSubType(a.typeIdx()).compType().arrayType().fieldType().storageType();

            	// packed 类型(i8/i16)肯定不是 ref，直接跳过
            	if (storageType.packedType() != null) return; // 或 break，整个数组都不是 ref

            	ValType atype = storageType.valType();
            	if (atype != null && atype.isReference()) {
            	    for (int i = 0; i < a.length(); i++) {
            	        markIfGcRef(atype,a.get(i), reachable);
            	    }
            	}
        }
    }
}
