package reobf.wasm4oc.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class BinaryExcutablesManager {
	static String osName = System.getProperty("os.name");
	static boolean isWindows = osName != null && osName.toLowerCase().contains("windows");

	
static long lastcall;
	
	
	public static synchronized byte[] process(byte[] in,int tool) throws Exception  {
		while(Math.abs(lastcall-System.currentTimeMillis())<1000*5/*5 sec*/) {
			throw new RuntimeException("Too frequent.");//Thread.sleep(250);
		}
		lastcall=System.currentTimeMillis();
		Process process = new ProcessBuilder(
				tools.get(tool).getAbsolutePath(), "-", "-o", "-", "--enable-gc","--enable-reference-types"
			).start();

		process.getOutputStream().write(in);
		
		process.getOutputStream().close();

			
			byte[] result = process.getInputStream().readAllBytes();
			byte[] err = process.getErrorStream().readAllBytes();
			process.waitFor();
			process.destroy();
			if(result.length==0)return err;
	    return result;
	}
	
	static ArrayList<File> tools=new ArrayList<>();
	public final static int ASSEMBLE=0;
	public final static int DISASSEMBLE=1;
	public static void unpack(File folder) {
	String suffix=isWindows?".exe":"";
	
	String[] todo= {"/wasm-as"+suffix,"/wasm-dis"+suffix};
	for(String s:todo) {
	InputStream input = BinaryExcutablesManager.class.getResourceAsStream("/bin"+s);
		File name=new File(folder, s);
		try {
			folder.mkdirs();
			if(name.exists()==false) {
			name.createNewFile();
			 Files.copy(input, name.toPath(), StandardCopyOption.REPLACE_EXISTING);
			 }
			 tools.add(name);
		} catch (IOException e) {
		e.printStackTrace();	
		}
		
		
		
	}
	
}
}
