package reobf.wasm4oc.main.item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.http.client.entity.DeflateInputStream;

import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportGlobal;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.WasmExternRef;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.ValType;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import reobf.wasm4oc.main.item.ItemCPU.APIEnv;
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
	        WasiPreview1 wasi,ImportValues imp,HostFunction... f) {

	    Store store = new Store();
	    store.addFunction(wasi.toHostFunctions());
	    store.addFunction(f);
	    store.addImportValues(imp);
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
		extval.clear();
		prog=null;
	}
	public boolean running;
	public byte[] prog;
	private Instance  instance;
	private Map<Long,Object> extval=new HashMap<>();
	private long extvalcount;
	private int gcCD;
	private APIEnv env;
	public long[] cstring(Instance i,long[] pointer) {
		return new long[] {extRef(i.memory().readCString((int) pointer[0]))};
	}
	public long[] string(Instance i,long[] pointer) {
		return new long[] {extRef(i.memory().readString((int) pointer[0], (int) pointer[1]))};
	}
	public long[] wasm_cstring(Instance i,long[] pointer) {
		int ref=(int) pointer[0];
		int malloc=(int) pointer[1];
		var str=(String)extval.get(((WasmExternRef)instance.gcRef( ref)).value());
		byte[] b=str.getBytes();
		InterpreterMachine im = (InterpreterMachine)instance.getMachine();
		im.precall(instance.table(0).requiredRef(malloc), new long[] {b.length+1}, null, true);
		boolean done=im.docall(new int[] {10000});
		if(done==false) {
			throw new RuntimeException("malloc stuck, process crashed!");
		}
		long[] ret=im.postcall();
		instance.memory().writeCString((int)ret[0], str);
		return new long[] {ret[0]};
	}
	public long[] wasm_string(Instance i,long[] pointer) {
		int ref=(int) pointer[0];
		int malloc=(int) pointer[1];
		var str=(String)extval.get(((WasmExternRef)instance.gcRef( ref)).value());
		byte[] b=str.getBytes();
		InterpreterMachine im = (InterpreterMachine)instance.getMachine();
		im.precall(instance.table(0).requiredRef(malloc), new long[] {b.length}, null, true);
		boolean done=im.docall(new int[] {10000});
		if(done==false) {
			throw new RuntimeException("malloc stuck, process crashed!");
		}
		long[] ret=im.postcall();
		instance.memory().writeString(malloc, str);
		return new long[] {ret[0],b.length};
	}	
	public int extRef(Object forWAHT) {
		extvalcount++;
		//extval.put(extvalcount, forWAHT);
		
		if(gcCD++>128) {gcCD=0;
		LongArrayList ii=new LongArrayList();
		for(var v:instance.gcRefs.map.values()) {
			if(v instanceof WasmExternRef e) {
				ii.add(e.value());
			}
		}
		int size=extval.size();
		extval.keySet().retainAll(ii);
		System.out.println("Ext gc:"+size+"->"+extval.size());
		
		}
		extval.put(extvalcount, forWAHT);
		return instance.registerGcRef(new WasmExternRef(extvalcount));
	}
	
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
	        		this.env=m;
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
			if(instance!=null) {
			byte[] or;
			byte[] bytes =compress(or=instance.ser());
			nbt.setByteArray("context", bytes);
			nbt.setByteArray("prog", prog);
			nbt.setBoolean("hasContext", true);
			System.out.println(bytes.length+"/"+or.length);
			
			nbt.setLong("extvalcount", extvalcount);
			byte[] extvalx=nbt.getByteArray("extval");
			ObjectOutputStream os=new ObjectOutputStream(new DeflaterOutputStream(new ByteArrayOutputStream()));
			//ObjectOutputStream k=new ObjectInputStream(new DeflateInputStream(new ByteArrayInputStream(extvalx)));
			//extval=(Map<Long, Object>) k.readObject();
			os.writeObject(extval);
			}
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
		
		
		extval.clear();
		byte[] extvalx=nbt.getByteArray("extval");
		extvalcount=nbt.getLong("extvalcount" );
		
		if(extvalx.length>0) {
			ObjectInputStream k=new ObjectInputStream(new InflaterInputStream(new ByteArrayInputStream(extvalx)));
			extval=(Map<Long, Object>) k.readObject();
		}
		}catch(Exception e) {e.printStackTrace();}
	}
		
		
		
		
	}
	Function<int[],int[]> refill=s->{s[0]=1000;return s;};
	private int entryIndex;
	
	public void init() {try{WasmModule get = Parser.parse(new ByteArrayInputStream( prog));
	var options = WasiOptions.builder()
	        .withStdout(System.out)
	        .build();
	Map<String, Supplier<WasmModule>> deps = new HashMap<>();
	var wasi = WasiPreview1.builder().withOptions(options).build();
		
	List<HostFunction> cfs=new ArrayList<>();
	HostFunction cf=new HostFunction("env", "yield", FunctionType.of(Collections.emptyList(), Collections.emptyList()), 
			
			(i,a)->{
				count[0]=0;
				return new long[0];}
			
			);
	cfs.add(cf);
	HostFunction cf2=new HostFunction("env", "print", FunctionType.of(Collections.singletonList(ValType.I32), Collections.emptyList()), 
			
			(i,a)->{
				System.out.println(a[0]);
				env.disp.push(a[0]+"");
				return new long[0];}
			
			);
	cfs.add(cf2);
	cfs.add(new HostFunction("env", "printJava", FunctionType.of(Collections.singletonList(ValType.I32), Collections.EMPTY_LIST), 
			(a,b)->{
				long handle=b[0];
				WasmExternRef  m=(WasmExternRef) a.gcRefs.get((int) handle);
				System.out.println(extval.get(m.value()));
				env.disp.push(extval.get(m.value()).toString());
				return new long[0];
			}));	
	cfs.add(new HostFunction("env", "string", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Collections.singletonList(ValType.I32)), 
			this::string));
	
	cfs.add(new HostFunction("env", "cstring", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.I32)), 
			this::cstring));	
	cfs.add(new HostFunction("env", "wasm_string", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32,ValType.I32)), 
			this::wasm_string));
	
	cfs.add(new HostFunction("env", "wasm_cstring", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Collections.singletonList(ValType.I32)), 
			this::wasm_cstring));		
	cfs.add(new HostFunction("env", "malloc", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) b[0])};
			}));		
	cfs.add(new HostFunction("env", "free", FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(a,b)->{
				((ByteBufferMemory)instance.memory()).javaFree((int) b[0]);
				return new long[] {};
			}));		
	
	
	globalInstance= new GlobalInstance(com.dylibso.chicory.wasm.types.Value.i32(0), MutabilityType.Var);
	var imports = ImportValues.builder()
	    .addGlobal(new ImportGlobal("env", "ops", globalInstance))
	    .build();
	
	Optional<StartSection> startSection = get.startSection();
	get.startSection=Optional.empty();
	int start=(int)(startSection.map(s->s.startIndex()).orElse(-1l).intValue());

   instance = instantiateWithDeps(get, "main", deps, wasi,imports,cfs.toArray(new HostFunction[0]));	
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
		
		try {
		
		if(instance!=null) {
			  InterpreterMachine im=((InterpreterMachine)instance.getMachine());
				long start=System.nanoTime();
				boolean finished=im.docall(refill.apply(count));
				
				System.out.println((System.nanoTime()-start)/1000);
				System.out.println(MinecraftServer.getServer().getTickCounter());
				if(finished) {instance=null;extval.clear();}
				/*while(!im.docall(refill.apply(x))){
				
				
				//System.out.println(x[0]);
			}*/
			
			
		}
		
		
		}catch(Exception e) {
			
			e.printStackTrace();
			instance=null;
			extval.clear();
			prog=null;
		}
				
				
				
	}
GlobalInstance globalInstance;// = new GlobalInstance(com.dylibso.chicory.wasm.types.Value.i32(0), MutabilityType.Var);
int[] count=new int[1];
}
