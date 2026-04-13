package reobf.wasm4oc.main.item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.http.client.entity.DeflateInputStream;

import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportGlobal;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.IntegerNormal;
import com.dylibso.chicory.runtime.IntegerVolatile;
import com.dylibso.chicory.runtime.IntegerWrapper;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.SyncCallRequestedException;
import com.dylibso.chicory.runtime.WasmArray;
import com.dylibso.chicory.runtime.WasmExternRef;
import com.dylibso.chicory.runtime.WasmFunctionHandle;
import com.dylibso.chicory.wasi.WasiExitException;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.ValType;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ArgumentsImpl;
import li.cil.oc.server.machine.Callbacks;
import li.cil.oc.server.machine.Callbacks.Callback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import reobf.wasm4oc.main.item.ItemCPU.APIEnv;
import scala.collection.Iterator;
import scala.collection.JavaConverters;
import scala.collection.Seq;
@Architecture.Name("wasm")
@Architecture.NoMemoryRequirements
public class Arch implements Architecture{
	Object lock=new Object();
	private static void loadDeps(
        WasmModule module,
        String moduleName,
        Map<String, Supplier<WasmModule>> nameToModuleSupplier,
        Store store,
        Set<String> instantiated, Collection<BiFunction<String,FunctionType, HostFunction>> dynf) {
    Set<String> needed = new LinkedHashSet<>();
    for (int i = 0; i < module.importSection().importCount(); i++) {
        var imp = module.importSection().getImport(i);
        //module.typeSection().getType(0).returns();
        String depName = imp.module();
        if (!instantiated.contains(depName)) {
            needed.add(depName);
        }
       
        if(imp instanceof  FunctionImport fi)
        if(imp.module().equals("env")) {
        	 var type=module.typeSection().getType(fi.typeIndex());
        	for(var dynff:dynf) {
        		
        		var get=dynff.apply(imp.name(),type);
        		if(get!=null) {
        			store.addFunction(get);
        			break;
        		}
        	}
        }
        
    }
    for (String depName : needed) {
        var supplier = nameToModuleSupplier.get(depName);
        if (supplier == null) {
            throw new RuntimeException("No module: " + depName);
        }
        var depModule = supplier.get();
        loadDeps(depModule, depName, nameToModuleSupplier, store, instantiated,dynf);
        store.instantiate(depName, imports -> Instance.builder(depModule).withStart(false).withImportValues(imports).build());
        instantiated.add(depName);
    }
}
	public static Instance instantiateWithDeps(
	        WasmModule main,
	        String mainName,
	        Map<String, Supplier<WasmModule>> nameToModuleSupplier,
	        WasiPreview1 wasi,ImportValues imp,Collection<BiFunction<String,FunctionType, HostFunction>> dynf,HostFunction... f) {

	    Store store = new Store();
	    store.addFunction(wasi.toHostFunctions());
	    store.addFunction(f);
	    store.addImportValues(imp);
	    Set<String> instantiated = new HashSet<>();
	    instantiated.add("wasi_snapshot_preview1"); 
	    instantiated.add("env"); 
	    //instantiated.add(mainName); 
	 
	    loadDeps(main, mainName, nameToModuleSupplier, store, instantiated,dynf);

	    return store.instantiate(mainName, imports -> Instance.builder(main).withStart(false).withImportValues(imports).build());
	}
	
	
    Machine machine;

