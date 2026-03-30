package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.File;

public class Main {

	public static void main(String[] args) {
		WasmModule get = Parser.parse(new File("C:\\Users\\zyf\\Desktop\\wasm\\wasm-interpreter-on-java\\src\\test\\resources\\jp\\hisano\\wasm\\interpreter\\spec\\i32\\i32.0.wasm"));
		Instance instance = Instance.builder(get)
			    .withStart(false)  
			    .build();
		instance.exportsRaw().forEach((a,b)->System.out.println(a));
		instance.export("add").apply(null);
	}

}
