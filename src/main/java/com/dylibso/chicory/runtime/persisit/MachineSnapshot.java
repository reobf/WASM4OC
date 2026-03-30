package com.dylibso.chicory.runtime.persisit;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;

public class MachineSnapshot  implements Serializable {
    long[] mstack;    
    int mstackCount;
    List<FrameSnapshot> callStack;
}