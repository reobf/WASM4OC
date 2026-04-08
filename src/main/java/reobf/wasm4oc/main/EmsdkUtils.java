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

public class EmsdkUtils {
	static File folder;
	public static void init(File folder0) {
		folder=folder0;
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
	static final Set<String> SKIP_DIRS = Set.of(
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
	
	
	
	static public String compile(String[] args, boolean type, byte[] b) {
	 
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

	    ArrayList<String> list = new ArrayList<>();
	    list.add(pythondir);
	    list.add(p.getAbsolutePath());
	    list.add("-");
	    list.add("-x");
	    list.add(type ? "c++" : "c");
	    list.add("-o");
	    list.add(p2.getAbsolutePath());
	    list.addAll(Arrays.asList(args));

	    Thread t = new Thread(() -> {
	        try {
	            Process pro = new ProcessBuilder(list.toArray(new String[0])).start();
	            pro.getOutputStream().write(b);
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
