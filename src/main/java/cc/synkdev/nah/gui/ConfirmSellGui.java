package cc.synkdev.nah.gui;

import cc.synkdev.nah.NexusAuctionHouse;
import cc.synkdev.nah.api.events.ItemListEvent;
import cc.synkdev.nah.manager.DataFileManager;
import cc.synkdev.nah.manager.Util;
import cc.synkdev.nah.manager.WebhookManager;
import cc.synkdev.nah.objects.BINAuction;
import cc.synkdev.nah.manager.Lang;
import cc.synkdev.nah.manager.FileManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ConfirmSellGui {
    NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    public Gui gui(Player p, long price) {
        Gui gui = Gui.gui()
                .title(FileManager.getGuiTitle("confirm-sell", "&eConfirm Listing"))
                .rows(FileManager.getGuiRows("confirm-sell", 4))
                .disableAllInteractions()
                .create();
        gui.getFiller().fill(FileManager.getFillerItem("confirm-sell"));
        
        int itemSlot = FileManager.getGuiSlot("confirm-sell", "item-slot", 13);
        if (itemSlot >= 0) {
            gui.setItem(itemSlot, item(p));
        }

        int confirmSlot = FileManager.getGuiSlot("confirm-sell", "confirm", 20);
        if (confirmSlot >= 0) {
            gui.setItem(confirmSlot, confirm(price));
        }

        int cancelSlot = FileManager.getGuiSlot("confirm-sell", "cancel", 24);
        if (cancelSlot >= 0) {
            gui.setItem(cancelSlot, cancel());
        }
        return gui;
    }

    GuiItem item(Player p) {
        return ItemBuilder.from(p.getInventory().getItemInMainHand()).asGuiItem();
    }
    GuiItem confirm(long price) {
        long tax = Math.round(price*((double) core.getSellTaxPercent() /100));
        BaseItemBuilder<?> builder = FileManager.getGuiItemBuilder("confirm-sell", "confirm", Material.GREEN_WOOL, "&r&c&lConfirm", null);
        if (core.getSellTaxPercent() > 0) {
            builder.lore(Component.empty(), Lang.translateComp("taxes", core.getSellTaxPercent()+"", tax+""));
        }
        return builder.asGuiItem(event -> {
            Player pl = (Player) event.getWhoClicked();
            ItemStack itemStack = pl.getInventory().getItemInMainHand();
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                pl.sendMessage(core.prefix()+ChatColor.RED+Lang.translate("emptyHand", core));
                return;
            }
            if (Util.serializeItemstack(itemStack).length()>20000) {
                pl.sendMessage(core.prefix()+Lang.translate("tooBig", core));
                return;
            }
            if (core.banned.contains(itemStack.getType())) {
                pl.sendMessage(core.prefix() + Lang.translate("sellBanned", core));
                return;
            }
            if (!core.getEcon().has(pl, tax)) {
                pl.sendMessage(core.prefix() + ChatColor.RED + Lang.translate("notEnoughTaxes", core));
                return;
            }
            core.getEcon().withdrawPlayer(pl, tax);
            long expire = (System.currentTimeMillis() / 1000) + core.getExpireTime();

            ItemListEvent listEvent = new ItemListEvent(pl, itemStack, price, new Date(expire * 1000L));
            Bukkit.getPluginManager().callEvent(listEvent);

            if (listEvent.isCancelled()) return;

            BINAuction bA = new BINAuction(core.getId(), listEvent.getPlayer().getUniqueId(), listEvent.getItem(), listEvent.getPrice(), expire);
            core.setId(core.getId() + 1);
            pl.getInventory().setItemInMainHand(null);
            core.runningBINs.add(bA);
            DataFileManager.sort();
            pl.sendMessage(core.prefix() + ChatColor.GREEN + Lang.translate("successSell", core, price + ""));
            pl.closeInventory();
            WebhookManager.sendWebhook("new-listing", bA, listEvent.getPlayer().getName(), listEvent.getPrice() + "");

            pl.closeInventory();
        });
    }
    GuiItem cancel() {
        return FileManager.getGuiItem("confirm-sell", "cancel", Material.BARRIER, "&r&c&lCancel", null, event -> {
            Player pl = (Player) event.getWhoClicked();
            pl.closeInventory();
        });
    }
}