    public Arch(Machine machine) {
        this.machine = /*(li.cil.oc.api.machine.Machine)*/ machine;

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
		onDead();
	}
	public boolean running;
	public byte[] prog;
	private Instance  instance;
	private HashMap<Long,Object> extval=new HashMap();
	private long extvalcount;
	private int gcCD;
	private APIEnv env;
	public long[] cstring(Instance i,long[] pointer) {
		return new long[] {extRef(i.memory().readCString((int) pointer[0]))};
	}
	public long[] string(Instance i,long[] pointer) {
		return new long[] {extRef(i.memory().readString((int) pointer[0], (int) pointer[1]))};
	}
	public WasmFunctionHandle wrap(BiFunction<Memory, Long, Object>n) {
		return (a,b)->{
			return new long[] {extRef(n.apply(a.memory(),b[0]))};
		};
	}
	public WasmFunctionHandle unwrap(Function<Number, Long>n) {
		return (a,b)->{
			Number nm=(Number) extval.get(((WasmExternRef)instance.gcRef((int)b[0])).value());
			return new long[] {n.apply(nm)};
		};
	}
	public long[] wasm_cstring(Instance i,long[] pointer) {
		int ref=(int) pointer[0];
		int malloc=(int) pointer[1];
		var str=(String)extval.get(((WasmExternRef)instance.gcRef( ref)).value());
		byte[] b=str.getBytes();
		InterpreterMachine im = (InterpreterMachine)instance.getMachine();
		im.precall(instance.table(0).requiredRef(malloc), new long[] {b.length+1}, null, true);
		boolean done=im.docall(new IntegerNormal(10000));
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
		boolean done=im.docall(new IntegerNormal(10000));
		if(done==false) {
			throw new RuntimeException("malloc stuck, process crashed!");
		}
		long[] ret=im.postcall();
		instance.memory().writeString(malloc, str);
		int retpx=((ByteBufferMemory)instance.memory()).javaMalloc(2*4);
		var bnox=((ByteBufferMemory)instance.memory()).getBlockNOffset(retpx);
		
		System.arraycopy(intArrayToByteArray(new int[] {(int) ret[0],b.length}), 0, bnox.block, 0, 2*4);
		
		
		
		
		return new long[] {retpx};
	}	
	public int extRef(Object forWAHT) {
		/*var get=extval.inverse().get(forWAHT);
		if(get!=null) {
			return get.intValue();
		}*/
		
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
		asyncCounter.set(0);
		synchronized (lock) {
			
		
		
		nbt.setBoolean("end", end);
		try {
			if(instance!=null) {
				
				
			if(asyncException!=null) {
				ByteArrayOutputStream b;
				try(var o=new ObjectOutputStream(b=new ByteArrayOutputStream())) {
				o.writeObject(asyncException);

				nbt.setByteArray("asyncException",b.toByteArray());
				}catch(Exception e) {
					
					try(var o2=new ObjectOutputStream(b=new ByteArrayOutputStream())) {
						o2.writeObject(new RuntimeException("Cannot serialize"));
						nbt.setByteArray("asyncException",b.toByteArray());	
					}
				}
			}
				
				
				
				nbt.setBoolean("requestForSyncCall", instance.requestForSyncCall);
				nbt.setInteger("opsCount",count.get());
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
			{
				NBTTagCompound t=new NBTTagCompound();
				ems_id_to_type.entrySet().forEach(s->{
					t.setInteger(""+s.getKey(), s.getValue().ordinal());
				});
				nbt.setTag("ems_id_to_type", t);
			}
			{
				NBTTagCompound t=new NBTTagCompound();
				invokerMap.entrySet().forEach(s->{
					t.setIntArray(""+s.getKey(), s.getValue().toInts());
				});
				nbt.setTag("invokerMap", t);
			}			
			{
				NBTTagCompound t=new NBTTagCompound();

				nbt.setInteger("handleCounter", handleCounter);
			}	
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			try (DeflaterOutputStream def = new DeflaterOutputStream(b);
			     ObjectOutputStream o = new ObjectOutputStream(def)) {
			    o.writeObject(handleToEMVAL);
			}
		
			nbt.setByteArray("handleToEMVAL", b.toByteArray());
			{
				NBTTagCompound t=new NBTTagCompound();
				handleToRefcount.entrySet().forEach(s->{
					t.setInteger(""+s.getKey(), s.getValue());
				});
				nbt.setTag("handleToRefcount", t);
			}							
			{
				NBTTagCompound t=new NBTTagCompound();
				dtorMap.entrySet().forEach(s->{
					t.setByteArray(""+s.getKey(),longsToBytes( s.getValue()));
				});
				nbt.setTag("dtorMap", t);
			}

			
			
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		}
		
	}	
	@Override
	public void load(NBTTagCompound nbt) {	
		asyncCounter.set(0);
	synchronized (lock) {
		end=nbt.getBoolean("end");
	if(nbt.getBoolean("hasContext")) {
		
		
		byte[] bs=nbt.getByteArray("asyncException");
		if(bs.length>0) {
			ObjectInputStream o;
			try (var on = new ObjectInputStream(new ByteArrayInputStream(bs))){
				
				asyncException=(Exception) on.readObject();
			} catch (Exception e) {	
			}
			
			
		}
		
	
		
		
		count.set(nbt.getInteger("opsCount"));
		{	ems_id_to_type.clear();
			NBTTagCompound t=(NBTTagCompound) nbt.getTag("ems_id_to_type");
			for(var s:t.func_150296_c()) {
				ems_id_to_type.put(Integer.valueOf(s), TYPE.values()[t.getInteger(s)]);
			}
		}
		{	
			invokerMap.clear();
		NBTTagCompound t=(NBTTagCompound) nbt.getTag("invokerMap");
		for(var s:t.func_150296_c()) {
			invokerMap.put(Integer.valueOf(s), Invoker.from(t.getIntArray(s)));
		}
		}
		{		
		handleCounter=nbt.getInteger("handleCounter");
		}
		
		{
			byte[] compressedData = nbt.getByteArray("handleToEMVAL");

			if (compressedData != null && compressedData.length > 0) {
			    try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
			         InflaterInputStream iis = new InflaterInputStream(bais);
			         ObjectInputStream ois = new ObjectInputStreamWithLoader(iis, this.getClass().getClassLoader())) {
			        
			    		handleToEMVAL = (BiMap<Integer, Object>) ois.readObject();

			    } catch (IOException | ClassNotFoundException e) {
			        e.printStackTrace();
			    }
			}
			
		}
		{	handleToRefcount.clear();
		NBTTagCompound t=(NBTTagCompound) nbt.getTag("handleToRefcount");
		for(var s:t.func_150296_c()) {
			handleToRefcount.put(Integer.valueOf(s), t.getInteger(s));
		}
	   
		}	
		{dtorMap.clear();
		NBTTagCompound t=(NBTTagCompound) nbt.getTag("dtorMap");
		for(var s:t.func_150296_c()) {
			dtorMap.put(Integer.valueOf(s),bytesToLongs( t.getByteArray(s)));
		}
		}
		
	try {
		byte[] bytes =decompress(nbt.getByteArray("context"));
		prog=nbt.getByteArray("prog");
		init();
		instance.requestForSyncCall=nbt.getBoolean("requestForSyncCall");
		
		instance.deser(bytes);
		
		
		extval.clear();
		byte[] extvalx=nbt.getByteArray("extval");
		extvalcount=nbt.getLong("extvalcount" );
		
		if(extvalx.length>0) {
			ObjectInputStream k=new ObjectInputStream(new InflaterInputStream(new ByteArrayInputStream(extvalx)));
			extval=new HashMap();
			extval.putAll((Map) k.readObject());//.add(Map<Long, Object>) k.readObject();
		}
		}catch(Exception e) {e.printStackTrace();}
	}
		
		
	}
		
	}
	Function<IntegerWrapper,IntegerWrapper> refill=s->{
		
		if(env.opsPerTick()*20>s.get()) {
			s.inc(Math.min(env.opsPerTick(),env.opsPerTick()*20-s.get()));
		}
		
		return s;};
	private int entryIndex;
	boolean end;
	
	@SuppressWarnings({ "unchecked", "unlikely-arg-type" })
	public void init() {try{WasmModule get = Parser.parse(new ByteArrayInputStream( prog));
	var options = WasiOptions.builder()
	        .withStdout(System.out)
	        .build();
	Map<String, Supplier<WasmModule>> deps = new HashMap<>();
	var wasi = WasiPreview1.builder().withOptions(options).build();
		
	List<HostFunction> cfs=new ArrayList<>();
	HostFunction cf=new HostFunction("env", "yield", FunctionType.of(Collections.emptyList(), Collections.emptyList()), 
			
			(i,a)->{
				yield[0]+=count.get();
				count.set(0);
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
	cfs.add(new HostFunction("env", "printJava", FunctionType.of(Collections.singletonList(ValType.ExternRef), Collections.EMPTY_LIST), 
			(a,b)->{
				long handle=b[0];
				
				if(a.gcRefs.get((int) handle) instanceof WasmExternRef m) {
				//WasmExternRef  m=(WasmExternRef) a.gcRefs.get((int) handle);
				System.out.println(extval.get(m.value()));
				env.disp.push(extval.get(m.value()).toString());
				}else if(a.gcRefs.get((int) handle) instanceof WasmArray m){
					String s="Array:"+
					new LongArrayList(m.elements()).longStream().mapToObj(sx-> 
					Objects.toString(
					extval.get(((WasmExternRef)instance.gcRef((int) sx)).value())
					)
							).collect(Collectors.joining(","));
					
					;
					System.out.println(s);
					env.disp.push(s);
				}
				
				
				return new long[0];
			}));	
	Collection<BiFunction<String,FunctionType, HostFunction>> dyn=new ArrayList();
	
	dyn.add((str,tp)->{
		if(str.startsWith("pack")) {
			
			try {
			int amount=Integer.valueOf( str.substring(4));
			return new HostFunction("env", str, FunctionType.of(Collections.nCopies(amount, ValType.ExternRef), Arrays.asList(ValType.ExternRef)), 
					(a,b)->{
						ArrayList arr=new ArrayList();
					
						for(long l:b) {
							arr.add(extval.get(((WasmExternRef)instance.gcRef((int) l)).value()));
							//if(instance.gcRef((int) l)==null)throw new RuntimeException();
						}
						
						return new long[] {extRef(arr)};
					});
			}catch(NumberFormatException  e) {}
			
		}
		
		
		return null;
	});
	cfs.add(new HostFunction("env", "type", FunctionType.of(Collections.singletonList(ValType.ExternRef), Arrays.asList(ValType.I32)), 
			(a,b)->{
				long handle=b[0];
				WasmExternRef  m=(WasmExternRef) a.gcRefs.get((int) handle);
				var value=(extval.get(m.value()));
				int ret=0;
				if(value instanceof Integer) {ret=1;}
				else if(value instanceof Long) {ret=2;}
				else if(value instanceof Float) {ret=3;}
				else if(value instanceof Double) {ret=4;}
				else if(value instanceof List) {ret=5;}
				else if(value instanceof Map) {ret=6;}
				return new long[] {ret};
			}));
	cfs.add(new HostFunction("env", "size", FunctionType.of(Collections.singletonList(ValType.ExternRef), Arrays.asList(ValType.I32)), 
			(a,b)->{
				long handle=b[0];
				WasmExternRef  m=(WasmExternRef) a.gcRefs.get((int) handle);

				return new long[] {((List)extval.get(m.value())).size()};
			}));
	cfs.add(new HostFunction("env", "get", FunctionType.of(Arrays.asList(ValType.ExternRef,ValType.I32), Arrays.asList(ValType.ExternRef)), 
			(a,b)->{
				long handle=b[0];
				WasmExternRef  m=(WasmExternRef) a.gcRefs.get((int) handle);

				Object o=((List)extval.get(m.value())).get((int) b[1]);

				return new long[] {extRef(o)};
			}));
	
	dyn.add((str,tp)->{
		if(str.equals("OC_invoke")) {
			
			try {
			var par=tp.params();
			
			if(par.size()==3&&
					par.get(0).equals(ValType.ExternRef)&&
					par.get(1).equals(ValType.ExternRef)&&
					par.get(2).equals(ValType.ExternRef))
			return new HostFunction("env", str, FunctionType.of(Arrays.asList(ValType.ExternRef,ValType.ExternRef,ValType.ExternRef), Arrays.asList(ValType.ExternRef)), 
					(a,b)->{
						WasmExternRef add=(WasmExternRef) instance.gcRef( (int) b[0]);
						String a0= extval.get(add.value()).toString();
						WasmExternRef fun=(WasmExternRef) instance.gcRef( (int) b[1]);
						String a1= extval.get(fun.value()).toString();
						
						WasmExternRef inarr=(WasmExternRef) instance.gcRef( (int) b[2]);
						List l=(List) extval.get(inarr.value());
					

						Object[] jo=l.toArray();

						try {
							Object[] result = machine.invoke(a0,a1,jo);
								
							for(int i=0;i<result.length;i++) {
								result[i]=toJavaMap( result[i]);
								
							}
							ArrayList retarr=new ArrayList( Arrays.asList(result));
							
							
							return new long[] {extRef(retarr)};

						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
							
						}
						
						
					});			
			
			
			if(par.size()==1&&par.get(0).equals(ValType.ExternRef))
			return new HostFunction("env", str, FunctionType.of(Arrays.asList(ValType.ExternRef), Arrays.asList(ValType.ExternRef)), 
					(a,b)->{
					
						WasmExternRef inarr=(WasmExternRef) instance.gcRef( (int) b[0]);
						List l=(List) extval.get(inarr.value());
					

						Object[] jo=l.toArray();

						try {
							Object[] result = machine.invoke(jo[0].toString(), jo[1].toString(), 
									Arrays.copyOfRange(jo, 2, jo.length)
									
									);
							for(int i=0;i<result.length;i++) {
								result[i]=toJavaMap( result[i]);
								
							}
							ArrayList retarr=new ArrayList( Arrays.asList(result));
							
							
							return new long[] {extRef(retarr)};

						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
							
						}
						
						
					});
			
			
			}catch(NumberFormatException  e) {}
			
		}
		
		
		return null;
	});
	
	/*
	cfs.add(new HostFunction("env", "OC_invoke", FunctionType.of(Arrays.asList(ValType.ExternRef), Arrays.asList(ValType.ExternRef)), 
			(a,b)->{
				
				WasmExternRef inarr=(WasmExternRef) instance.gcRef( (int) b[0]);
				List l=(List) extval.get(inarr.value());
			

				Object[] jo=l.toArray();

				try {
					Object[] result = machine.invoke(jo[0].toString(), jo[1].toString(), 
							Arrays.copyOfRange(jo, 2, jo.length)
							
							);
					for(int i=0;i<result.length;i++) {
						result[i]=toJavaMap( result[i]);
						
					}
					ArrayList retarr=new ArrayList( Arrays.asList(result));
					
					
					return new long[] {extRef(retarr)};

				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
					
				}
				
				
				//return new long[]{0};
				
			}));*/
	
	cfs.add(new HostFunction("env", "double", FunctionType.of(Arrays.asList(ValType.F64), Collections.singletonList(ValType.ExternRef)), 
			this.wrap((m,i)->Double.longBitsToDouble(i))));
	cfs.add(new HostFunction("env", "wasm_double", FunctionType.of(Arrays.asList(ValType.ExternRef), Collections.singletonList(ValType.F64)), 
			this.unwrap((i)->Double.doubleToRawLongBits(i.doubleValue()))));	
	cfs.add(new HostFunction("env", "float", FunctionType.of(Arrays.asList(ValType.F32), Collections.singletonList(ValType.ExternRef)), 
			this.wrap((m,i)->Float.intBitsToFloat(i.intValue()))));
	cfs.add(new HostFunction("env", "wasm_float", FunctionType.of(Arrays.asList(ValType.ExternRef), Collections.singletonList(ValType.F32)), 
			this.unwrap((i)->Float.floatToRawIntBits(i.floatValue())+0L)));	
	cfs.add(new HostFunction("env", "long", FunctionType.of(Arrays.asList(ValType.I64), Collections.singletonList(ValType.ExternRef)), 
			this.wrap((m,i)->(i))));
	cfs.add(new HostFunction("env", "wasm_long", FunctionType.of(Arrays.asList(ValType.ExternRef), Collections.singletonList(ValType.I64)), 
			this.unwrap((i)->(i.longValue()))));	
	cfs.add(new HostFunction("env", "int", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.ExternRef)), 
			this.wrap((m,i)->(long)(i).intValue())));
	cfs.add(new HostFunction("env", "wasm_int", FunctionType.of(Arrays.asList(ValType.ExternRef), Collections.singletonList(ValType.I32)), 
			this.unwrap((i)->i.longValue())));	
	cfs.add(new HostFunction("env", "string", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Collections.singletonList(ValType.ExternRef)), 
			this::string));
	
	cfs.add(new HostFunction("env", "cstring", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.ExternRef)), 
			this::cstring));	
	cfs.add(new HostFunction("env", "wasm_string", FunctionType.of(Arrays.asList(ValType.ExternRef,ValType.I32), Arrays.asList(ValType.I32)), 
			this::wasm_string));
	
