package reobf.wasm4oc.main.item;


import java.util.Optional;

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
import li.cil.oc.server.machine.Machine;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import reobf.wasm4oc.main.BinaryExcutablesManager;

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
		@Callback(doc = "test"
				,direct = false)
		 public Object[] test(Context context, Arguments arguments) throws Exception{
			System.out.println(MinecraftServer.getServer().getTickCounter());
			Machine m=null;
			Context x=m;
			return null;
		}
		
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
		@Callback(doc = ""
				,direct = true)
		 public Object[] debug(Context context, Arguments arguments) throws Exception{
			System.out.println(arguments.checkAny(0));
			System.out.println(arguments.checkAny(0).getClass());
			System.out.println(arguments.checkAny(0) instanceof java.util.Map);
			return null;
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
