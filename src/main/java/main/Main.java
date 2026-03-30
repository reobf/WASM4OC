package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.zip.GZIPOutputStream;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.InterpreterMachine;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.runtime.WasmArray;

import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.Value;

public class Main {
	private static void loadDeps(
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
	
	
	public static void main0(String[] args) throws IOException, ClassNotFoundException {
		int ix=0;
		WasmModule get = Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\b.wasm"));
		var options = WasiOptions.builder()
		        .withStdout(System.out)
		        .build();
		Map<String, Supplier<WasmModule>> deps = new HashMap<>();
		
		//deps.put("utils", () -> Parser.parse(new File("utils.wasm")));

		var wasi = WasiPreview1.builder().withOptions(options).build();
			
		
		byte[] b;
		{
		
		Optional<StartSection> startSection = get.startSection();
		get.startSection=Optional.empty();
		int start=(int)(startSection.map(s->s.startIndex()).orElse(-1l).intValue());
		
		
		Instance instance = instantiateWithDeps(get, "main", deps, wasi);
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());
		{
			int index=
					start!=-1?start:
					instance.getExports().get("_start").index();
			
			
			im.precall(index, new long[] {}, null,false);
			int[] x=new int[1];
			int maxtry=10;
			while(!im.docall(x=new int[] {100})){
				if(maxtry--<0)break;
				System.out.println(x[0]+" "+(ix++));
			}
			
		}
		
		
		
		b=instance.ser();
		   try (var out = new ByteArrayOutputStream();
			         var gzip = new GZIPOutputStream(out)) {
			        gzip.write(b);
			        gzip.finish();
			        System.out.println(out.toByteArray().clone().length);
			    }
		
		}
		System.out.println(b.length);
		System.out.println("----读取---");
		{
		

		Instance instance = instantiateWithDeps(get, "main", deps, wasi);
		instance.deser(b);
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());//.call(index, new long[] {});
		int[] x;
		
		while(!im.docall(x=new int[] {500})){
			
			System.out.println(x[0]+" "+(ix++));
		}
		long[] a=im.postcall();
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());//.call(index, new long[] {});
		{
			int index=instance.getExports().get("main").index();
			im.precall(index, new long[] {}, null,false);
			int[] x=new int[1];
			int ix=0;
			while(!im.docall(new int[1],x=new int[] {50})){
				
				System.out.println(x[0]+" "+(ix++));
			}
			long[] a=im.postcall();
			
			}
		}		
		*/
		
		
		
		}
	
	}
	
	public static void main2(String[] args) throws IOException, ClassNotFoundException {
		int ix=0;
		WasmModule get = Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\k\\a.wasm"));
		var options = WasiOptions.builder()
		        .withStdout(System.out)
		        .build();
		Map<String, Supplier<WasmModule>> deps = new HashMap<>();
		//deps.put("b", () -> Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\k\\b.wasm")));
		//deps.put("a", () -> Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\k\\a.wasm")));
		var wasi = WasiPreview1.builder().withOptions(options).build();
			
		
		byte[] b;
		{
		
		

		Instance instance = instantiateWithDeps(get, "a", deps, wasi);
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());
		{
			int index=instance.getExports().get("_start").index();
			im.precall(index, new long[] {}, null,false);
			int[] x=new int[1];
			int maxtry=10;
			while(!im.docall(x=new int[] {300})){
				if(maxtry--<0)break;
				System.out.println("序号"+(ix++)+" 使用步数"+(300-x[0]));
			}
			
		}
		
		
		System.out.println("----保存---");
		b=instance.ser();	
		
		System.out.println("长度"+b.length);
		   try (var out = new ByteArrayOutputStream();
			         var gzip = new GZIPOutputStream(out)) {
			        gzip.write(b);
			        gzip.finish();
			        System.out.println("压缩后长度"+out.toByteArray().clone().length);
			    }
		
		}
		
		System.out.println("----读取---");
		{
		

		Instance instance = instantiateWithDeps(get, "main", deps, wasi);
		instance.deser(b);
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());//.call(index, new long[] {});
		int[] x;
		
		while(!im.docall(x=new int[] {300})){
			
			System.out.println("序号"+(ix++)+" 使用步数"+(300-x[0]));
		}
		long[] a=im.postcall();

		
		
		
		}
	
	}
	public static void main(String[] args0) throws IOException, ClassNotFoundException, InterruptedException {
	
		Process process = new ProcessBuilder(
				"C:\\Users\\zyf\\Desktop\\ems\\wasm-as.exe", "-", "-o", "-", "--enable-gc","--enable-reference-types"
			).start();

		process.getOutputStream().write(Files.readAllBytes(new File("C:\\Users\\zyf\\Desktop\\ems\\loop.wasm").toPath()));
			//process.getOutputStream().write(Files.readAllBytes(Path.of(null)));
			process.getOutputStream().close();

			byte[] errors = process.getErrorStream().readAllBytes();
			byte[] result = process.getInputStream().readAllBytes();
			process.waitFor();
			System.out.println(new String(errors));
		System.out.println(new String(result));
		//Wat2Wasm .parse(new File("C:\\Users\\zyf\\Desktop\\ems\\loop.wat"));
		
		
		WasmModule get = Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\loop.wasm"));
		var options = WasiOptions.builder()
		        .withStdout(System.out)
		        .build();
		Map<String, Supplier<WasmModule>> deps = new HashMap<>();
		//deps.put("b", () -> Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\k\\b.wasm")));
		//deps.put("a", () -> Parser.parse(new File("C:\\Users\\zyf\\Desktop\\ems\\k\\a.wasm")));
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
		
		/////////////////////////
		/*var logString = new HostFunction(
			    "teavm", "logString",
			    FunctionType.of(List.of(ValType.I32), List.of()),
			    (Instance inst, long[] args) -> {
			        int ptr = (int) args[0];
			        // TeaVM 的字符串是 GC 对象，ptr 是 gcRef handle
			        // 暂时先打印 handle 值，需要进一步了解 TeaVM 的字符串编码
			        System.err.println("[teavm] logString handle=" + ptr);
			        return null;
			    }
			);

			var logInt = new HostFunction(
			    "teavm", "logInt",
			    FunctionType.of(List.of(ValType.I32), List.of()),
			    (Instance inst, long... args) -> {
			        int val = (int) args[0];
			        System.err.println("[teavm] logInt=" + val);
			        return null;
			    }
			);

			var logOutOfMemory = new HostFunction(
			    "teavm", "logOutOfMemory",
			    FunctionType.of(List.of(), List.of()),
			    (Instance inst, long... args) -> {
			        System.err.println("[teavm] OUT OF MEMORY");
			        return null;
			    }
			);

			var putwcharsOut = new HostFunction(
				    "teavm", "putwcharsOut",
				    FunctionType.of(List.of(ValType.I32, ValType.I32), List.of()),
				    (Instance inst, long... args) -> {
				        int ptr = (int) args[0];
				        int len = (int) args[1];
				        // 直接从线性内存读字符
				        byte[] bytes = inst.memory().readBytes(ptr, len * 2); // UTF-16，每字符2字节
				        StringBuilder sb = new StringBuilder();
				        for (int i = 0; i < len; i++) {
				            int lo = bytes[i * 2] & 0xFF;
				            int hi = bytes[i * 2 + 1] & 0xFF;
				            sb.append((char)(lo | (hi << 8)));
				        }
				        System.out.print(sb);
				        return null;
				    }
				);

			var currentTimeMillis = new HostFunction(
			    "teavm", "currentTimeMillis",
			    FunctionType.of(List.of(), List.of(ValType.F64)),
			    (Instance inst, long... args) -> {
			        double millis = (double) System.currentTimeMillis();
			        return new long[]{ Double.doubleToRawLongBits(millis) };
			    }
			);
		*/
		////////////////////
		
		Function<int[],int[]> refill=s->{s[0]=1;return s;};
		Optional<StartSection> startSection = get.startSection();
		get.startSection=Optional.empty();
		int start=(int)(startSection.map(s->s.startIndex()).orElse(-1l).intValue());
		Instance instance = instantiateWithDeps(get, "a", deps, wasi,cf,
				cf2
				
				
				);
		InterpreterMachine im=((InterpreterMachine)instance.getMachine());
		{
			int index=
					start!=-1?start:
					instance.getExports().get("_start").index();
			im.precall(index, new long[] {}, null,false);
			
			long l=System.currentTimeMillis();
			int i=0;
			while(!im.docall(refill.apply(x))){
				i+=100;
				//System.out.println((1.0*i)/((System.currentTimeMillis()-l)));
				
				//System.out.println(x[0]);
			}
			
		
		
		long[] a=im.postcall();

		
		
		
		}
	
	}

}