	cfs.add(new HostFunction("env", "wasm_cstring", FunctionType.of(Arrays.asList(ValType.ExternRef,ValType.I32), Collections.singletonList(ValType.I32)), 
			this::wasm_cstring));		
	cfs.add(new HostFunction("env", "malloc", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) b[0])};
			}));		
	cfs.add(new HostFunction("env", "jmalloc", FunctionType.of(Arrays.asList(ValType.I32), Collections.singletonList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) b[0])};
			}));	
	cfs.add(new HostFunction("env", "free", FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(a,b)->{
				((ByteBufferMemory)instance.memory()).javaFree((int) b[0]);
				return new long[] {};
			}));
	cfs.add(new HostFunction("env", "jfree", FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(a,b)->{
				((ByteBufferMemory)instance.memory()).javaFree((int) b[0]);
				return new long[] {};
			}));	
	cfs.add(new HostFunction("env", "calloc", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) (b[0]*b[1]))};
				
			}));	
	cfs.add(new HostFunction("env", "jcalloc", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) (b[0]*b[1]))};
				
			}));	
	cfs.add(new HostFunction("env", "__libc_calloc", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaMalloc((int) (b[0]*b[1]))};
				
			}));	
	
	
	cfs.add(new HostFunction("env", "realloc", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaRealloc((int)b[0],(int)b[1])};
				
			}));
	
	cfs.add(new HostFunction("env", "jrealloc", FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(a,b)->{
				return new long[] {((ByteBufferMemory)instance.memory()).javaRealloc((int)b[0],(int)b[1])};
				
			}));	
	
	cfs.add(new HostFunction("env", "ops", FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(a,b)->{
			
				return new long[] {count.get()};
			}));	
	cfs.add(new HostFunction("env", "ticks", FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(a,b)->{
			
				return new long[] {env.ticks};
			}));	
	/*globalInstance= new GlobalInstance(com.dylibso.chicory.wasm.types.Value.i32(0), MutabilityType.Var) {
		
		@Override
		public ValType getType() {	
			setValue(count[0]);
			return super.getType();
		}
		
	};*/
	var imports = ImportValues.builder()
	    //.addGlobal(new ImportGlobal("env", "ops", globalInstance))
	    .build();
	
	Optional<StartSection> startSection = get.startSection();
	get.startSection=Optional.empty();
	int start=(int)(startSection.map(s->s.startIndex()).orElse(-1l).intValue());
