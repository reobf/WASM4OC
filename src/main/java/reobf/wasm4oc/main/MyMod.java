package reobf.wasm4oc.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.item.Item;
import reobf.wasm4oc.main.item.ItemCPU;
import reobf.wasm4oc.util.SSHDServer;

@Mod(modid = MyMod.MODID, version = Tags.VERSION, name = "wasm4oc", acceptedMinecraftVersions = "[1.7.10]")
public class MyMod {

	
    public static final String MODID = "wasm4oc";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "reobf.wasm4oc.main.ClientProxy", serverSide = "reobf.wasm4oc.main.CommonProxy")
    public static CommonProxy proxy;
	//public static Item ccard;
	public static Item cpu;
	public static Item stfp;
	public static Item ccard;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        SSHDServer.class.getDeclaredFields();// init
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
