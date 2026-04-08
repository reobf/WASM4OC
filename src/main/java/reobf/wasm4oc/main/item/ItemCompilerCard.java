package reobf.wasm4oc.main.item;


import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.AbstractSftpEventListenerAdapter;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.server.component.FileSystem;
import li.cil.oc.server.fs.FileSystem.RamFileSystem;
import li.cil.oc.server.machine.Machine;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import reobf.wasm4oc.main.BinaryExcutablesManager;
import reobf.wasm4oc.main.EmsdkUtils;
import reobf.wasm4oc.util.MapFileSystemProvider;

public class ItemCompilerCard  extends Item implements HostAware{
	public class APIEnv implements ManagedEnvironment {

        @Override
        public void update() {
     

        }
        private Node _node = Network.newNode(this, Visibility.Network)
        		.withComponent("compiler")
            .create();

        public APIEnv(ItemStack stack) {
            this.stack = stack;
        }

        ItemStack stack;

        @Override
        public Node node() {
            return _node;

        }
        SshServer sshd;
        @Override
        public void onConnect(Node node) {
        	
        	
        	
        }

        @Override
        public void onDisconnect(Node node) {

        }



        @Override
        public void load(NBTTagCompound nbt) {
            Optional.ofNullable(nbt.getTag("node"))
                .ifPresent(s -> { if (node() != null) node().load((NBTTagCompound) s); });

        }

        @Override
        public void save(NBTTagCompound nbt) {
            NBTTagCompound t = new NBTTagCompound();
            Optional.ofNullable(node())
                .ifPresent(s -> s.save(t));
            nbt.setTag("node", t);
        }

        @Override
        public boolean canUpdate() {

            return true;
        }

		@Override
		public void onMessage(Message message) {
		
			
		}

		/*
		@Callback(doc = "assemble(address:string, path:string[, output:string]):string -- Provide address of a diskdrive and path to a .wat file,"
				+" assemble it to .wasm and ouput it to the output path."
				,direct = true,limit=1)
		 public Object[] assemble(Context context, Arguments arguments) throws Exception{
			return doJob( context,  arguments,BinaryExcutablesManager.ASSEMBLE);
		}
		
		@Callback(doc = "disassemble(address:string, path:string[, output:string]):string -- Provide address of a diskdrive and path to a .wasm file,"
				+" disassemble it to .way and ouput it to the output path."
				,direct = true,limit=1)
		 public Object[] disassemble(Context context, Arguments arguments) throws Exception{
			return doJob( context,  arguments,BinaryExcutablesManager.DISASSEMBLE);
		}		

	
		 private Object[] doJob(Context context, Arguments arguments,int mode) throws Exception{
			 String[][] suffix= {{".wat",".wasm"},{".wasm",".wat"}};
			 
				String addr=arguments.checkString(0);
				String in=arguments.checkString(1);
				String outalt="";
				if(in.endsWith(suffix[mode][0])) {
					outalt=in.substring(0, in.length()-suffix[mode][0].length())+suffix[mode][1];
				}else {
					outalt=in+suffix[mode][1];
				}
				String out=arguments.optString(2, outalt);
				
				var hst=node().network().node(addr).host();
				if(!(hst instanceof FileSystem)) {
					throw new RuntimeException("'path' is not a valid address of a filesystem component!");
				}
				li.cil.oc.api.fs.FileSystem fs= ((FileSystem) hst).fileSystem();
				
				int open=fs.open(in, Mode.Read);
				var h=fs.getHandle(open);
				byte inbyte[]=new byte[(int) h.length()];
				h.read(inbyte);
				h.close();
				byte[] result=BinaryExcutablesManager.process(inbyte, mode);
				
				open=fs.open(out, Mode.Write);
			    h=fs.getHandle(open);
				h.write(result);
				h.close();
				
				return null;
			
	    }*/
		
