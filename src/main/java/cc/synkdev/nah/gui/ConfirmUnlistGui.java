package cc.synkdev.nah.gui;

import cc.synkdev.nah.NexusAuctionHouse;
import cc.synkdev.nah.api.NAHUtil;
import cc.synkdev.nah.api.events.ItemUnlistEvent;
import cc.synkdev.nah.manager.DataFileManager;
import cc.synkdev.nah.objects.BINAuction;
import cc.synkdev.nah.manager.Lang;
import cc.synkdev.nah.manager.FileManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
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
import java.util.List;

public class ConfirmUnlistGui {
    private final NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    private Player p;
    private BINAuction bA;
    private ItemStack item;
    public Gui gui(Player p, BINAuction bA) {
        this.p = p;
        this.bA = bA;
        item = bA.getItem();
        Gui gui = Gui.gui()
                .title(FileManager.getGuiTitle("confirm-unlist", "&eConfirm Unlist"))
                .rows(FileManager.getGuiRows("confirm-unlist", 4))
                .disableAllInteractions()
                .create();
        gui.getFiller().fill(FileManager.getFillerItem("confirm-unlist"));
        
        int itemSlot = FileManager.getGuiSlot("confirm-unlist", "item-slot", 13);
        if (itemSlot >= 0) {
            gui.setItem(itemSlot, item());
        }

        int confirmSlot = FileManager.getGuiSlot("confirm-unlist", "confirm", 20);
        if (confirmSlot >= 0) {
            gui.setItem(confirmSlot, confirm());
        }

        int cancelSlot = FileManager.getGuiSlot("confirm-unlist", "cancel", 24);
        if (cancelSlot >= 0) {
            gui.setItem(cancelSlot, cancel());
        }
        return gui;
    }

    GuiItem item() {
        return ItemBuilder.from(bA.getItem().clone()).lore(Component.empty(), Lang.translateComp("lore-unlist-item")).asGuiItem();
    }
    GuiItem confirm() {
        return FileManager.getGuiItem("confirm-unlist", "confirm", Material.GREEN_WOOL, "&r&c&lConfirm", null, event -> {
            Player pl = (Player) event.getWhoClicked();
            BINAuction bAa = NAHUtil.getAuction(bA.getId());
            if (bAa.getBuyable()) {
                bAa.setItem(this.item);
                bAa.setBuyer(bAa.getSeller());
                ItemUnlistEvent unlistEvent = new ItemUnlistEvent(pl, bAa);
                Bukkit.getPluginManager().callEvent(unlistEvent);

                if (unlistEvent.isCancelled()) return;

                core.runningBINs.remove(bAa);
                core.expiredBINs.add(bAa);
                DataFileManager.sort();
                if (pl.getInventory().firstEmpty() != -1) pl.getInventory().addItem(this.item);
                else {
                    List<ItemStack> retrieveList = new ArrayList<>(core.retrieveMap.getOrDefault(pl.getUniqueId(), new ArrayList<>()));
                    retrieveList.add(this.item);
                    core.retrieveMap.put(pl.getUniqueId(), retrieveList);
                    pl.sendMessage(Lang.translate("retrieveFull", core));
                }
            }
            NAHUtil.open(pl, false, null, 1);
        });
    }
    GuiItem cancel() {
        return FileManager.getGuiItem("confirm-unlist", "cancel", Material.BARRIER, "&r&c&lCancel", null, event -> NAHUtil.open(p, false, null, 1));
    }
}
