package cc.synkdev.nah.gui;

import cc.synkdev.nah.NexusAuctionHouse;
import cc.synkdev.nah.api.NAHUtil;
import cc.synkdev.nah.manager.Util;
import cc.synkdev.nah.objects.BINAuction;
import cc.synkdev.nah.manager.Lang;
import cc.synkdev.nah.manager.FileManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerAuctionsGui {
    NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    int page;
    int max;
    MainGuiSnapshot snapshot;
    OfflinePlayer target;
    List<BINAuction> contents;
    public Gui gui(Player p, OfflinePlayer target, int page, MainGuiSnapshot snapshot) {
        this.snapshot = snapshot;
        this.contents = Util.getPlayerListings(target);
        core.checkExpiry.run();
        
        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.player-auctions.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) listingSlots.add(i);
        }
        int itemsPerPage = listingSlots.size();
        max = (contents.size()+ (itemsPerPage - 1)) / itemsPerPage;
        this.target = target;
        this.page = page;

        String titleStr = FileManager.getGui().getString("menus.player-auctions.title", "&e%s1%'s Auctions");
        titleStr = Util.addPlaceholders(titleStr, target.getName() != null ? target.getName() : "");
        Component titleComp = FileManager.parseMiniMessage(titleStr);

        Gui gui = Gui.gui()
                .disableAllInteractions()
                .title(titleComp)
                .rows(FileManager.getGuiRows("player-auctions", 6))
                .create();

        gui.getFiller().fill(FileManager.getFillerItem("player-auctions"));

        int prevPageSlot = FileManager.getGuiSlot("player-auctions", "previous-page", 48);
        int nextPageSlot = FileManager.getGuiSlot("player-auctions", "next-page", 50);

        if (page > 1 && prevPageSlot >= 0) {
            gui.setItem(prevPageSlot, arrowLeft(page));
        }
        if (page < max && nextPageSlot >= 0) {
            gui.setItem(nextPageSlot, arrowRight(page));
        }

        fillGui(gui, p, page, listingSlots);

        int backSlot = FileManager.getGuiSlot("player-auctions", "back", 49);
        if (snapshot != null && backSlot >= 0) {
            gui.setItem(backSlot, FileManager.getGuiItemBuilder("player-auctions", "back", Material.BARRIER, snapshot == null ? "&r&cClose" : "&r&cBack", null)
                    .asGuiItem(event -> {
                        new MainGui().gui(p, snapshot.getPage(), snapshot.getSearch(), snapshot.getFirstSort(), snapshot.getItSort()).open(p);
                    }));
        }
        return gui;
    }
    GuiItem arrowLeft(int page) {
        return FileManager.getGuiItem("player-auctions", "previous-page", Material.ARROW, "&r&e&lPrevious Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, target, page-1, snapshot).open(p);
        });
    }
    GuiItem arrowRight(int page) {
        return FileManager.getGuiItem("player-auctions", "next-page", Material.ARROW, "&r&e&lNext Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, target, page+1, snapshot).open(p);
        });
    }
    private void fillGui(Gui gui, Player p, int page, List<Integer> listingSlots) {
        int itemsPerPage = listingSlots.size();
        int min = itemsPerPage*(page-1);
        int maxIndex = itemsPerPage*page;

        int guiSlotIndex = 0;
        for (int i = min; i < maxIndex; i++) {
            if (contents.size() > i) {
                BINAuction bA = contents.get(i);
                if (guiSlotIndex >= listingSlots.size()) break;
                gui.setItem(listingSlots.get(guiSlotIndex), buyableItem(bA, p.hasPermission("nah.menu.manage"), bA.getSeller().equals(p.getUniqueId())));
                guiSlotIndex++;
            }
        }
    }
    GuiItem buyableItem(BINAuction bA, Boolean staff, Boolean self) {
        ItemStack copy = bA.getItem().clone();
        boolean shulker = copy.getType().name().contains("SHULKER_BOX");
        List<Component> lore = new ArrayList<>();
        if (!shulker) {
            lore.addAll(Util.loreToComps(bA.getItem()));
        }
        lore.add(Component.empty());
        lore.add(Lang.translateComp("price", Long.toString(bA.getPrice())));
        lore.add(Lang.translateComp("seller", Util.getName(bA.getSeller())));
        lore.add(Lang.translateComp("expiry", Util.convertSecondsToTime(bA.getExpiry())));
        lore.add(Component.empty());
        lore.add(Lang.translateComp("buyNow"));
        if (shulker) {
            lore.add(Lang.translateComp("shulkerMenu"));
        }
        if (staff) {
            lore.add(Lang.translateComp("staffMenu"));
        }
        if (self) {
            lore.add(Lang.translateComp("own-lore"));
        }
        return ItemBuilder.from(copy)
                .lore(lore)
                .asGuiItem(event -> {
                    Player p = (Player) event.getWhoClicked();
                    BINAuction bAa = NAHUtil.getAuction(bA.getId());
                    boolean right = event.isRightClick();
                    boolean shift = event.isShiftClick();
                    boolean rss = right&&shift&&shulker;

                    if (event.isShiftClick() && p.hasPermission("nah.menu.manage")) {
                        Gui gui = new ManageMenu().gui(bAa);
                        gui.open(p);
                        return;
                    }
                    if (!bAa.getBuyable()) {
                        p.sendMessage(core.prefix()+Lang.translate("already-bought", core));
                        gui(p, target, page, snapshot).open(p);
                        return;
                    }

                    if (rss) {
                        new ShulkerViewerGui().gui(bAa).open(p);
                        return;
                    }

                    if (event.isRightClick() && self) {
                        Gui gui = new ConfirmUnlistGui().gui(p, bAa);
                        gui.open(p);
                        return;
                    }

                    Gui gui = new ConfirmBuyGui().gui(bAa);
                    gui.open(p);
                });
    }
}
