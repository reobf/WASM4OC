package reobf.wasm4oc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableSet;

public class EmsdkUtils {
	static File folder;
	
	static void unpackEmsdk() {
		if(folder.exists()&&folder.list().length>2/*some hidden file?*/) {
			 System.out.println("Not empty, skip.");
			return;
			}
		var get = EmsdkUtils.class.getResourceAsStream("/assets/wasm4oc/win/emsdk.zip");
		if (get != null) {
		    try (var zip = new java.util.zip.ZipInputStream(get)) {
		        java.util.zip.ZipEntry entry;
		        while ((entry = zip.getNextEntry()) != null) {
		         
		            String name = entry.getName();
		            int slash = name.indexOf('/');
		            if (slash < 0) { zip.closeEntry(); continue; } 
		            String stripped = name.substring(slash + 1); 
		            if (stripped.isEmpty()) { zip.closeEntry(); continue; }
		            File target = new File(folder, stripped);
		            if (entry.isDirectory()) {
		                target.mkdirs();
		            } else {
		                target.getParentFile().mkdirs();
		                try (var out = new java.io.FileOutputStream(target)) {
		                    zip.transferTo(out);
		                }
		            }
		            zip.closeEntry();
		        }
		    } catch (IOException e) {
				
				e.printStackTrace();
			}
		} else {
		    System.out.println("No emsdk packed.");
		}
		
	}
	public static void init(File folder0) {
		folder=folder0;
		unpackEmsdk();
		pythondir =findPython(folder);
		System.out.println("Python3:"+pythondir);
		
	}
	public static String pythondir;
	

	
	static String findPython(File folder) {
	    List<File> candidates = new ArrayList<>();
	    candidates.add(new File("python3"));
	    candidates.add(new File("python"));//system py

	    File[] searchDirs = {
	        new File(folder, "python"),
	        new File(folder.getParentFile(), "python")
	    };
	    
	    for (File dir : searchDirs) {
	        if (!dir.exists()) continue;

	        collectPythonFiles(dir, candidates);
	    }
	    

	    
	    
	    for (File f : candidates) {
	        String cmd = f.isAbsolute() ? f.getAbsolutePath() : f.getName();
	        String result = testPython(cmd);
	        if (result != null) return result;
	    }
	    return null;
	}
	static final Set<String> SKIP_DIRS = ImmutableSet.of(
		  "include", "lib", "libs", "site-packages", "dist-packages","scripts"
		 );
	static void collectPythonFiles(File dir, List<File> result) {
	    File[] files = dir.listFiles();
	    if (files == null) return;
	    for (File f : files) {
	        if (f.isDirectory()) {
	        	 if (SKIP_DIRS.contains(f.getName().toLowerCase())) continue;
	            collectPythonFiles(f, result);
	        } else {
	            String name = f.getName().toLowerCase();
	            if (name.equals("python") || name.equals("python3") ||
	                name.equals("python.exe") || name.equals("python3.exe")) {
	                result.add(f);
	            }
	        }
	    }
	}

