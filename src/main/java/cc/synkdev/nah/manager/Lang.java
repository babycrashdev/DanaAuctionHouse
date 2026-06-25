package cc.synkdev.nah.manager;

import cc.synkdev.nah.NexusAuctionHouse;
import net.kyori.adventure.text.Component;

public class Lang {
    public static String translate(String key, NexusAuctionHouse core, String... args) {
        return FileManager.getMsg(key, args);
    }

    public static Component translateComp(String key, String... args) {
        return FileManager.getMsgComponent(key, args);
    }
}
