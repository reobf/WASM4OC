package reobf.wasm4oc.main.item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.ValType;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
@Architecture.Name("wasm")
@Architecture.NoMemoryRequirements
public class Arch implements Architecture{private static void loadDeps(
        WasmModule module,
        String moduleName,
        Map<String, Supplier<WasmModule>> nameToModuleSupplier,
        Store store,
        Set<String> instantiated) {
    Set<String> needed = new LinkedHashSet<>();
    for (int i = 0; i < module.importSection().importCount(); i++) {
        var imp = module.importSection().getImport(i);
        String depName = imp.module();
        if (!instantiated.contains(depName)) {
            needed.add(depName);
        }
    }
    for (String depName : needed) {
        var supplier = nameToModuleSupplier.get(depName);
        if (supplier == null) {
            throw new RuntimeException("No module: " + depName);
        }
        var depModule = supplier.get();
        loadDeps(depModule, depName, nameToModuleSupplier, store, instantiated);
        store.instantiate(depName, imports -> Instance.builder(depModule).withStart(false).withImportValues(imports).build());
        instantiated.add(depName);
    }
}
	public static Instance instantiateWithDeps(
	        WasmModule main,
	        String mainName,
	        Map<String, Supplier<WasmModule>> nameToModuleSupplier,
	        WasiPreview1 wasi,HostFunction... f) {

	    Store store = new Store();
	    store.addFunction(wasi.toHostFunctions());
	    store.addFunction(f);
	   
	    Set<String> instantiated = new HashSet<>();
	    instantiated.add("wasi_snapshot_preview1"); 
	    instantiated.add("env"); 
	    //instantiated.add(mainName); 
	 
	    loadDeps(main, mainName, nameToModuleSupplier, store, instantiated);

	    return store.instantiate(mainName, imports -> Instance.builder(main).withStart(false).withImportValues(imports).build());
	}
	
	
    Machine machine;

    public Arch(Machine machine) {
        this.machine = (li.cil.oc.api.machine.Machine) machine;

    }
	@Override
	public boolean isInitialized() {
		
		return true;
	}

	@Override
	public boolean recomputeMemory(Iterable<ItemStack> components) {
	
		return true;
	}

	@Override
	public boolean initialize() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void close() {
		running=false;
		instance=null;
		prog=null;
	}
	public boolean running;
	public byte[] prog;
	private Instance  instance;
	@Override
	public void runSynchronized() {
	
		running=true;
	}

	@Override
	public ExecutionResult runThreaded(boolean isSynchronizedReturn) {
	
		return new ExecutionResult.SynchronizedCall();
	}

	@Override
	public void onSignal() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnect() {
        for(var n:machine.node().network()
	            .nodes()) {
	        		if(n.host() instanceof ItemCPU.APIEnv m) {
	        		m.arch=this;
	        		}	
	        		
        }
		
	}


	public static byte[] decompress(byte[] compressed) throws Exception {
	    Inflater inflater = new Inflater();
	    inflater.setInput(compressed);

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];
	    while (!inflater.finished()) {
	        int count = inflater.inflate(buffer);
	        baos.write(buffer, 0, count);
	    }
	    inflater.end();
	    return baos.toByteArray();
	}
	public static byte[] compress(byte[] input) {
	    Deflater deflater = new Deflater();
	    deflater.setInput(input);
	    deflater.finish();

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];
	    while (!deflater.finished()) {
	        int count = deflater.deflate(buffer);
	        baos.write(buffer, 0, count);
	    }
	    deflater.end();
	    return baos.toByteArray();
	}
	@Override
	public void save(NBTTagCompound nbt) {
		try {
			byte[] bytes =compress(instance.ser());
			nbt.setByteArray("context", bytes);
			nbt.setByteArray("prog", prog);
			nbt.setBoolean("hasContext", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
	}	
	@Override
	public void load(NBTTagCompound nbt) {
	if(nbt.getBoolean("hasContext")) {
	try {
		byte[] bytes =decompress(nbt.getByteArray("context"));
		prog=nbt.getByteArray("prog");
		init();
		instance.deser(bytes);
		
		}catch(Exception e) {e.printStackTrace();}
	}
		
		
		
		
	}
	Function<int[],int[]> refill=s->{s[0]=100;return s;};
	private int entryIndex;
	
	public void init() {try{WasmModule get = Parser.parse(new ByteArrayInputStream( prog));
	var options = WasiOptions.builder()
	        .withStdout(System.out)
	        .build();
	Map<String, Supplier<WasmModule>> deps = new HashMap<>();
	var wasi = WasiPreview1.builder().withOptions(options).build();
		
int[] x=new int[1];
	HostFunction cf=new HostFunction("env", "yield", FunctionType.of(Collections.emptyList(), Collections.emptyList()), 
			
			(i,a)->{
				x[0]=0;
				return new long[0];}
			
			);
	
	HostFunction cf2=new HostFunction("env", "print", FunctionType.of(Collections.singletonList(ValType.I32), Collections.emptyList()), 
			
			(i,a)->{
				System.out.println(a[0]);
				return new long[0];}
			
			);
	
	
	
	Optional<StartSection> startSection = get.startSection();
	get.startSection=Optional.empty();
	int start=(int)(startSection.map(s->s.startIndex()).orElse(-1l).intValue());

   instance = instantiateWithDeps(get, "main", deps, wasi,cf,cf2);	
   entryIndex=
			start!=-1?start:
			instance.getExports().get("_start").index();
   InterpreterMachine im=((InterpreterMachine)instance.getMachine());
   im.precall(entryIndex, new long[] {}, null,false);
	}catch(Exception e) {
		e.printStackTrace();prog=null;
		
	}}
	public void doJob() {
		if(instance==null&&prog!=null) {
		init();
		}
		
		if(instance!=null) {
			  InterpreterMachine im=((InterpreterMachine)instance.getMachine());
				int[] x=new int[] {100};
				boolean finished=im.docall(refill.apply(x));
				if(finished) {instance=null;}
				/*while(!im.docall(refill.apply(x))){
				
				
				//System.out.println(x[0]);
			}*/
			
			
		}
		
		
		
				
				
				
	}

}
