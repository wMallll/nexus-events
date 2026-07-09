package com.nexusevents.moderation;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * El Palo Descalificador: item de moderacion identificado por su
 * nombre exacto. Solo los golpes dados con ESTE item (y con el permiso
 * correspondiente) descalifican; cualquier otro palo es un palo.
 */
public final class DisqualifyStick {

    private static final String NAME = "<red><bold>Descalificador";

    private DisqualifyStick() {
    }

    /**
     * Construye el palo descalificador (encantado, con nombre y lore).
     *
     * @return item listo para entregar.
     */
    @SuppressWarnings("deprecation")
    public static ItemStack create() {
        ItemStack parsed = XMaterial.STICK.parseItem();
        final ItemStack stick = parsed != null ? parsed : new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.toLegacy(NAME));
            meta.setLore(Arrays.asList(
                    TextUtil.toLegacy("<gray>Golpeá a un participante vivo"),
                    TextUtil.toLegacy("<gray>para descalificarlo del evento.")));
            stick.setItemMeta(meta);
        }
        XEnchantment.matchXEnchantment("unbreaking").ifPresent(enchant -> {
            Enchantment bukkit = enchant.getEnchant();
            if (bukkit != null) {
                stick.addUnsafeEnchantment(bukkit, 1);
            }
        });
        return stick;
    }

    /**
     * Indica si el item es el palo descalificador legitimo.
     *
     * @param item item a verificar.
     * @return true si coincide por nombre.
     */
    @SuppressWarnings("deprecation")
    public static boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && TextUtil.toLegacy(NAME).equals(meta.getDisplayName());
    }
}
