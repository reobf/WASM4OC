package reobf.wasm4oc.main.item;


import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
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
import reobf.wasm4oc.main.Config;
import reobf.wasm4oc.util.MapFileSystemProvider;
import reobf.wasm4oc.util.SSHDServer;
import reobf.wasm4oc.util.SyncedFileSystem;

public class ItemSFTPCard  extends Item implements HostAware{
	public class APIEnv implements ManagedEnvironment {
        int lastupdate;
        @Override
        public void update() {
        	lastupdate=MinecraftServer.getServer().getTickCounter();
        }
        
        private Node _node = Network.newNode(this, Visibility.Network)
        		.withComponent("sftp")
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
        	
        	
        	isAlive=true;
        }

        @Override
        public void onDisconnect(Node node) {
        	if(node==_node) {
        		isAlive=false;SSHDServer.killDead();
        		name=null;
        	}
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
		boolean isAlive;
		String name;
		
		@Callback(doc = "getPort():int -- Get the prot of SFTP Server. -1 means that the SFTP was disabled by an admin."
				,direct = true,limit=1)
		public Object[] getPort(Context context, Arguments arguments,int mode) throws Exception{
			
			return new Object[] {Config.port};
		}
		@Callback(doc = "stop():void -- Stop the SFTP server."
				,direct = false,limit=1)
		 public Object[] stop(Context context, Arguments arguments) throws Exception{
       		if(name==null) {
       			throw new RuntimeException("Nothing to stop!");
       		}
       		SSHDServer.unregister(name);
       		//SSHDServer.killDead();
       		
    		name=null;
    		return new Object[] {"Stopped."};
		}
		
		@Callback(doc = "start(address:string, user:string, password:string):string -- Start a SFTP server. Will be automically shutdown when unloaded."
				,direct = false,limit=1)
		 public Object[] start(Context context, Arguments arguments) throws Exception{
			if(Config.port==-1) {throw new RuntimeException("SFTP disabled by an admin!");}
			if(name!=null)throw new RuntimeException("Already started!");
			var hst=node().network().node(arguments.checkString(0)).host();
			if(!(hst instanceof FileSystem)) {
				throw new RuntimeException("'address' is not a valid address of a filesystem component!");
			}
			FileSystem v=(FileSystem) hst;
			
			
			
			SSHDServer.register( arguments.checkString(1), arguments.checkString(2),new SyncedFileSystem(v.fileSystem(), ()->{
				try {
				return isAlive&&hst.node().network()==node().network()&&
						Math.abs(lastupdate-MinecraftServer.getServer().getTickCounter())<20
						;
						
						}catch(Exception w) {return false;}
				
			}));
			name=arguments.checkString(1);
			return new Object[] {"Started."};
		}
		
		
    }
	
	@Override
	public boolean worksWith(ItemStack stack) {
		
		return stack.getItem() instanceof ItemSFTPCard;
	}

	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
	
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
	
		return stack.getItem() instanceof ItemSFTPCard;
	}

}
