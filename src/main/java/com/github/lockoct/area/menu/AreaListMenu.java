package com.github.lockoct.area.menu;

import com.github.lockoct.Main;
import com.github.lockoct.area.listener.AreaManageMenuListener;
import com.github.lockoct.entity.CollectArea;
import com.github.lockoct.entity.CollectAreaContainer;
import com.github.lockoct.menu.PageableMenu;
import com.github.lockoct.utils.DatabaseUtil;
import com.github.lockoct.utils.I18nUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.pager.Pager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AreaListMenu extends PageableMenu {
    private List<CollectArea> areas;
    private final List<Integer> containerCountList = new ArrayList<>();

    public AreaListMenu(Player player) {
        super(I18nUtil.getText(Main.plugin, player, "areaListMenu.title"), new HashMap<>(), player, Main.plugin);
    }

    public AreaListMenu(String title, Player player) {
        super(title, new HashMap<>(), player, Main.plugin);
    }

    public AreaListMenu(int currentPage, Player player) {
        super(currentPage, I18nUtil.getText(Main.plugin, player, "areaListMenu.title"), new HashMap<>(), player, Main.plugin);
    }

    public AreaListMenu(int currentPage, String title, Player player) {
        super(currentPage, title, new HashMap<>(), player, Main.plugin);
    }

    @Override
    protected void setPageContent(int page) {
        Dao dao = DatabaseUtil.getDao();
        if (dao != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Pager pager = dao.createPager(page, PAGE_SIZE);
                    Cnd cond = Cnd.where("deleted", "=", 0).and("create_user", "=", getPlayer().getUniqueId().toString());
                    areas = dao.query(CollectArea.class, cond.orderBy("create_time", "desc"), pager);
                    pager.setRecordCount(dao.count(CollectArea.class, cond));
                    setTotalPage(pager.getPageCount());
                    setTotal(pager.getRecordCount());
                    // 设置分页
                    setPageElement();
                    // 填充物品
                    for (int i = 0; i < PAGE_SIZE; i++) {
                        Inventory inv = getInventory();
                        if (i < areas.size()) {
                            ItemStack is = new ItemStack(Material.GRASS_BLOCK);
                            ItemMeta im = is.getItemMeta();
                            assert im != null;
                            im.setDisplayName(areas.get(i).getName());
                            // 区域内箱子数量填在附加信息中
                            int containerCount = dao.count(CollectAreaContainer.class, Cnd.where("area_id", "=", areas.get(i).getId()));
                            containerCountList.add(i, containerCount);
                            ArrayList<String> loreList = new ArrayList<>();
                            loreList.add(I18nUtil.getText(Main.plugin, getPlayer(), "areaListMenu.itemInfo", containerCount));
                            im.setLore(loreList);
                            is.setItemMeta(im);
                            inv.setItem(i, is);
                        } else {
                            // 填充空位
                            inv.setItem(i, null);
                        }
                    }
                }
            }.runTaskAsynchronously(Main.plugin);
        }
    }

    // 翻页按钮、分页信息
    @Override
    protected void setPageElement() {
        super.setPageElement();

        Inventory inv = getInventory();

        // 获取分页信息元素
        ItemStack is = inv.getItem(49);
        assert is != null;
        ItemMeta im = is.getItemMeta();
        assert im != null;
        // 分页附加信息
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(I18nUtil.getText(Main.plugin, getPlayer(), "areaListMenu.pageStatisticsInfo", getTotal()));
        im.setLore(loreList);
        is.setItemMeta(im);
        inv.setItem(49, is);
    }

    public void toManageMenu(int index) {
        if (index < PAGE_SIZE) {
            HashMap<String, Object> context = getMenuContext();
            // 区域信息
            context.put("areaInfo", areas.get(index));
            // 区域内箱子数量
            context.put("areaContainerCount", containerCountList.get(index));
            // 列表菜单当前页码
            context.put("fromPage", getCurrentPage());

            AreaManageMenu menu = new AreaManageMenu(getMenuContext(), getPlayer());
            close();
            menu.open(new AreaManageMenuListener(menu));
        }
    }
}
