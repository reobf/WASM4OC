package reobf.wasm4oc.main;



import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.Driver;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.common.Loot;

import li.cil.oc.server.machine.Machine;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import reobf.wasm4oc.main.item.Arch;
import reobf.wasm4oc.main.item.ItemCPU;
import reobf.wasm4oc.main.item.ItemCompilerCard;
import reobf.wasm4oc.main.item.ItemSFTPCard;

public class CommonProxy {
	 public static final CreativeTabs tab = new CreativeTabs( "WASM4OC")
	    {
	       
	        @SideOnly(Side.CLIENT)
	        public Item getTabIconItem()
	        {
	            return MyMod.cpu;
	        }
	        public void displayAllReleventItems(java.util.List<ItemStack> p_78018_1_) {
	        	
	        	p_78018_1_.add(im);
	        	p_78018_1_.add(new ItemStack( MyMod.ccard));
	        	p_78018_1_.add(new ItemStack( MyMod.cpu));
	        	p_78018_1_.add(new ItemStack( MyMod.cpu,1,1));
	        	p_78018_1_.add(new ItemStack( MyMod.cpu,1,2));
	        	p_78018_1_.add(new ItemStack( MyMod.stfp));
	        	
	        };
	        
	        
	    };
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
    	
    	createLootDisk("compiler");
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        File folder=new File(event.getSuggestedConfigurationFile().getParentFile().getParentFile(),"emsdk");
        folder.mkdirs();
        EmsdkUtils.init(folder);
       // BinaryExcutablesManager.unpack(folder);
           	GameRegistry.registerItem(
            MyMod.ccard = new ItemCompilerCard().setMaxStackSize(1)
            .setUnlocalizedName("wasm4oc.oc.compilercard")
            .setTextureName("wasm4oc:compilercard"),
        "wasm4oc.oc.compilercard");
         	
           	GameRegistry.registerItem(
            MyMod.cpu = new ItemCPU().setMaxStackSize(1)
            .setUnlocalizedName("wasm4oc.oc.cpu")
            .setTextureName("wasm4oc:cpu0"),
        "wasm4oc.oc.cpu");
           	
          	GameRegistry.registerItem(
                    MyMod.stfp = new ItemSFTPCard().setMaxStackSize(1)
                    .setUnlocalizedName("wasm4oc.oc.sftpcard")
                    .setTextureName("wasm4oc:sftpcard"),
                "wasm4oc.oc.sftpcard");         	
           	li.cil.oc.server.driver.Registry.add((li.cil.oc.api.driver.Item) MyMod.cpu);
            li.cil.oc.server.driver.Registry.add((li.cil.oc.api.driver.Item) MyMod.ccard);
            li.cil.oc.server.driver.Registry.add((li.cil.oc.api.driver.Item) MyMod.stfp);
       // MyMod.LOG.info(Config.greeting);
        MyMod.LOG.info("I am MyMod at version " + Tags.VERSION);
        
        Loot.factories();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {          
   	 //OCApi.put(MyMod.ccard, ItemCompilerCard.APIEnv.class);
    	 OCApi.put(MyMod.stfp, ItemSFTPCard.APIEnv.class);
   	  OCApi.forEach((k, v) -> Driver.add(new li.cil.oc.api.driver.EnvironmentProvider() {

             @Override
             public Class<?> getEnvironment(ItemStack itemStack) {
                 Object kk = k;
                 if (kk instanceof Block) {
                     kk = Item.getItemFromBlock((Block) kk);
                 }
                 if (itemStack != null && (itemStack.getItem() == kk
                     || (kk instanceof ItemStack
                         ? (((ItemStack) kk).getItem() == itemStack.getItem()
                             && ((ItemStack) kk).getItemDamage() == itemStack.getItemDamage())
                         : false))) {
                     return v;
                 }
                 return null;
             }
         }));
    	
    }
    public static Map<Object, Class> OCApi = new HashMap<>();

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
    	
    	 Machine.add(Arch.class);
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
    public static   ItemStack im;
    public void createLootDisk(String path)  {

    		var ro=li.cil.oc.api.FileSystem.fromClass(CommonProxy.class, "wasm4oc", path);
    		im=Loot.registerLootDisk(path, 9, ()->ro, true);
    		im.setStackDisplayName("Emscipten Compiler");
    		im=li.cil.oc.common.init.Items.registerStack(im, path);
    	
    }
}
