package reobf.wasm4oc.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.AbstractSftpEventListenerAdapter;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;


import li.cil.oc.server.component.FileSystem;
import li.cil.oc.server.fs.FileSystem.RamFileSystem;
import reobf.wasm4oc.main.Config;

public class SSHDServer {
	public static int findAvailablePort(int start) throws IOException {
	    try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
	        socket.setReuseAddress(true);
	        return socket.getLocalPort();
	    }
	}
	static {init();}
	static Map<String,SyncedFileSystem> users=new ConcurrentHashMap<>();
	static Map<String,String> userpasswd=new ConcurrentHashMap<String, String>();
	static SshServer sshd;
	
	public static void register(String user,String pwd,SyncedFileSystem fs) {
		if(users.containsKey(user))throw new RuntimeException("Username occupied!");
		users.put(user, fs);
		userpasswd.put(user, pwd);
	}
	public static void unregister(String user) {
		users.remove(user);
		userpasswd.remove(user);
		
	}
	
	static public void init() {
	if(Config.port==-1)return;
	if(sshd!=null)return;
	 {
		// sshd.setShellFactory(null);
	sshd = SshServer.setUpDefaultServer();
	sshd.setPort(Config.port);
	sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
	sshd.setPasswordAuthenticator((u, p2, s) -> {
		killDead();
		if(Objects.equals(p2, userpasswd.get(u))) {
			return users.containsKey(u);
		}return false;
		
	});
	sshd.setNioWorkers(4);
	
	/*li.cil.oc.api.fs.FileSystem ocFs =ofs.fileSystem();
	MapFileSystemProvider provider = new MapFileSystemProvider();
	java.nio.file.FileSystem nioFs = provider.newFileSystem(
	    URI.create("ocfs://myfs"),
	    Map.of("ocFs", ocFs)
	);*/

	sshd.setFileSystemFactory(new FileSystemFactory() {
		
		@Override
		public Path getUserHomeDir(SessionContext session) throws IOException {
			
			return null;
		}
		
		@Override
		public java.nio.file.FileSystem createFileSystem(SessionContext session) throws IOException {
		
			
			li.cil.oc.api.fs.FileSystem ocFs =users.get(session.getUsername());
			if(ocFs==null)throw new IOException("no fs");
			MapFileSystemProvider provider = new MapFileSystemProvider();
			java.nio.file.FileSystem nioFs = provider.newFileSystem(
			    URI.create("ocfs://myfs"),
			    Map.of("ocFs", ocFs)
			);
			return nioFs;
		}
	});
	
	sshd.setSubsystemFactories(List.of(new SftpSubsystemFactory()
			{
		
		
		
			}
			
			
			));
	try {
		sshd.start();
	} catch (IOException e) {
		
		e.printStackTrace();
	}}
	
}
	public static void killDead() {
		var k=users.entrySet().iterator();
		while(k.hasNext()) {
			var n=k.next();
			if(n.getValue().isAlivePredicate.get()==false) {
				userpasswd.remove(n.getKey());
				k.remove();
				
			}
			
		}
		
	}
}
