package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValType.sizeOf;

import com.dylibso.chicory.runtime.Instance.InstanceAwareObjectInputStream;
import com.dylibso.chicory.runtime.InterpreterMachine.PendingCall;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a frame, doesn't hold the stack, just local variables and the `pc` which
 * is the program counter in this function. Instead of keeping an absolute pointer
 * to positions in code the program counter is relative to the function and we store it
 * here so we know where to resume when we return from an inner function call.
 * This also means it's not possible to set the program counter to an instruction in another function
 * on accident, as this is not allowed in the spec. You can only jump to instructions
 * within the function you are in and only specific places.
 */
public class StackFrame implements Serializable{
	transient private List<AnnotatedInstruction> code;
    transient AnnotatedInstruction currentInstruction;
    public PendingCall pendingcall;
    private final int funcId;
    private int pc;
    public final long[] locals;
    public final ValType[] localTypes;
    private final int[] localIdx;
    private transient Instance instance;
	private void readObject(ObjectInputStream in)
	        throws IOException, ClassNotFoundException {
		if (in instanceof InstanceAwareObjectInputStream) {
	        this.instance = ((InstanceAwareObjectInputStream) in).getIns();
	    }
	    in.defaultReadObject();
	}
    private final List<CtrlFrame> ctrlStack = new ArrayList<>();
    public int UID;
	private Object readResolve() {
		//System.out.println("do");
		if(!isWASMFrame) {
	    	code= 	Collections.emptyList();
	    	 currentInstruction = null;//code.get(pc);
		}else {

            FunctionBody func = instance.function(funcId);
            code=func.instructions();
            currentInstruction = code.get(pc);
        }
	       
        return this;
    }
    public StackFrame(Instance instance, int funcId, long[] args) {
        this(
                instance,
                funcId,
                args,
                false);
    }
    private boolean isWASMFrame;
    StackFrame(
            Instance instance,
            int funcId,
            long[] args,
            boolean isWASMFrame) {
    	
    		UID=instance.UIDcounter++;
    	    this.isWASMFrame=isWASMFrame;
    	    List<AnnotatedInstruction>code;
            List<ValType> argsTypes ;
            List<ValType> localTypes ;
    	   
    	    if(!isWASMFrame) {
    	    	code= 	Collections.emptyList();
    	    	argsTypes= 	Collections.emptyList();
    	    	localTypes= 	Collections.emptyList();
    	    	}else {
             int typeId = instance.functionType(funcId);
             FunctionType typef = instance.type(typeId);
             FunctionBody func = instance.function(funcId);
           code=func.instructions();
            argsTypes = typef.params();
            localTypes = func.localTypes();
            }
        this.code = code;
        this.instance = instance;
        this.funcId = funcId;
        this.locals = Arrays.copyOf(args, sizeOf(argsTypes) + sizeOf(localTypes));
        int localsSize = argsTypes.size() + localTypes.size();
        this.localTypes = new ValType[localsSize];
        for (int i = 0; i < argsTypes.size(); i++) {
            this.localTypes[i] = argsTypes.get(i);
        }
        for (int i = 0; i < localTypes.size(); i++) {
            this.localTypes[argsTypes.size() + i] = localTypes.get(i);
        }
        this.localIdx = new int[localsSize];

        // initialize codesegment locals.
        int j = 0;
        for (var i = 0; i < localTypes.size(); i++) {
            ValType type = localTypes.get(i);
            var idx = j + sizeOf(argsTypes);
            if (!type.equals(ValType.V128)) {
                locals[idx] = Value.zero(type);
                j += 1;
            } else {
                locals[idx] = Value.zero(ValType.I64);
                locals[idx + 1] = Value.zero(ValType.I64);
                j += 2;
            }
        }

        // initialize local indexes
        j = 0;
        for (int i = 0; i < this.localTypes.length; i++) {
            this.localIdx[i] = j;
            if (!localType(i).equals(ValType.V128)) {
                j += 1;
            } else {
                j += 2;
            }
        }
    }

    void reset(long[] args) {
        for (int i = 0; i < locals.length; i++) {
            setLocal(i, args[i]);
        }
        pc = 0;
    }

    int funcId() {
        return funcId;
    }

    public ValType localType(int i) {
        return this.localTypes[i];
    }

    public int localIndexOf(int idx) {
        return this.localIdx[idx];
    }

    void setLocal(int i, long v) {
        this.locals[i] = v;
    }

    long local(int i) {
        return locals[i];
    }

    @Override
    public String toString() {
        var nameSec = instance.module().nameSection();
        var id = "[" + funcId + "]";
        if (nameSec != null) {
            var funcName = nameSec.nameOfFunction(funcId);
            if (funcName != null) {
                id = funcName + id;
            }
        }
        return id + "\n\tpc=" + pc + " locals=" + Arrays.toString(locals);
    }
    public int callState=0;
    AnnotatedInstruction loadCurrentInstruction() {
        currentInstruction = code.get(pc++);
        callState=0;
        return currentInstruction;
    }

    int currentPc() {
        return pc - 1;
    }

    boolean isLastBlock() {
        return currentInstruction.depth() == 0;
    }

    boolean terminated() {
        return pc >= code.size();
    }

    void pushCtrl(CtrlFrame ctrlFrame) {
        ctrlStack.add(ctrlFrame);
    }

    void pushCtrl(OpCode opcode, int startValues, int returnValues, int height) {
        ctrlStack.add(new CtrlFrame(opcode, startValues, returnValues, height));
    }

    void pushCtrl(OpCode opcode, int startValues, int returnValues, int height, int pc) {
        ctrlStack.add(new CtrlFrame(opcode, startValues, returnValues, height, pc));
    }

    int ctrlStackSize() {
        return ctrlStack.size();
    }

    CtrlFrame popCtrl() {
        var ctrlFrame = ctrlStack.remove(ctrlStack.size() - 1);
        return ctrlFrame;
    }

    CtrlFrame popCtrl(int n) {
        int mostRecentCallHeight = ctrlStack.size();
        while (true) {
            if (ctrlStack.get(--mostRecentCallHeight).opCode == OpCode.CALL) {
                break;
            }
        }
        var finalHeight = ctrlStack.size() - (mostRecentCallHeight + n + 1);
        CtrlFrame ctrlFrame = null;
        while (ctrlStack.size() > finalHeight) {
            ctrlFrame = popCtrl();
        }
        return ctrlFrame;
    }

    CtrlFrame popCtrlTillCall() {
        while (true) {
            var ctrlFrame = popCtrl();
            if (ctrlFrame.opCode == OpCode.CALL) {
                return ctrlFrame;
            }
        }
    }

    void jumpTo(int newPc) {
        pc = newPc;
    }

    static void doControlTransfer(CtrlFrame ctrlFrame, MStack stack) {
        var endResults = ctrlFrame.startValues + ctrlFrame.endValues;
        long[] returns = new long[endResults];
        ValType[] returnTypes = new ValType[endResults];
        
        for (int i = 0; i < returns.length; i++) {
            if (stack.size() > 0) {
                returnTypes[i] = stack.peekType(); 
                returns[i] = stack.pop();        
            }
        }

        while (stack.size() > ctrlFrame.height) {
            stack.pop(); 
        }

        for (int i = 0; i < returns.length; i++) {
            long value = returns[returns.length - 1 - i];
            ValType type = returnTypes[returns.length - 1 - i];
            stack.push(value, type);
        }
    }
}
