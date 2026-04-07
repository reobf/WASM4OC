package reobf.wasm4oc.main;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

   public static int port=2222;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        port = configuration.getInt("SFTP_Port", Configuration.CATEGORY_GENERAL, port, -1, 65535, "SFTP Port. 0 for auto, -1 to disable");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
