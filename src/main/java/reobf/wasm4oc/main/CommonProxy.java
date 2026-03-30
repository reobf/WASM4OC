package reobf.wasm4oc.main;



import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.api.Driver;
import li.cil.oc.api.detail.ItemAPI;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import reobf.wasm4oc.main.item.ItemCompilerCard;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        File folder=new File(event.getSuggestedConfigurationFile().getParentFile().getParentFile(),"wasm4oc_bin");
        folder.mkdirs();
        BinaryExcutablesManager.unpack(folder);
           	GameRegistry.registerItem(
            MyMod.ccard = new ItemCompilerCard().setMaxStackSize(1)
            .setUnlocalizedName("wasm4oc.oc.compilercard")
            .setTextureName("wasm4oc:compilercard"),
        "wasm4oc.oc.compilercard");
            li.cil.oc.server.driver.Registry.add((li.cil.oc.api.driver.Item) MyMod.ccard);
        MyMod.LOG.info(Config.greeting);
        MyMod.LOG.info("I am MyMod at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {          
    	
    	 OCApi.put(MyMod.ccard, ItemCompilerCard.APIEnv.class);
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
    	
    	 
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
}