cfs.addAll(getEms());
   instance = instantiateWithDeps(get, "main", deps, wasi,imports,dyn,cfs.toArray(new HostFunction[0]));	
   entryIndex=
			start!=-1?start:
			instance.getExports().get("_start").index();
   InterpreterMachine im=((InterpreterMachine)instance.getMachine());
   im.precall(entryIndex, new long[] {}, null,false);
	}catch(Exception e) {
		e.printStackTrace();
		env.disp.push(e.toString());
		prog=null;
		end=true;
	}}

	Thread r;
	boolean asyncfinished;
	Exception asyncException;
	IntegerVolatile asyncCounter=new IntegerVolatile(0);
	public void doJob() {
		if(instance==null&&prog!=null&&end==false) {
		init();
		}
		boolean finished=false;
		try {
		
		if(instance!=null) {
			InterpreterMachine im=((InterpreterMachine)instance.getMachine());
			//li.cil.oc.server.machine.Machine m=(li.cil.oc.server.machine.Machine) machine;
			//m.crash(null);machine.crash(null)
			//int pages=((ByteBufferMemory)instance.memory()).actualAlloc;
			//int size=pages*65536+prog.length;
			  //machine
			
				if(env.async())
				{
					if(r==null) {
						instance.nosynccall=true;
						r=new Thread(()->{
						while(true) {
						//Thread.yield();
						try {Thread.sleep(1);} catch (InterruptedException e) {}
						synchronized(lock) {	
						
						if(instance==null) {System.out.println("a");return;}
						if(instance.requestForSyncCall) {continue;}
						//if(running==false) {System.out.println("v");return;}
						asyncCounter.set(/*env.opsPerTick()*/20000);
						
						try {
						boolean k=im.docall(asyncCounter);
						if(k) {asyncfinished=true; System.out.println("vc");return;}
						}catch(Exception e) {
							asyncException=e;return;
						}
						}
						
							
						}
						
					});r.setDaemon(true);
						r.start();
						boolean a=r.isAlive();
						System.out.println(a);
					}
					if(asyncException!=null) {
						var tmp=asyncException;
						asyncException=null;
						throw tmp;
					}
					asyncCounter.set(0);
					synchronized(lock) {
						
						if(instance.requestForSyncCall) {
						instance.nosynccall=false;
						boolean k=im.docall(new IntegerNormal(64));
						instance.requestForSyncCall=false;
						instance.nosynccall=true;
						}
						/*if(!asyncfinished) {
						instance.nosynccall=false;
						instance.requestForSyncCall=false;
						boolean k=im.docall(new int[] {64});
						if(k) {asyncfinished=true;}
						instance.nosynccall=true;
						}
						if(asyncfinished) {finished=true;asyncfinished=false;}*/
					}
					
				}
					else {
			  long start=System.currentTimeMillis();
				int old=count.get();
				refill.apply(count);
				System.out.println(old+"->"+count.get());
				 finished=im.docall(count);
				count.inc(yield[0]);
				yield[0]=0;
				//System.out.println(old+"->"+count.get());
				//System.out.println((System.currentTimeMillis()-start));
				//System.out.println(MinecraftServer.getServer().getTickCounter());
				
					}
				if(finished) {
					
				/*instance=null;
				extval.clear();		
				invokerMap.clear();
				handleCounter=1000;
				handleToEMVAL.clear();
				handleToRefcount.clear();
				dtorMap.clear();
				extvalcount=0;*/
				onDead();
				
				
				
				
				
				long[] get=im.postcall();
				if(get.length>0)
				env.disp.push("Exit value:"+get[0]);
				else
				env.disp.push("Exit value:"+"None");
				
				end=true;
				}
				
				
				/*while(!im.docall(refill.apply(x))){
				
				
				//System.out.println(x[0]);
			}*/
			
			
		}
		
		
		}catch(Exception e) {
			if(e instanceof WasiExitException ee) {
				env.disp.push("Exit value:"+ee.exitCode());
				
			}else {
				env.disp.push("Error:"+e.toString());
			}
			e.printStackTrace();
			instance=null;
			onDead();
			end=true;
		}
				
				
				
	}
	
	public void onDead() {
		asyncCounter.set(0);
		if(r!=null) {
			try {
				r.join();
			} catch (InterruptedException e) {
			
			}
		}
		instance=null;
		extval.clear();
		invokerMap.clear();
		handleCounter=1000;
		handleToEMVAL.clear();
		handleToRefcount.clear();
		dtorMap.clear();
		extvalcount=0;
		count.set(env.opsPerTick()*20);
		prog=null;
		asyncfinished=false;
		r=null;
		
	}
//GlobalInstance globalInstance;// = new GlobalInstance(com.dylibso.chicory.wasm.types.Value.i32(0), MutabilityType.Var);
IntegerWrapper count=new IntegerNormal(0);
int[] yield=new int[1];
public static int[] byteArrayToIntArray(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    int[] ints = new int[bytes.length / 4];
    buffer.asIntBuffer().get(ints);
    return ints;
}
public static byte[] intArrayToByteArray(int[] ints) {
	ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4);
	buffer.asIntBuffer().put(ints);
	byte[] byteArray = buffer.array();
	return byteArray;
}

public static Object toJavaMap(Object o) {
    if (o == null) {
        return null;
    }
    if (o instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) o;
        HashMap<Object, Object> result = (map instanceof HashMap hm)?hm:new HashMap<>(map.size());

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = toJavaMap(entry.getKey());//map as key, really?    
            Object value = toJavaMap(entry.getValue()); 
            result.put(key, value);
        }
        return result;
    }

    // Scala mutable.Map
    if (o instanceof scala.collection.mutable.Map scalaMap) {
    		return toJavaMap(JavaConverters.mapAsJavaMapConverter(scalaMap));
    }

    // Scala immutable.Map
    if (o instanceof scala.collection.immutable.Map scalaMap) {
    		return toJavaMap(JavaConverters.mapAsJavaMapConverter(scalaMap));
    }

   
    return o;
}


//emscipten host
///////////////////////////////////////////////////////////////
//////////////////////////////


