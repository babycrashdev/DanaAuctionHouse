package cc.synkdev.nah.manager;

import cc.synkdev.nah.NexusAuctionHouse;

public class Lang {
    public static String translate(String key, NexusAuctionHouse core, String... args) {
        return FileManager.getMsg(key, args);
    }
}
