package reobf.wasm4oc.main.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.component.Screen;
import li.cil.oc.common.item.traits.CPULike;
import li.cil.oc.server.component.GraphicsCard;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.server.network.Component;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemCPU extends Item implements HostAware, Processor {
	public class APIEnv implements ManagedEnvironment {
		ArrayDeque<String> disp = new ArrayDeque<String>();
		GraphicsCard firstcard;
		public Arch arch;

		@Override
		public void update() {
			if (arch == null)
				return;
			if (arch.machine.isRunning() == false)
				return;
			////
			if (arch.prog == null)
				try {
					var machine = arch.machine;
					List<li.cil.oc.common.component.Screen> scs = new ArrayList<>();
					List<li.cil.oc.server.component.FileSystem> fss = new ArrayList<>();
					List<GraphicsCard> gcs = new ArrayList<>();
					machine.node().network().nodes().forEach(s -> {

						if (s.host() instanceof li.cil.oc.common.component.Screen) {

							var sc = (Screen) s.host();
							scs.add(sc);

						}
						;
						if (s.host() instanceof GraphicsCard g) {

							gcs.add(g);

						}
						if (s.host() instanceof li.cil.oc.server.component.FileSystem i) {
							fss.add(i);

						}
					}

					);
					byte[] gets = null;
					String addr = null;
					for (var fs : fss) {

						try {
							int handle = fs.fileSystem().open("/init.wasm", li.cil.oc.api.fs.Mode.Read);
							var h = fs.fileSystem().getHandle(handle);
							byte[] get;
							h.read(get = new byte[(int) h.length()]);
							gets = (get);
							h.close();
							addr = fs.node().address();
							break;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
						}
					}
					GraphicsCard firstcard = gcs.size() > 0 ? gcs.get(0) : null;
					Screen firstscreen = gcs.size() > 0 ? scs.get(0) : null;
					if (firstcard != null && firstscreen != null) {
						machine.invoke(firstcard.node().address(), "bind",
								new Object[] { firstscreen.node().address() });

						Object[] get = machine.invoke(firstcard.node().address(), "getResolution", new Object[] {});
						machine.invoke(firstcard.node().address(), "fill", new Object[] { 1, 1, get[0], get[1], " " });

						if (gets == null) {
							machine.invoke(firstcard.node().address(), "set",
									new Object[] { 1, 1, "no bootable devices" });
							machine.invoke(firstcard.node().address(), "set",
									new Object[] { 1, 2, "no /init.wasm found" });
						} else {
							disp.push("booting from FileSystem: " + addr);

							// machine.invoke(firstcard.node().address(), "set", new Object[] {1,1,"booting
							// from FileSystem: "+addr});
							// machine.invoke(firstcard.node().address(), "set", new Object[] {1,2, gets});
							arch.prog = gets;
						}

					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			/// 
			
			if (arch.prog != null) {
				arch.doJob();
				var machine = arch.machine;
				if (firstcard == null) {
					machine.node().network().nodes().forEach(s -> {
						if (s.host() instanceof GraphicsCard g) {

							firstcard = g;
						}
					});

				}

				Iterator<String> it = disp.iterator();
				int i = 0;
				try {
					if (firstcard != null) {
						firstcard.getBuffer(i);
						Object[] get = machine.invoke(firstcard.node().address(), "getResolution", new Object[] {});
						machine.invoke(firstcard.node().address(), "fill", new Object[] { 1, 1, get[0], get[1], " " });

						while (it.hasNext()) {
							if (i >= (int) get[1]-1) {
								it.remove();
							}

							machine.invoke(firstcard.node().address(), "set", new Object[] { 1, 1 + (i++), it.next() });

						}
					}

				} catch (Exception e) {
					disp = null;
					e.printStackTrace();
				}

			}


		}

		// public RedstoneEnv(EnvironmentHost
		// env){this.env=env;};EnvironmentHost env;
		private Node _node = Network.newNode(this, Visibility.Network).withComponent("wasm_cpu").create();

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
			// TODO Auto-generated method stub

		}

		@Override
		public void onDisconnect(Node node) {

		}

		@Override
		public void load(NBTTagCompound nbt) {
			Optional.ofNullable(nbt.getTag("node")).ifPresent(s -> {
				if (node() != null)
					node().load((NBTTagCompound) s);
			});

		}

		@Override
		public void save(NBTTagCompound nbt) {
			NBTTagCompound t = new NBTTagCompound();
			Optional.ofNullable(node()).ifPresent(s -> s.save(t));
			nbt.setTag("node", t);
		}

		@Override
		public boolean canUpdate() {

			return true;
		}

		@Override
		public void onMessage(Message message) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public boolean worksWith(ItemStack stack) {

		return stack.getItem() instanceof ItemCPU;
	}

	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {

		return new APIEnv(stack);
	}

	@Override
	public String slot(ItemStack stack) {

		return Slot.CPU;
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
	public int supportedComponents(ItemStack stack) {

		return 128;
	}

	@Override
	public Class<? extends Architecture> architecture(ItemStack stack) {

		return Arch.class;
	}

	@Override
	public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {

		return stack.getItem() instanceof ItemCPU;
	}

}