public Collection<HostFunction> getEms(){
	var all=new ArrayList<HostFunction>();
	all.add(new HostFunction("env", "posix_memalign", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(i,a)->{
				return posix_memalign(this.instance, a);}
			)
			);
	all.add(new HostFunction("env", "_emval_get_global", 
			FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList(ValType.I32)), 
			(i,a)->{
				return _emval_get_global(this.instance, a);}
			)
			);
	all.add(new HostFunction("env", "_emval_decref", 
			FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(i,a)->{
				 _emval_decref(this.instance, a);return new long[0];}
			)
			);	
	all.add(new HostFunction("env", "_emval_create_invoker", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32), Arrays.asList(ValType.I32)), 
			(i,a)->{
			return	_emval_create_invoker(this.instance, a);}
			)
			);		
	all.add(new HostFunction("env", "_emval_invoke", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32,ValType.I32,ValType.I32), Arrays.asList(ValType.F64)), 
			(i,a)->{
			return	_emval_invoke(this.instance, a,true);}
			)
			);		
	all.add(new HostFunction("env", "_emval_invoke_i64", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32,ValType.I32,ValType.I32), Arrays.asList(ValType.I64)), 
			(i,a)->{
			return	_emval_invoke(this.instance, a,false);}
			)
			);		
	all.add(new HostFunction("env", "_emval_run_destructors", 
			FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(i,a)->{
			_emval_run_destructors(this.instance, a);return new long[0];}
			)
			);			
	all.add(new HostFunction("env", "_embind_register_void", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_void(a);
				return new long[0];}
			)
			);		
	all.add(new HostFunction("env", "_embind_register_bool", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_bool(a);
				return new long[0];}
			)
			);	
	all.add(new HostFunction("env", "_embind_register_integer", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32,ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_integer(a);
				return new long[0];}
			)
			);		
	all.add(new HostFunction("env", "_embind_register_bigint", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32,ValType.I64,ValType.I64), Arrays.asList()), 
			(i,a)->{
				_embind_register_bigint(a);
				return new long[0];}
			)
			);		
	all.add(new HostFunction("env", "_embind_register_float", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_float(a);
				return new long[0];}
			)
			);	
	all.add(new HostFunction("env", "_embind_register_std_string", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_std_string(a);
				return new long[0];}
			)
			);	
	all.add(new HostFunction("env", "_embind_register_std_wstring", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_std_wstring(a);
				return new long[0];}
			)
			);	
	all.add(new HostFunction("env", "_embind_register_emval", 
			FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()), 
			(i,a)->{
				_embind_register_emval(a);
				return new long[0];}
			)
			);		
	all.add(new HostFunction("env", "_embind_register_memory_view", 
			FunctionType.of(Arrays.asList(ValType.I32,ValType.I32,ValType.I32), Arrays.asList()), 
			(i,a)->{
			
				return new long[0];}
			)
			);		
	
	all.add(new HostFunction("env", "_emval_incref",
		    FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList()),
		    (i,a) -> { _emval_incref(i,a); return new long[0]; }
		));

		all.add(new HostFunction("env", "_emval_new_cstring",
		    FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList(ValType.I32)),
		    (i,a) -> _emval_new_cstring(i,a)
		));

		all.add(new HostFunction("env", "_emval_get_property",
		    FunctionType.of(Arrays.asList(ValType.I32, ValType.I32), Arrays.asList(ValType.I32)),
		    (i,a) -> _emval_get_property(i,a)
		));

		all.add(new HostFunction("env", "_emval_new_u16string",
		    FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList(ValType.I32)),
		    (i,a) -> _emval_new_u16string(i,a)
		));

		all.add(new HostFunction("env", "_emval_set_property",
		    FunctionType.of(Arrays.asList(ValType.I32, ValType.I32, ValType.I32), Arrays.asList()),
		    (i,a) -> { _emval_set_property(i,a); return new long[0]; }
		));

		all.add(new HostFunction("env", "_emval_new_object",
		    FunctionType.of(Arrays.asList(), Arrays.asList(ValType.I32)),
		    (i,a) -> _emval_new_object(i,a)
		));

		all.add(new HostFunction("env", "_emval_typeof",
		    FunctionType.of(Arrays.asList(ValType.I32), Arrays.asList(ValType.I32)),
		    (i,a) -> _emval_typeof(i,a)
		));
		all.add(new HostFunction("env", "_emval_equals",
			    FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)),
			    (i,a) -> _emval_equals(i,a)
			));		
		
		all.add(new HostFunction("env", "_emval_strictly_equals",
			    FunctionType.of(Arrays.asList(ValType.I32,ValType.I32), Arrays.asList(ValType.I32)),
			    (i,a) -> _emval_strictly_equals(i,a)
			));			
	
	return all;
}




public enum TYPE{
	
	VOID(s->null,s->0L),
	BOOL(s->Boolean.valueOf(s==1),s->((Boolean)s)?1l:0l),
	INT(s->s.intValue(),s->((Integer)s)+0l),
	LONG(s->s.longValue(),s->((Long)s)),
	FLOAT(s->Float.intBitsToFloat(s.intValue()),s->Float.floatToRawIntBits((Float)s)+0l,true,false),
	DOUBLE(s->Double.longBitsToDouble(s.longValue()),s->Double.doubleToRawLongBits((Double)s)+0l,false,true),
	EMVAL((s,i)->i.handleToEMVAL.get(s.intValue()),(s,i)->new long[] {i.handleToEMVAL.inverse().getOrDefault(s, 4)}),
	String((s,i)->i.instance.memory().readStdString(s.intValue()),
			(s,i)->{
				String ss=((java.lang.String) s);
				int p=((ByteBufferMemory)i.instance.memory()).javaMalloc(ss.getBytes().length+4);
				i.instance.memory().writeStdString(p, ss);
				return new long[] {p,p};
			}
			),
	StringU16((s,i)->i.instance.memory().readStdStringU16(s.intValue()),
			(s,i)->{
				String ss=((java.lang.String) s);
				int p=((ByteBufferMemory)i.instance.memory()).javaMalloc(ss.getBytes(StandardCharsets.UTF_16LE).length+4);
				i.instance.memory().writeStdStringU16(p, ss);
				return new long[] {p,p};
			}),
	StringU32((s,i)->i.instance.memory().readStdStringU32(s.intValue()),
			(s,i)->{
				String ss=((java.lang.String) s);
				int p=((ByteBufferMemory)i.instance.memory()).javaMalloc(ss.getBytes(java.nio.charset.Charset.forName("UTF-32LE")).length+4);
				i.instance.memory().writeStdStringU32(p, ss);
				return new long[] {p,p};
			}),
	MEM((s,i)->null,(s,i)->null),//fixme
	;
	boolean isFloat;
	boolean isDouble;
	//Function<Long,Object> cv;
	BiFunction<Long,Arch,Object> cv2;
	BiFunction<Object,Arch,long[]> cv1;
	TYPE(Function<Long,Object> cv,Function<Object,Long> cvv){
		this(cv,cvv,false,false);
			}
	TYPE(Function<Long,Object> cv,Function<Object,Long> cvv,boolean f,boolean d){this.cv2=
			(a,b)->cv.apply(a);
			this.cv1=(a,b)->new long[] {cvv.apply(a)};
			this.isFloat=f;	this.isDouble=d;
			}
	TYPE(BiFunction<Long,Arch,Object> cv,BiFunction<Object,Arch,long[]> cvv){
		
		this.cv2=cv;this.cv1=cvv;
		
	}
}

