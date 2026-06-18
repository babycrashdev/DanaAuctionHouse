package cc.synkdev.nah.gui;

import cc.synkdev.nah.NexusAuctionHouse;
import cc.synkdev.nah.api.NAHUtil;
import cc.synkdev.nah.manager.Util;
import cc.synkdev.nah.objects.BINAuction;
import cc.synkdev.nah.objects.ItemSort;
import cc.synkdev.nah.objects.SortingTypes;
import cc.synkdev.nah.manager.Lang;
import cc.synkdev.nah.manager.FileManager;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.builder.item.SkullBuilder;
import dev.triumphteam.gui.builder.item.BaseItemBuilder;
import dev.triumphteam.gui.components.util.GuiFiller;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainGui {
    NexusAuctionHouse core = NexusAuctionHouse.getInstance();
    int page;
    String searchS;
    int firstSort;
    ItemSort itSort;
    Player p;
    MainGuiSnapshot snapshot;
    public Gui gui(Player p, int page, String search, int firstSort, ItemSort itSort) {
        snapshot = new MainGuiSnapshot(page, search, firstSort, itSort);
        this.p = p;
        core.checkExpiry.run();
        this.page = page;
        this.searchS = search;
        this.firstSort = firstSort;
        this.itSort = itSort;
        SortingTypes sort = core.playerSortingTypes.getOrDefault(p.getUniqueId(), SortingTypes.PRICEMIN);

        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.main.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                if (i % 9 != 0) listingSlots.add(i);
            }
        }
        int itemsPerPage = listingSlots.size();
        int max = (core.runningBINs.size()+ (itemsPerPage - 1)) / itemsPerPage;

        Gui gui = Gui.gui()
                .disableAllInteractions()
                .title(FileManager.getGuiTitle("main", "&eAuction House"))
                .rows(FileManager.getGuiRows("main", 6))
                .create();

        gui.getFiller().fill(FileManager.getFillerItem("main"));

        int prevPageSlot = FileManager.getGuiSlot("main", "previous-page", 48);
        int nextPageSlot = FileManager.getGuiSlot("main", "next-page", 50);

        if (page > 1 && prevPageSlot >= 0) {
            gui.setItem(prevPageSlot, arrowLeft(page));
        }
        if (page < max && nextPageSlot >= 0) {
            gui.setItem(nextPageSlot, arrowRight(page));
        }

        int sorterSlot = FileManager.getGuiSlot("main", "sort", 52);
        if (sorterSlot >= 0) {
            gui.setItem(sorterSlot, sorter(p, page, search));
        }

        int scrollUpSlot = FileManager.getGuiSlot("main", "sorts-scroll-up", 0);
        if (firstSort > 0 && scrollUpSlot >= 0) {
            gui.setItem(scrollUpSlot, FileManager.getGuiItem("main", "sorts-scroll-up", Material.ARROW, "&eSorts", Arrays.asList("", "&eClick to scroll"), event -> {
                gui(p, page, search, firstSort - 1, itSort).open(p);
            }));
        }

        boolean useFirst = firstSort<=0;
        int scrollDownSlot = FileManager.getGuiSlot("main", "sorts-scroll-down", 45);
        if (core.itemSorts.size() > 5 && core.itemSorts.size() >= firstSort+6 && scrollDownSlot >= 0) {
            gui.setItem(scrollDownSlot, FileManager.getGuiItem("main", "sorts-scroll-down", Material.ARROW, "&eSorts", Arrays.asList("", "&eClick to scroll"), event -> {
                gui(p, page, search, firstSort + 1, itSort).open(p);
            }));
        }

        int index = firstSort;
        int startRow = useFirst ? 1 : 2;
        for (int i = startRow; i < 6; i++) {
            if (core.itemSorts.size() <= index) break;

            ItemSort iSort = core.itemSorts.entrySet().stream().toList().get(index).getValue();
            boolean same = iSort == itSort;
            int categorySlot = (i - 1) * 9;
            gui.setItem(categorySlot, ItemBuilder.from(iSort.getIcon())
                    .glow(same)
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .name(FileManager.parseMiniMessage("<yellow>" + iSort.getName()))
                    .lore(Component.empty(), Lang.translateComp(same ? "clickUnselect" : "clickSelect"))
                    .asGuiItem(event -> {
                        if (same) {
                            gui(p, page, search, firstSort, null).open(p);
                        } else {
                            gui(p, page, search, firstSort, iSort).open(p);
                        }
                    }));
            index++;
        }

        if (search != null) {
            fillGui(gui, p, page, search);
        } else if (!core.itemSorts.isEmpty()) {
            fillGui(gui, p, page, sort, itSort);
        } else {
            fillGui(gui, p, page, sort);
        }

        int searchSlot = FileManager.getGuiSlot("main", "search", 46);
        if (searchSlot >= 0) {
            gui.setItem(searchSlot, search());
        }

        int retrieveSlot = FileManager.getGuiSlot("main", "retrieve", 53);
        if (retrieveSlot >= 0) {
            gui.setItem(retrieveSlot, FileManager.getGuiItemBuilder("main", "retrieve", Material.CHEST, "&r&e&lRetrieve Items", null)
                    .lore(Component.empty(), Lang.translateComp("retrieveCount", core.retrieveMap.getOrDefault(p.getUniqueId(), new ArrayList<>()).size() + ""), Component.empty(), Lang.translateComp("clickBrowse"))
                    .asGuiItem(event -> {
                        NAHUtil.openExpiredGui(p);
                    }));
        }

        int viewOwnSlot = FileManager.getGuiSlot("main", "view-own", 49);
        if (p.hasPermission("nah.menu.player.own") && viewOwnSlot >= 0) {
            BaseItemBuilder<?> builder = FileManager.getGuiItemBuilder("main", "view-own", Material.PLAYER_HEAD, "&r&eView Your Listings", null);
            if (builder instanceof SkullBuilder skullBuilder) {
                gui.setItem(viewOwnSlot, skullBuilder.owner(p).asGuiItem(event -> {
                    if (event.getWhoClicked().hasPermission("nah.menu.player.own")) {
                        NAHUtil.openPlayerListings(p, p, new MainGuiSnapshot(page, search, firstSort, itSort));
                    } else {
                        event.getWhoClicked().sendMessage(core.prefix()+ChatColor.RED+Lang.translate("noPerm", core));
                    }
                }));
            } else {
                gui.setItem(viewOwnSlot, builder.asGuiItem(event -> {
                    if (event.getWhoClicked().hasPermission("nah.menu.player.own")) {
                        NAHUtil.openPlayerListings(p, p, new MainGuiSnapshot(page, search, firstSort, itSort));
                    } else {
                        event.getWhoClicked().sendMessage(core.prefix()+ChatColor.RED+Lang.translate("noPerm", core));
                    }
                }));
            }
        }
        return gui;
    }
    GuiItem arrowLeft(int page) {
        return FileManager.getGuiItem("main", "previous-page", Material.ARROW, "&r&e&lPrevious Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, page-1, null, 0, itSort).open(p);
        });
    }
    GuiItem arrowRight(int page) {
        return FileManager.getGuiItem("main", "next-page", Material.ARROW, "&r&e&lNext Page", null, inventoryClickEvent -> {
            Player p = (Player) inventoryClickEvent.getWhoClicked();
            gui(p, page+1, null, 0, itSort).open(p);
        });
    }
    private void fillGui(Gui gui, Player p, int page, SortingTypes sort, ItemSort itemSort) {
        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.main.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                if (i % 9 != 0) listingSlots.add(i);
            }
        }
        int min = listingSlots.size() * (page - 1);
        int max = listingSlots.size() * page;

        int guiSlotIndex = 0;

        for (int i = min; i < max; i++) {
            if (sort.list.size() <= i) break;

            BINAuction bA = sort.list.get(i);
            if (itemSort != null && !itemSort.getContents().contains(bA.getItem().getType())) {
                continue;
            }

            if (guiSlotIndex >= listingSlots.size()) break;
            int slot = listingSlots.get(guiSlotIndex);

            gui.setItem(slot, buyableItem(bA, p.hasPermission("nah.menu.manage"), bA.getSeller().equals(p.getUniqueId())));
            guiSlotIndex++;
        }
    }
    private void fillGui(Gui gui, Player p, int page, SortingTypes sort) {
        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.main.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                if (i % 9 != 0) listingSlots.add(i);
            }
        }
        int min = listingSlots.size()*(page-1);
        int max = listingSlots.size()*page;

        int guiSlotIndex = 0;
        for (int i = min; i < max; i++) {
            if (sort.list.size() > i) {
                BINAuction bA = sort.list.get(i);
                if (guiSlotIndex >= listingSlots.size()) break;
                gui.setItem(listingSlots.get(guiSlotIndex), buyableItem(bA, p.hasPermission("nah.menu.manage"), bA.getSeller().equals(p.getUniqueId())));
                guiSlotIndex++;
            }
        }
    }

    private void fillGui(Gui gui, Player p, int page, String research) {
        List<Integer> listingSlots = FileManager.getGui().getIntegerList("menus.main.listing-slots");
        if (listingSlots.isEmpty()) {
            listingSlots = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                if (i % 9 != 0) listingSlots.add(i);
            }
        }
        int min = listingSlots.size()*(page-1);
        int max = listingSlots.size()*page;

        List<BINAuction> list = new ArrayList<>(Util.searchList(research, core.playerSortingTypes.getOrDefault(p.getUniqueId(), SortingTypes.PRICEMIN)));
        int guiSlotIndex = 0;

        for (int i = min; i < max; i++) {
            if (list.size() <= i) break;

            BINAuction bA = list.get(i);

            if (guiSlotIndex >= listingSlots.size()) break;
            int slot = listingSlots.get(guiSlotIndex);

            gui.setItem(slot, buyableItem(bA, p.hasPermission("nah.menu.manage"), bA.getSeller().equals(p.getUniqueId())));
            guiSlotIndex++;
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
        if (p.hasPermission("nah.manage.unlist.own") && self) {
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
                        NAHUtil.open(p, false, searchS, page);
                        return;
                    }

                    if (rss) {
                        new ShulkerViewerGui().gui(bAa).open(p);
                        return;
                    }

                    if (event.isRightClick() && self && p.hasPermission("nah.manage.unlist.own")) {
                        Gui gui = new ConfirmUnlistGui().gui(p, bAa);
                        gui.open(p);
                        return;
                    }

                    Gui gui = new ConfirmBuyGui().gui(bAa);
                    gui.open(p);
                });
    }
    GuiItem sorter(Player p, int page, String search) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (SortingTypes sT : core.sortingTypes) {
            String arrow = ChatColor.RESET+"  "+ChatColor.YELLOW+"-> "+ChatColor.BOLD;
            if (core.playerSortingTypes.getOrDefault(p.getUniqueId(), SortingTypes.PRICEMIN) == sT) {
                lore.add(Component.text(arrow+ChatColor.GOLD+ChatColor.BOLD+sT.string));
            } else {
                lore.add(Component.text(ChatColor.RESET+"  "+ChatColor.YELLOW+sT.string));
            }
        }
        lore.add(Component.empty());
        lore.add(Lang.translateComp("clickScroll"));

        return FileManager.getGuiItemBuilder("main", "sort", Material.HOPPER, "&r&e&lSort Options", null)
                .lore(lore)
                .asGuiItem(event -> {
                    Player pl = (Player) event.getWhoClicked();
                    switch (core.playerSortingTypes.getOrDefault(pl.getUniqueId(), SortingTypes.PRICEMIN)) {
                        case PRICEMAX:
                            if (!core.playerSortingTypes.containsKey(pl.getUniqueId())) {
                                core.playerSortingTypes.put(pl.getUniqueId(), SortingTypes.LATESTPOSTED);
                            } else {
                                core.playerSortingTypes.replace(pl.getUniqueId(), SortingTypes.LATESTPOSTED);
                            }
                            break;
                        case LATESTPOSTED:
                            if (!core.playerSortingTypes.containsKey(pl.getUniqueId())) {
                                core.playerSortingTypes.put(pl.getUniqueId(), SortingTypes.EXPIRESSOON);
                            } else {
                                core.playerSortingTypes.replace(pl.getUniqueId(), SortingTypes.EXPIRESSOON);
                            }
                            break;
                        case EXPIRESSOON:
                            if (!core.playerSortingTypes.containsKey(pl.getUniqueId())) {
                                core.playerSortingTypes.put(pl.getUniqueId(), SortingTypes.PRICEMIN);
                            } else {
                                core.playerSortingTypes.replace(pl.getUniqueId(), SortingTypes.PRICEMIN);
                            }
                            break;
                        case PRICEMIN:
                            if (!core.playerSortingTypes.containsKey(pl.getUniqueId())) {
                                core.playerSortingTypes.put(pl.getUniqueId(), SortingTypes.PRICEMAX);
                            } else {
                                core.playerSortingTypes.replace(pl.getUniqueId(), SortingTypes.PRICEMAX);
                            }
                            break;
                    }
                    gui(p, page, search, 0, itSort).open(p);
                });
    }
    GuiItem search() {
        List<Component> lore = new ArrayList<>();
        if (searchS != null) {
            lore.add(Component.empty());
            lore.add(Lang.translateComp("currSearch", searchS));
            lore.add(Lang.translateComp("searchReset"));
        }
        lore.add(Component.empty());
        lore.add(Lang.translateComp("clickSearch"));
        return FileManager.getGuiItemBuilder("main", "search", Material.OAK_SIGN, "&r&eSearch", null)
                .lore(lore)
                .asGuiItem(event -> {
                    Player p = (Player) event.getWhoClicked();
                    if (event.isRightClick() && searchS != null) {
                        NAHUtil.open(p, false, null, 1);
                        return;
                    }


                    AnvilGUI.Builder anvil = new AnvilGUI.Builder();
                    anvil.plugin(core);
                    anvil.itemLeft(new ItemStack(Material.PAPER));

                    ItemStack out = new ItemStack(Material.PAPER);
                    ItemMeta meta = out.getItemMeta();
                    meta.displayName(Lang.translateComp("search"));
                    out.setItemMeta(meta);
                    anvil.itemOutput(out);

                    anvil.text(Lang.translate("enterSearch", core));
                    anvil.onClick((integer, stateSnapshot) -> {
                        if (integer != 2) {
                            return List.of();
                        }
                        p.closeInventory();
                        NAHUtil.open(p, false, stateSnapshot.getText(), 1);
                        return List.of();
                    });
                    anvil.open(p);
                });
    }
}
