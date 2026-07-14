package reobf.wasm4oc.main.item;


import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
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

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.server.component.FileSystem;
import li.cil.oc.server.component.GraphicsCard;
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
        private Node _node = Network.newNode(this, Visibility.Network)
        		.withComponent("sftp")
            .create();
        VGPU vgpu=new VGPU();
        public class VGPU extends GraphicsCard{

        	
        	char[][] arr=new char[0][0];
        	
        	@Override
        	public Object[] setResolution(Context context, Arguments args) {
        		int w=args.checkInteger(0);
        		int h=args.checkInteger(1);
        		arr=(char[][]) Array.newInstance(char.class, h,w);
        		return super.setResolution(context, args);
        	}
        	
      @Override
    public Object[] fill(Context context, Arguments args) {
    	
    	return super.fill(context, args);
    }
        	@Override
        	public Object[] set(Context context, Arguments args) {
        		
        		
        		int x=args.checkInteger(0);
        		int y=args.checkInteger(1);
        		var tt=args.checkByteArray(2);
        		String s=new String(tt,Charset.forName("utf-8"));//checkSting uses wrong decoder
        		boolean v=args.optBoolean(3, false);
        		var get=s.codePoints();
        		var gets=get.toArray();
        		try {
        			for(var c:gets) {
        			arr[y][x]=(char) c;
        			if(v)
        				y++;
        			else 
        				x++;
        			
        		}
        		
        		for(var vv:arr) {
        			
        			for(var vvv:vv) {
        				System.out.print(vvv);
        			}System.out.print('\n');
        		}
        		
        		}catch(Exception e) {
        			
        			int a=1;
        		}
        		
        		
        		return super.set(context, args);
        	}
        	
			public VGPU() {
				super(2);
				/*li.cil.oc.api.network.ComponentConnector n=(ComponentConnector) this.node();
				n.setVisibility(Visibility.Network);*/
				Field f;
				try {
				f = GraphicsCard.class.getDeclaredField("node");
				f.setAccessible(true);
				var newnode=
				Network.newNode(this, Visibility.Network).
			    withComponent("gpu").
			    withConnector().
			    create();
				f.set(this, newnode);
				
				} catch (Exception e) {
					e.printStackTrace();
				}
			
				
				
			}
			
        	@Override
        	public void save(NBTTagCompound nbt) {
        		// TODO Auto-generated method stub
        		super.save(nbt);
        	}
        	@Override
        	public void load(NBTTagCompound nbt) {
        		// TODO Auto-generated method stub
        		super.load(nbt);
        	}
        }
        
        
        @Override
        public void update() {
        	lastupdate=MinecraftServer.getServer().getTickCounter();
        }
        

        
        /*private Node _nodev = Network.newNode(vgpu, Visibility.Network)
        		.withComponent("gpu")
            .create();*/
        
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
        
        	if(!_node.canBeReachedFrom(vgpu.node())) {
        		
        		Network.joinNewNetwork(vgpu.node());
        		_node.connect(vgpu.node());
        	}
        	
        	isAlive=true;
        }

        @Override
        public void onDisconnect(Node node) {
        	if(node==_node) {
        		isAlive=false;SSHDServer.killDead();
        		name=null;
        		//_node.disconnect(vgpu.node());
        	}
        }



        @Override
        public void load(NBTTagCompound nbt) {
            Optional.ofNullable(nbt.getTag("node"))
                .ifPresent(s -> { if (node() != null) node().load((NBTTagCompound) s); });
            Optional.ofNullable(nbt.getTag("vnode"))
            .ifPresent(s -> { if (vgpu.node() != null) {vgpu.node().load((NBTTagCompound) s);vgpu.load((NBTTagCompound)s); }});
       if(FMLCommonHandler.instance().getEffectiveSide()==Side.SERVER)
            if(!_node.canBeReachedFrom(vgpu.node())) {
        		
        		Network.joinNewNetwork(vgpu.node());
        		_node.connect(vgpu.node());
        		
        	}
        }

        @Override
        public void save(NBTTagCompound nbt) {
        	{
        	NBTTagCompound t = new NBTTagCompound();
            Optional.ofNullable(node())
                .ifPresent(s -> s.save(t));
            nbt.setTag("node", t);
        	}  
        	{
        	NBTTagCompound t = new NBTTagCompound();
            Optional.ofNullable(vgpu.node())
                .ifPresent(s -> s.save(t));
            vgpu.save(t);
            nbt.setTag("vnode", t);
        	}           
            
            
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
		public Object[] getPort(Context context, Arguments arguments) throws Exception{
			
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
