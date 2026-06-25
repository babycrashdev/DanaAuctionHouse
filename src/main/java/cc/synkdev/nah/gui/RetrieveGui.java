package cc.synkdev.nah.gui;

import cc.synkdev.nah.NexusAuctionHouse;
import cc.synkdev.nah.manager.Util;
import cc.synkdev.nah.manager.Lang;
import cc.synkdev.nah.manager.FileManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RetrieveGui {
    NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    int max = 0;
    public Gui gui(Player p, int page) {
        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.retrieve.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) listingSlots.add(i);
        }
        int itemsPerPage = listingSlots.size();
        max = (core.retrieveMap.getOrDefault(p.getUniqueId(), new ArrayList<>()).size()+ (itemsPerPage - 1)) / itemsPerPage;
        Gui gui = Gui.gui()
                .disableAllInteractions()
                .title(FileManager.getGuiTitle("retrieve", "&eRetrieve Items"))
                .rows(FileManager.getGuiRows("retrieve", 6))
                .create();
        
        gui.getFiller().fill(FileManager.getFillerItem("retrieve"));

        for (int slot : listingSlots) {
            gui.removeItem(slot);
        }

        int prevPageSlot = FileManager.getGuiSlot("retrieve", "previous-page", 48);
        int nextPageSlot = FileManager.getGuiSlot("retrieve", "next-page", 50);

        if (page > 1 && prevPageSlot >= 0) gui.setItem(prevPageSlot, arrowLeft(page));
        if (page < max && nextPageSlot >= 0) gui.setItem(nextPageSlot, arrowRight(page));
        fillGui(gui, p, page, listingSlots);

        return gui;
    }
    GuiItem arrowLeft(int page) {
        return FileManager.getGuiItem("retrieve", "previous-page", Material.ARROW, "&r&e&lPrevious Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, page-1).open(p);
        });
    }
    GuiItem arrowRight(int page) {
        return FileManager.getGuiItem("retrieve", "next-page", Material.ARROW, "&r&e&lNext Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, page+1).open(p);
        });
    }
    private void fillGui(Gui gui, Player p, int page, List<Integer> listingSlots) {
        int itemsPerPage = listingSlots.size();
        int min = itemsPerPage*(page-1);
        int maxIndex = itemsPerPage*page;

        List<ItemStack> list = new ArrayList<>(core.retrieveMap.getOrDefault(p.getUniqueId(), new ArrayList<>()));
        int guiSlotIndex = 0;
        for (int i = min; i < maxIndex; i++) {
            if (list.size() > i) {
                if (guiSlotIndex >= listingSlots.size()) break;
                gui.setItem(listingSlots.get(guiSlotIndex), item(list.get(i), page));
                guiSlotIndex++;
            }
        }
    }
    GuiItem item(ItemStack item, int page) {
        List<Component> lore = new ArrayList<>();
        lore.addAll(Util.loreToComps(item));
        lore.add(Component.empty());
        lore.add(Lang.translateComp("clickRetrieve"));
        return ItemBuilder.from(item.clone())
                .lore(lore)
                .asGuiItem(event -> {
                    Player p = (Player) event.getWhoClicked();
                    List<ItemStack> list = core.retrieveMap.getOrDefault(p.getUniqueId(), new ArrayList<>());
                    if (!list.isEmpty() && list.contains(item)) {
                        if (p.getInventory().firstEmpty() == -1) {
                            p.sendMessage(core.prefix()+Lang.translate("fullInv", core));
                            return;
                        }
                        p.getInventory().addItem(item);
                        list.remove(item);
                        if (!list.isEmpty()) {
                            core.retrieveMap.replace(p.getUniqueId(), list);
                            gui(p, page).open(p);
                        }
                        else {
                            core.retrieveMap.remove(p.getUniqueId());
                            p.closeInventory();
                        }
                    }
                });
    }
}