		@Callback(doc = "compile(address:string, path:string[, args:string...]):string -- Compile C source file to wasm, returns token",
	            direct = true, limit = 1)
	    public Object[] compile(Context context, Arguments arguments) throws Exception {
	        String addr = arguments.checkString(0);
	        String path = arguments.checkString(1);
	        if(EmsdkUtils.pythondir==null) {throw new RuntimeException("No python installed.");}
	        var hst = node().network().node(addr).host();
	        if (!(hst instanceof FileSystem)) {
	            throw new RuntimeException("Not a valid filesystem address!");
	        }
	        li.cil.oc.api.fs.FileSystem fs = ((FileSystem) hst).fileSystem();
	        

	        int open = fs.open(path, Mode.Read);
	        var h = fs.getHandle(open);
	        byte[] src = new byte[(int) h.length()];
	        h.read(src);
	        h.close();
	        

	        boolean isCpp = false;
	        

	        ArrayList<String> extraArgs = new ArrayList<>();
	        for (int i = 2; i < arguments.count(); i++) {
	            extraArgs.add(arguments.checkString(i));
	        }
	        
	        String token = EmsdkUtils.compile(extraArgs.toArray(new String[0]), isCpp, src);
	        return new Object[]{token};
	    }
		@Callback(doc = "compileCpp(address:string, path:string[, args:string...]):string -- Compile C++ source file to wasm, returns token",
	            direct = true, limit = 1)
	    public Object[] compileCpp(Context context, Arguments arguments) throws Exception {
	        String addr = arguments.checkString(0);
	        String path = arguments.checkString(1);
	        if(EmsdkUtils.pythondir==null) {throw new RuntimeException("No python installed.");}
	        var hst = node().network().node(addr).host();
	        if (!(hst instanceof FileSystem)) {
	            throw new RuntimeException("Not a valid filesystem address!");
	        }
	        li.cil.oc.api.fs.FileSystem fs = ((FileSystem) hst).fileSystem();
	        

	        int open = fs.open(path, Mode.Read);
	        var h = fs.getHandle(open);
	        byte[] src = new byte[(int) h.length()];
	        h.read(src);
	        h.close();
	        

	        boolean isCpp = true;
	        

	        ArrayList<String> extraArgs = new ArrayList<>();
	        for (int i = 2; i < arguments.count(); i++) {
	            extraArgs.add(arguments.checkString(i));
	        }
	        
	        String token = EmsdkUtils.compile(extraArgs.toArray(new String[0]), isCpp, src);
	        return new Object[]{token};
	    }
	    @Callback(doc = "getStatus(token:string):string -- Get compile status: RUNNING, DONE or FAILED",
	            direct = true, limit = 1)
	    public Object[] getStatus(Context context, Arguments arguments) throws Exception {
	        String token = arguments.checkString(0);
	        return new Object[]{EmsdkUtils.getStatus(token)};
	    }

	    @Callback(doc = "getResult(token:string, address:string, path:string):boolean -- Write compiled wasm to filesystem, returns true on success",
	            direct = true, limit = 1)
	    public Object[] getResult(Context context, Arguments arguments) throws Exception {
	        String token = arguments.checkString(0);
	        String addr = arguments.checkString(1);
	        String path = arguments.checkString(2);
	        
	        byte[] result = EmsdkUtils.getResult(token);
	        if (result == null) return new Object[]{false};
	        
	        var hst = node().network().node(addr).host();
	        if (!(hst instanceof FileSystem)) {
	            throw new RuntimeException("Not a valid filesystem address!");
	        }
	        li.cil.oc.api.fs.FileSystem fs = ((FileSystem) hst).fileSystem();
	        
	        int open = fs.open(path, Mode.Write);
	        var h = fs.getHandle(open);
	        h.write(result);
	        h.close();
	        
	        return new Object[]{true};
	    }
	    @Callback(doc = "getError(token:string):string -- Get compilation error message",
	            direct = true, limit = 1)
	    public Object[] getError(Context context, Arguments arguments) throws Exception {
	    	return new Object[] {EmsdkUtils.getResultError( arguments.checkString(0))};
	    }
    }
	
	@Override
	public boolean worksWith(ItemStack stack) {
		
		return stack.getItem() instanceof ItemCompilerCard;
	}

	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
		// TODO Auto-generated method stub
		return new APIEnv(stack);
	}

	@Override
	public String slot(ItemStack stack) {
		
		return Slot.Card;
	}

	@Override
	public int tier(ItemStack stack) {
		
		return 0;
	}

	@Override
	public NBTTagCompound dataTag(ItemStack stack) {
	
		return null;
	}

	@Override
	public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
	
		return stack.getItem() instanceof ItemCompilerCard;
	}

}