HashMap<Integer,TYPE> ems_id_to_type=new HashMap<>();

public  long[] _embind_register_void(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.VOID);
	return null;}
public  long[] _embind_register_bool(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.BOOL);

	return null;}
public  long[] _embind_register_integer(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.INT);

	return null;}
public  long[] _embind_register_bigint(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.LONG);

	return null;}
public  long[] _embind_register_float(long[] a) {
	if(a[2]==2)
	ems_id_to_type.put((int)a[0], TYPE.FLOAT);
	if(a[2]==4)
	ems_id_to_type.put((int)a[0], TYPE.DOUBLE);
	return null;}
public  long[] _embind_register_double(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.DOUBLE);

	return null;}
public  long[] _embind_register_std_string(long[] a) {
	
	ems_id_to_type.put((int)a[0], TYPE.String);

	return null;}
public  long[] _embind_register_std_wstring(long[] a) {
	if(a[2]==2)
	ems_id_to_type.put((int)a[1], TYPE.StringU16);
	if(a[2]==4)
	ems_id_to_type.put((int)a[1], TYPE.StringU32);	

	return null;}
public  long[] _embind_register_emval(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.EMVAL);

	return null;}
public  long[] _embind_register_memory_view(long[] a) {
	ems_id_to_type.put((int)a[0], TYPE.MEM);

	return null;}


static class Invoker {
    int[] typeIds; // [returnTypeId, arg1TypeId, arg2TypeId, ...]
    int kind;      // 0=FUNCTION, 1=METHOD, 2=CONSTRUCTOR, 3=CAST

	public int[] toInts() {
		int[] ret=new int[typeIds.length+2];
		ret[0]=kind;
		ret[1]=typeIds.length;
		System.arraycopy(typeIds, 0, ret, 2, typeIds.length);
		return ret;
	}
	public static Invoker from(int[] b) {
		var v=new Invoker();
		v.kind=b[0];
		int len=b[1];
		v.typeIds=new int[len];
		System.arraycopy(b, 2, v.typeIds, 0,len);
		
		return v;
	}
}
static class OCHolder implements Externalizable{
	private li.cil.oc.api.machine.Value p;

