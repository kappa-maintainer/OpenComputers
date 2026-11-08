package li.cil.oc.util;

import net.minecraftforge.fml.common.FMLCommonHandler;

public final class SideTracker {
    public static boolean isServer() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer();
    }

    public static boolean isClient() {
        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }
}