	static String testPython(String cmd) {
	    try {
	        Process p = new ProcessBuilder(cmd, "--version").start();
	        String output = new String(p.getInputStream().readAllBytes()).trim();
	        if (output.isEmpty()) {
	            output = new String(p.getErrorStream().readAllBytes()).trim();
	        }
	        p.waitFor();
	        if (output.startsWith("Python 3.")) {
	            String[] parts = output.split("\\.");
	            int minor = Integer.parseInt(parts[1]);
	            return minor >= 6 ? cmd : null;
	        }
	    } catch (Exception ignored) {}
	    return null;
	}
	static String hijackmalloc=
	"""
	#include <cstddef>
	extern "C" {
	// import host functions
	__attribute__((import_module("env"), import_name("jmalloc")))
	extern void* jmalloc(size_t size);
	__attribute__((import_module("env"), import_name("jfree")))
	extern void jfree(void* ptr);
	__attribute__((import_module("env"), import_name("jcalloc")))
	extern void* jcalloc(size_t a,size_t b);	
	__attribute__((import_module("env"), import_name("jrealloc")))
	extern void* jrealloc(void* ptr,size_t newsize);	
	// malloc&free cannot be extern, or the em++ compiler will complain
	void* malloc(size_t size) {
		return jmalloc(size); 
	}
	void free(void* ptr) {
		jfree(ptr);
	}
	void* calloc(size_t a,size_t b) {
		return jcalloc(a,b); 
	}	
	void* realloc(void* ptr,size_t newsize) {
		return jrealloc(ptr,newsize); 
	}	
	
	
	}
	
	""";
	
	
	static Boolean EmccAvailableCahce;
	public static boolean isSysEmccAvailable() {
		if(EmccAvailableCahce==null) {
	    try {
	        Process process = new ProcessBuilder("emcc", "--version")
	            .start();
	        int exitCode = process.waitFor();
	        EmccAvailableCahce= exitCode == 0;
	    } catch (Exception e) {
	    	EmccAvailableCahce=false;
	    }
	    }
		return EmccAvailableCahce;
	}
	static public String compile(String[] args, boolean type, byte[] b) {
		if(pythondir==null) {return "0";}
	    File tempDir = new File(folder, "temp");
	    tempDir.mkdirs();
	    long oneHourAgo = System.currentTimeMillis() - 3600_000;
	    File[] old = tempDir.listFiles();
	    if (old != null) {
	        for (File f : old) {
	            if (f.lastModified() < oneHourAgo) f.delete();
	        }
	    }

	    String token = Long.toHexString(System.currentTimeMillis());
	    File p = new File(folder, type?"upstream/emscripten/em++.py":"upstream/emscripten/emcc.py");
	    File p2 = new File(tempDir, token + ".wasm");
	    File fail = new File(tempDir, token + ".fail");

	    List<String> argsl=new ArrayList<String>();
	    argsl.addAll(Arrays.asList(args));
	    if(argsl.contains("--host-malloc")||argsl.contains("-hm")) {
	    	argsl.remove("--host-malloc");
	    	argsl.remove("-hm");
	    	argsl.add("-sMALLOC=none");
	    	argsl.add("-sERROR_ON_UNDEFINED_SYMBOLS=0");
	    	b=ArrayUtils.addAll(hijackmalloc.getBytes(), b);
	    }
	    if(argsl.contains("--recommanded")||argsl.contains("-R")) {
	    	argsl.remove("--recommanded");
	    	argsl.remove("-R");
	    	argsl.add("--bind");
	    	argsl.add("-O3");
	    	argsl.add("-sASSERTIONS=0");
	    	argsl.add("-sSTACK_OVERFLOW_CHECK=0");
	    	argsl.add("-mbulk-memory");
	    	argsl.add("-flto");
	    	//b=ArrayUtils.addAll(hijackmalloc.getBytes(), b);
	    }	    
	    
	    ArrayList<String> list = new ArrayList<>();
	    if(isSysEmccAvailable()) {
	    	list.add(type?"em++":"emcc");
	    }else{
	    	list.add(pythondir);
	    	list.add(p.getAbsolutePath());
	    }
	    
	    list.add("-");
	    list.add("-x");
	    list.add(type ? "c++" : "c");
	    list.add("-o");
	    list.add(p2.getAbsolutePath());
	    list.addAll(argsl);
	    var fb=b;
	    Thread t = new Thread(() -> {
	        try {
	            Process pro = new ProcessBuilder(list.toArray(new String[0])).start();
	            pro.getOutputStream().write(fb);
	            pro.getOutputStream().close();
	            
	            byte[] errbs=pro.getErrorStream().readAllBytes();
	            int exitCode = pro.waitFor();
	            pro.destroy();
	            if (exitCode != 0 || !p2.exists()) {
	                fail.createNewFile();
	                Files.write(fail.toPath(), errbs, StandardOpenOption.WRITE);
	            }
	        } catch (Exception e) {e.printStackTrace();
	            e.printStackTrace();
	            try { fail.createNewFile(); 
	            Files.write(fail.toPath(), e.toString().getBytes(), StandardOpenOption.WRITE);
	            
	            } catch (Exception ignored) {}
	        }
	    });
	    t.setDaemon(true);
	    t.start();

	    return token;
	}


	static public String getStatus(String token) {
		if(token.equals("0"))return "FAILED";
	    File tempDir = new File(folder, "temp");
	    if (new File(tempDir, token + ".fail").exists()) return "FAILED";
	    if (new File(tempDir, token + ".wasm").exists()) return "DONE";
	    return "RUNNING";
	}



	static public byte[] getResult(String token) {
	    File f = new File(new File(folder, "temp"), token + ".wasm");
	    if (!f.exists()) return null;
	    try {
	        byte[] data = Files.readAllBytes(f.toPath());
	        f.delete();
	        return data;
	    } catch (Exception e) {
	        return null;
	    }
	}	
	static public byte[] getResultError(String token) {
		if(token.equals("0"))return "Python not installed.".getBytes();
	    File f = new File(new File(folder, "temp"), token + ".fail");
	    if (!f.exists()) return null;
	    try {
	        byte[] data = Files.readAllBytes(f.toPath());
	        f.delete();
	        return data;
	    } catch (Exception e) {
	        return null;
	    }
	}
	
	
	
	
}