	public OCHolder(li.cil.oc.api.machine.Value p) {
		this.p=p;
	}
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		NBTTagCompound tag=new NBTTagCompound();
		p.save(tag);
		var bytes=CompressedStreamTools.compress(tag);
		out.writeInt(bytes.length);
		out.write(bytes);
		out.writeUTF(p.getClass().getName());
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		byte[] b=new byte[in.readInt()];
		in.read(b);
		NBTTagCompound tag=CompressedStreamTools.func_152457_a(b, NBTSizeTracker.field_152451_a);
		try {
			Object o=Class.forName(in.readUTF()).newInstance();
			p=(li.cil.oc.api.machine.Value) o;
			p.load(tag);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
static class StringHolder implements Serializable{
String name;
String type;
	public StringHolder(String name,String type) {
		this.name=name;this.type=type;
	}}

long[] _emval_get_global(Instance inst, long[] args) {
    int namePtr = (int) args[0];
    String name = inst.memory().readCString(namePtr);
    return new long[]{ putEMVAL(new StringHolder(name,"global")) };
}


Map<Integer, Invoker> invokerMap = new HashMap<>();
int handleCounter = 1000;
BiMap<Integer, Object>  handleToEMVAL=  HashBiMap.create();
Map<Integer, Integer>  handleToRefcount= new HashMap<>();
int newHandle() { return handleCounter++; }

int putEMVAL(Object obj) {
	if(obj==null) { 
		handleToEMVAL.put(4, null);
		return 4;}
    Integer existing = handleToEMVAL.inverse().get(obj);
    if (existing != null) {
        handleToRefcount.merge(existing, 1, Integer::sum);
        return existing;
    }
    int h = newHandle();
    handleToEMVAL.put(h, obj);
    handleToRefcount.put(h, 1);
    return h;
}

// _emval_decref
long[] _emval_decref(Instance inst, long[] args) {
    int handle = (int) args[0];
    if (handle <= 8) return null; // reserved handles
    int rc = handleToRefcount.getOrDefault(handle, 0) - 1;
    if (rc <= 0) {
        handleToRefcount.remove(handle);
        handleToEMVAL.remove(handle);
    } else {
        handleToRefcount.put(handle, rc);
    }
    return null;
}

Map<Integer, long[]> dtorMap = new HashMap<>();


int packDestructor(long[] ptrs) {
 if (ptrs.length == 0) return 0;
 int h = newHandle();
 dtorMap.put(h, ptrs);
 return h;
}


long[] _emval_run_destructors(Instance inst, long[] args) {
 int handle = (int) args[0];
 if (handle == 0) return null;
 long[] ptrs = dtorMap.remove(handle);
 if (ptrs != null) {
     for (long ptr : ptrs) {
         if (ptr != 0) ((ByteBufferMemory)instance.memory()).javaFree((int) ptr);
     }
 }
 return null;
}
/*void free(int i){}
int malloc(int i){
	return 0;}*/
Object convert0(int typeId, long raw){
	
	return ems_id_to_type.get(typeId).cv2.apply(raw, this);
	}

long[] convert1(int returnTypeId, Object result){
	
	return ems_id_to_type.get(returnTypeId).cv1.apply(result, this);}

long[] _emval_invoke(Instance inst, long[] args, boolean f64) {
 int invokerHandle     = (int) args[0];
 int objectHandle      = (int) args[1];
 int methodNamePtr     = (int) args[2];
 int destructorsOutPtr = (int) args[3];
 int argsPtr           = (int) args[4];

 Invoker invoker = invokerMap.get(invokerHandle);
 Object owner = handleToEMVAL.get(objectHandle);
 String methodName = inst.memory().readCString(methodNamePtr);

 int argCount = invoker.typeIds.length - 1;
 Object[] invokeArgs = new Object[argCount];
 List<Long> dtorPtrs = new ArrayList<>();

 for (int i = 0; i < argCount; i++) {
     int typeId = invoker.typeIds[i + 1];
     long raw = inst.memory().readI64(argsPtr + i * 8);
     invokeArgs[i] = convert0(typeId, raw);
 }
 /*for (int j = 0; j < 32; j++) {
	    System.out.printf("%02x ", inst.memory().read(71664 + j));
	}*/
 Object result;
 a:if(invoker.kind==3){
	 if(ems_id_to_type.get(invoker.typeIds[0])==TYPE.EMVAL)
	 {result=cast(invokeArgs[0]);
	 break a;
	 }
	 if(ems_id_to_type.get(invoker.typeIds[1])==TYPE.EMVAL) {
	 long ret[]=castToNum(invokeArgs[0],invoker.typeIds[0],f64); 
	 if(ret.length>1) {
		 int dtorHandle = packDestructor(new long[] {ret[1]});
		 inst.memory().writeI32(destructorsOutPtr, dtorHandle);
	 }
	 
	 return  new long[] {ret[0]};
	 }
	 throw new RuntimeException();
 }else 
 result= invoke(owner, methodName, invokeArgs);
 putEMVAL(result);
 int returnTypeId = invoker.typeIds[0];
 long[] wireResult = convert1(returnTypeId, result);
 long wireValue  = wireResult[0];
 var rettp=ems_id_to_type.get(returnTypeId);
 if(f64) {
	 if((!rettp.isFloat)&&(!rettp.isDouble)) {
		 wireValue=  Double.doubleToRawLongBits((double)wireValue);
		// return value is a 32bit integer (i32/pointer/boolean) here
		// chicory converts the long to double bit-wise, emscipten convert double to int value-wise
		// So if it's not a float, do this to make emscipten happy.
		// int -> double -> double-bits
	 }
	 else if(ems_id_to_type.get(returnTypeId).isFloat) { 
		wireValue=  Double.doubleToRawLongBits(Float.intBitsToFloat((int) wireValue));
		// float-bits -> float -> double -> double-bits
	 }
 }
 
 
 long dtorPtr    = wireResult.length>1?wireResult[1]:0; 

 if (dtorPtr != 0) {
     dtorPtrs.add(dtorPtr);
 }



 long[] dtorArray = dtorPtrs.stream().mapToLong(Long::longValue).toArray();
 int dtorHandle = packDestructor(dtorArray);
 inst.memory().writeI32(destructorsOutPtr, dtorHandle);

 return new long[]{ wireValue};
}

private Object cast(Object object) {
	if(object==null)return null;
	return object.toString();
}
private long[] castToNum(Object object, int typeIds, boolean f64) {
	if(object==null)return new long[1];
	var t=ems_id_to_type.get(typeIds);
	if(t==TYPE.INT&&f64) {return new long[] {Double.doubleToRawLongBits(Integer.valueOf(object.toString()))};}
	if(t==TYPE.BOOL&&f64) {return new long[] {Double.doubleToRawLongBits(Boolean.valueOf(object.toString())?1:0)};}
	if(t==TYPE.DOUBLE&&f64) {return new long[] {Double.doubleToRawLongBits(Double.valueOf(object.toString()))};}
	if(t==TYPE.FLOAT&&f64) {return new long[] {Double.doubleToRawLongBits(Float.valueOf(object.toString()))};}
	if(t==TYPE.LONG&&!f64) {return new long[] {(Long.valueOf(object.toString()))};}
	if(t==TYPE.String&&f64) {
		var ss=object.toString();
		int p=((ByteBufferMemory)instance.memory()).javaMalloc(ss.getBytes().length+4);
		instance.memory().writeStdString(p, ss);
		
		//System.out.println("p=" + p);
		//System.out.println("len=" + instance.memory().readInt(p));
		//System.out.println("str=" + instance.memory().readStdString(p));
		//System.out.printf("byte at p+4: %02x%n", instance.memory().readByte(p + 4));
		
		return new long[] {Double.doubleToRawLongBits(p),p};
	}
	if(t==TYPE.StringU16&&f64) {
		var ss=object.toString();
		int p=((ByteBufferMemory)instance.memory()).javaMalloc(ss.getBytes().length*2+4);
		instance.memory().writeStdStringU16(p, ss);
		return new long[] {Double.doubleToRawLongBits(p),p};
	}
	if(t==TYPE.StringU32&&f64) {
		var ss=object.toString();
		int p=((ByteBufferMemory)instance.memory()).javaMalloc(ss.getBytes().length*4+4);
		instance.memory().writeStdStringU32(p, ss);
		return new long[] {Double.doubleToRawLongBits(p),p};
	}
	
	
	//return object.toString();
	throw new RuntimeException();
}
long[] _emval_create_invoker(Instance inst, long[] args) {
    int argCount = (int) args[0];
    int typesPtr = (int) args[1];
    int kind     = (int) args[2];

    int[] typeIds = new int[argCount];
    for (int i = 0; i < argCount; i++) {
        typeIds[i] = (int) inst.memory().readI32(typesPtr + i * 4);
    }

    Invoker invoker = new Invoker();
    invoker.typeIds = typeIds; // typeIds[0]=returnType, [1..]=argTypes
    invoker.kind = kind;

    int handle = newHandle();
    invokerMap.put(handle, invoker);
    return new long[]{ handle };
}

public Map arrayToMap(Object[] a) {
	HashMap<String,Object> m=new HashMap();
	
	for(int i=0;i<a.length;i++) {
		
		if(a[i] instanceof li.cil.oc.api.machine.Value p) {
			
			a[i]=new OCHolder(p);
			
		}
		m.put(""+i, a[i]);}
	
	return m;
}


Object invoke(Object owner, String method, Object[] args) {
	if(owner instanceof OCHolder h) {
		if(method==null) {
			//if(instance.nosynccall==true)throw new SyncCallRequestedException();
			
			
			
			
			return toJavaMap(arrayToMap(h.p.call(machine, new ArgumentsImpl(
					JavaConverters.asScalaBufferConverter(Arrays.<Object>asList(args))
					.asScala().seq()
					
					))));
		}
		
		
		try {
		
			
			
			li.cil.oc.server.machine.Machine mc=(li.cil.oc.server.machine.Machine) machine;
			//mc.methods(h.p);
			scala.collection.Map<String, Callback> map = Callbacks.apply(h.p);
			 var cb=map.get(method).get();
			 if(cb.annotation().direct()==false&&instance.nosynccall==true)throw new SyncCallRequestedException();
				
			 return toJavaMap(arrayToMap( convert( cb.apply(h.p, machine, new ArgumentsImpl( scala.collection.mutable.WrappedArray$.MODULE$.make(args))))));
				
			//return toJavaMap(arrayToMap(machine.invoke(h.p, method, args)));
		} 
		catch (SyncCallRequestedException e) {throw e;}
		catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}
	
if(owner instanceof StringHolder h) {
	if(h.type.equals("global")) {
		if(h.name.equals("proxy")) {
			return new StringHolder(args[0].toString(),"proxy");
		}
		if(h.name.equals("component")) {
			return new StringHolder(args[0].toString(),"component");
		}
		if(h.name.equals("print")) {
			env.disp.push(args[0].toString());
			System.out.println(args[0].toString());
			return null;
		}		
		if(h.name.equals("yield")) {
			yield[0]+=count.get();
			count.set(0);
			
			return null;
		}	
		
	}
	if(h.type.equals("component")) {
		if(method.equals("ofType")) {
		var name=h.name;
		ArrayList<String>add=new ArrayList<String>();
		var it=machine.node().network().nodes().iterator();
		while(it.hasNext()) {
			var get=it.next();
			if(get instanceof li.cil.oc.api.network.Component co) {
				if(co.name().equals(name)) {add.add(co.address());}
			}
		}
		return arrayToMap(add.toArray());
		}
		if(method.equals("list")) {
			var name=h.name;
			ArrayList<String>add=new ArrayList<String>();
			var it=machine.node().network().nodes().iterator();
			while(it.hasNext()) {
				var get=it.next();
				/*if(get instanceof li.cil.oc.api.network.Component co) {
					if(co.name().equals(name)) {add.add(co.address());}
				}*/
				add.add(get.address());
			}
			return arrayToMap(add.toArray());
			
		}
		
	}
	if(h.type.equals("proxy")) {
		try {
			var get=env.node().network().node(h.name);
			if(get instanceof li.cil.oc.server.network.Component c) {
				//li.cil.oc.api.machine.Callback cb=c.annotation(method);
				scala.collection.Map<String, Callback> map=c.li$cil$oc$server$network$Component$$callbacks();
				var cb=map.get(method).get();
				if(cb.annotation().direct()==false&&instance.nosynccall==true)throw new SyncCallRequestedException();
				
				return toJavaMap(arrayToMap( convert( cb.apply(c.host(), machine, new ArgumentsImpl( scala.collection.mutable.WrappedArray$.MODULE$.make(args))))));
				//toJavaMap(arrayToMap(c.invoke(method, machine, args)));
				//c.invoke(method, machine, (Seq)null);
			}
			
			//var gets=Registry.converters().iterator().next();
			
			//gets.
			
			
			throw new AssertionError();
			//return  toJavaMap(arrayToMap(machine.invoke(h.name, method, args)));
		} catch (SyncCallRequestedException e) {throw e;}
		
		catch (Exception e) {
			e.printStackTrace();
			
			var fun=instance.exports().function("throw");
			if(fun!=null) {
				byte[] message=e.getMessage().getBytes();
				int ptr=((ByteBufferMemory)instance.memory()).javaMalloc(message.length+1);
				((ByteBufferMemory)instance.memory()).writeCString(ptr, e.getMessage());
				fun.apply(ptr);
			}
			
			throw new RuntimeException(e);
		}
	}
	/*try {
		return  toJavaMap(machine.invoke(h.name, method, args));
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	}*/
	
}
	/*
System.out.println(owner instanceof StringHolder);
System.out.println(owner.getClass());
System.out.println(StringHolder.class);
System.out.println(owner.getClass().getClassLoader());
System.out.println(StringHolder.class.getClassLoader());
*/
    throw new RuntimeException("invoke: unknown method " + method + " on " + owner);
}



long[] _emval_incref(Instance inst, long[] args) {
    int handle = (int) args[0];
    if (handle <= 8) return new long[0];
    handleToRefcount.merge(handle, 1, Integer::sum);
    return new long[0];
}
long[] _emval_new_cstring(Instance inst, long[] args) {
    int ptr = (int) args[0];
    String s = inst.memory().readCString(ptr);
    return new long[]{ putEMVAL(s) };
}
long[] _emval_get_property(Instance inst, long[] args) {
    int objHandle = (int) args[0];
    int keyHandle = (int) args[1];
    
    Object obj = handleToEMVAL.get(objHandle);
    Object key = handleToEMVAL.get(keyHandle);
    Object v = ((Map) obj).get(key.toString());
     return new long[]{ putEMVAL(v) };
}

long[] _emval_new_u16string(Instance inst, long[] args) {
    int ptr = (int) args[0];
    String s = inst.memory().readCStringU16(ptr);
    return new long[]{ putEMVAL(s) };
}
long[] _emval_set_property(Instance inst, long[] args) {
    int objHandle   = (int) args[0];
    int keyHandle   = (int) args[1];
    int valueHandle = (int) args[2];

    Object obj   = handleToEMVAL.get(objHandle);
    Object key   = handleToEMVAL.get(keyHandle);
    Object value = handleToEMVAL.get(valueHandle);
    Map<Object, Object> map = (Map<Object, Object>) obj;
    map.put(key.toString(), value);
    return new long[0];
}
long[] _emval_new_object(Instance inst, long[] args) {
    return new long[]{ putEMVAL(new HashMap<String, Object>()) };
}
long[] _emval_typeof(Instance inst, long[] args) {
    int handle = (int) args[0];
    Object obj = handleToEMVAL.get(handle);
    String type;
    if (obj == null)           type = "null";
    else if (obj instanceof Boolean)  type = "boolean";
    else if (obj instanceof Integer)   type = "int";
    else if (obj instanceof Long)   type = "long";
    else if (obj instanceof String)   type = "String";
    else if (obj instanceof Float)   type = "float";
    else if (obj instanceof Double)   type = "double";
    else if (obj instanceof Map)   type = "Map";
    else                              type = "object";
     return new long[]{ putEMVAL(type) };
}
public static byte[] longsToBytes(long[] longs) {
    if (longs == null) return null;
    ByteBuffer buf = ByteBuffer.allocate(longs.length * 8);
    buf.asLongBuffer().put(longs);
    return buf.array();
}
public static long[] bytesToLongs(byte[] bytes) {
    if (bytes == null) return null;
    if (bytes.length % 8 != 0) {
        throw new IllegalArgumentException();
    }
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    long[] longs = new long[bytes.length / 8];
    buf.asLongBuffer().get(longs);
    return longs;
}
long[] _emval_equals(Instance inst, long[] args) {
    int handle = (int) args[0];
    Object obj = handleToEMVAL.get(handle);
    int handlex = (int) args[1];
    Object objx = handleToEMVAL.get(handlex);    

    boolean b=Objects.equals(obj.toString(), objx.toString());
     return new long[]{ b?1:0 };
}
long[] _emval_strictly_equals(Instance inst, long[] args) {
    int handle = (int) args[0];
    Object obj = handleToEMVAL.get(handle);
    int handlex = (int) args[1];
    Object objx = handleToEMVAL.get(handlex);    

    boolean b=Objects.equals(obj,objx);
     return new long[]{ b?1:0 };
}

static ArrayList<li.cil.oc.api.driver.Converter> allcvs;
static HashSet<Class<?>> donottouch=new HashSet<>();
static {
	donottouch.add(Integer.class);
	donottouch.add(String.class);
	donottouch.add(Long.class);
	donottouch.add(Float.class);
	donottouch.add(Double.class);
	donottouch.add(Boolean.class);
	donottouch.add(Short.class);
	donottouch.add(Character.class);
}
// scala is shit, use java impl
@SuppressWarnings("unchecked")
public Object[] convert(Object[] in) {
	
	
	if(allcvs==null)allcvs=new ArrayList( JavaConverters.asJavaCollectionConverter(Registry.converters()).asJavaCollection());
	HashMap map=new HashMap();
	for (int i=0;i<in.length;i++) {
		var value=in[i];
		if(value==null||donottouch.contains(value.getClass()))continue;
		boolean suc=false;
		for(var cv:allcvs) {
			cv.convert(value, map);
			//if(map.isEmpty()==false) {suc=true;break;}
		}
		if(map.isEmpty()==false) {
			in[i]=map;
			convert(map);
			map=new HashMap();
		}
	
	}
	return in;
}

public Map<?,?> convert(Map<?,?> in) {
	
	
	//if(allcvs==null)allcvs=new ArrayList( JavaConverters.asJavaCollectionConverter(Registry.converters()).asJavaCollection());
	HashMap map=new HashMap();
	var it=in.entrySet().iterator();
	
	while(it.hasNext()) {
		
		boolean suc=false;
		Entry en=it.next();
		var value=en.getValue();
		if(value==null||donottouch.contains(value.getClass()))continue;
		for(var cv:allcvs) {
			cv.convert(value, map);
			//if(map.isEmpty()==false) {suc=true;break;}
		}
		if(map.isEmpty()==false) {
			en.setValue((Object)map);
			map=new HashMap();
		}
		
		
	}
	return in;
}
long[] posix_memalign(Instance inst, long[] args) {
    int memptrPtr = (int) args[0]; 
    int alignment = (int) args[1];
    int size      = (int) args[2];

  
    int ptr =   ((ByteBufferMemory)instance.memory()).posix_memalign(size, alignment);
    

    inst.memory().writeI32(memptrPtr, ptr);
    
    return new long[]{ 0 }; // 0 = 成功
}
}
