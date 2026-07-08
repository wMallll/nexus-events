package com.nexusevents.event.hideandseek;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TextUtil;
import com.nexusevents.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Configuracion tipada del evento "Escondete si puedes", parseada desde
 * {@code events/hide-and-seek.yml}.
 *
 * <p>El material y los encantamientos del arma se resuelven con XSeries,
 * por lo que se escriben con nomenclatura moderna y funcionan en
 * cualquier version desde 1.9. Toda entrada invalida degrada a un valor
 * seguro con warning en consola.</p>
 */
public final class HideAndSeekConfig {

    private final int hideSeconds;
    private final int releaseSeconds;
    private final int maxDurationSeconds;
    private final int hunterCount;
    private final int reconnectWindowSeconds;
    private final boolean requireWeapon;
    private final boolean treatQuitAsVoluntary;

    private final XMaterial weaponMaterial;
    private final String weaponName;
    private final List<String> weaponLore;
    private final List<String> weaponEnchantments;

    private final TitleConfig hidingTitle;
    private final TitleConfig releaseTitle;
    private final TitleConfig hunterAssignedTitle;
    private final TitleConfig huntStartedTitle;

    private final String actionbarHiding;
    private final String actionbarRelease;
    private final String actionbarHunting;

    private HideAndSeekConfig(int hideSeconds, int releaseSeconds, int maxDurationSeconds,
                              int hunterCount, int reconnectWindowSeconds,
                              boolean requireWeapon, boolean treatQuitAsVoluntary,
                              XMaterial weaponMaterial, String weaponName,
                              List<String> weaponLore, List<String> weaponEnchantments,
                              TitleConfig hidingTitle, TitleConfig releaseTitle,
                              TitleConfig hunterAssignedTitle, TitleConfig huntStartedTitle,
                              String actionbarHiding, String actionbarRelease, String actionbarHunting) {
        this.hideSeconds = hideSeconds;
        this.releaseSeconds = releaseSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.hunterCount = hunterCount;
        this.reconnectWindowSeconds = reconnectWindowSeconds;
        this.requireWeapon = requireWeapon;
        this.treatQuitAsVoluntary = treatQuitAsVoluntary;
        this.weaponMaterial = weaponMaterial;
        this.weaponName = weaponName;
        this.weaponLore = Collections.unmodifiableList(weaponLore);
        this.weaponEnchantments = Collections.unmodifiableList(weaponEnchantments);
        this.hidingTitle = hidingTitle;
        this.releaseTitle = releaseTitle;
        this.hunterAssignedTitle = hunterAssignedTitle;
        this.huntStartedTitle = huntStartedTitle;
        this.actionbarHiding = actionbarHiding;
        this.actionbarRelease = actionbarRelease;
        this.actionbarHunting = actionbarHunting;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/hide-and-seek.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static HideAndSeekConfig parse(FileConfiguration file, Logger logger) {
        XMaterial material = XMaterial.matchXMaterial(file.getString("weapon.material", "STICK"))
                .orElse(null);
        if (material == null || material.parseMaterial() == null) {
            logger.warning("hide-and-seek: material de arma invalido '"
                    + file.getString("weapon.material") + "'. Se usa STICK.");
            material = XMaterial.STICK;
        }

        return new HideAndSeekConfig(
                TimeUtil.parseSeconds(file.getString("settings.hide-duration", ""), 30),
                TimeUtil.parseSeconds(file.getString("settings.release-countdown", ""), 5),
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 300),
                Math.max(1, file.getInt("settings.hunter-count", 1)),
                TimeUtil.parseSeconds(file.getString("settings.reconnect-window", ""), 60),
                file.getBoolean("settings.require-weapon", true),
                file.getBoolean("settings.treat-quit-as-voluntary", true),
                material,
                file.getString("weapon.name", "<red><bold>Cazador"),
                file.getStringList("weapon.lore"),
                file.getStringList("weapon.enchantments"),
                titleOr(file, "titles.hiding", "<green><bold><seconds>", "<gray>¡Escondete rápido!"),
                titleOr(file, "titles.release", "<red><bold><seconds>", "<gray>El cazador sale a cazar..."),
                titleOr(file, "titles.hunter-assigned", "<red><bold>SOS EL CAZADOR", "<gray>Esperá a que se escondan"),
                titleOr(file, "titles.hunt-started", "<red><bold>¡EL CAZADOR SALIÓ!", "<gray>Que no te atrape"),
                file.getString("actionbar.hiding", "<green>Tiempo para esconderse: <yellow><time>"),
                file.getString("actionbar.release", "<red>El cazador sale en <yellow><time>"),
                file.getString("actionbar.hunting",
                        "<gray>Tiempo: <yellow><time> <dark_gray>| <gray>Escondidos: <green><alive>")
        );
    }

    private static TitleConfig titleOr(FileConfiguration file, String path,
                                       String fallbackTitle, String fallbackSubtitle) {
        ConfigurationSection section = file.getConfigurationSection(path);
        TitleConfig parsed = TitleConfig.parse(section);
        if (section == null || !parsed.isEnabled()) {
            return TitleConfig.of(fallbackTitle, fallbackSubtitle, 0, 25, 5);
        }
        return parsed;
    }

    /**
     * Construye el arma del cazador con nombre, lore y encantamientos
     * configurados.
     *
     * @param logger logger para encantamientos invalidos.
     * @return item listo para entregar.
     */
    public ItemStack buildWeapon(Logger logger) {
        ItemStack item = weaponMaterial.parseItem();
        if (item == null) {
            item = new ItemStack(Material.STICK);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.toLegacy(weaponName));
            List<String> lore = new ArrayList<>();
            for (String line : weaponLore) {
                lore.add(TextUtil.toLegacy(line));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, logger);
        return item;
    }

    private void applyEnchantments(ItemStack item, Logger logger) {
        for (String entry : weaponEnchantments) {
            String[] parts = entry.split(":");
            int level = parts.length > 1 ? parseLevel(parts[1]) : 1;
            Enchantment enchantment = XEnchantment.matchXEnchantment(parts[0].trim().toUpperCase(Locale.ROOT))
                    .map(XEnchantment::getEnchant)
                    .orElse(null);
            if (enchantment == null) {
                logger.warning("hide-and-seek: encantamiento invalido '" + entry + "'. Se ignora.");
                continue;
            }
            item.addUnsafeEnchantment(enchantment, level);
        }
    }

    private static int parseLevel(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    /**
     * Indica si el item dado es el arma configurada del cazador.
     *
     * @param item item en mano del atacante.
     * @return true si coincide con el material configurado.
     */
    public boolean isWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return XMaterial.matchXMaterial(item) == weaponMaterial;
    }

    public int getHideSeconds() {
        return hideSeconds;
    }

    public int getReleaseSeconds() {
        return releaseSeconds;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public int getHunterCount() {
        return hunterCount;
    }

    public int getReconnectWindowSeconds() {
        return reconnectWindowSeconds;
    }

    public boolean isRequireWeapon() {
        return requireWeapon;
    }

    public boolean isTreatQuitAsVoluntary() {
        return treatQuitAsVoluntary;
    }

    public TitleConfig getHidingTitle() {
        return hidingTitle;
    }

    public TitleConfig getReleaseTitle() {
        return releaseTitle;
    }

    public TitleConfig getHunterAssignedTitle() {
        return hunterAssignedTitle;
    }

    public TitleConfig getHuntStartedTitle() {
        return huntStartedTitle;
    }

    public String getActionbarHiding() {
        return actionbarHiding;
    }

    public String getActionbarRelease() {
        return actionbarRelease;
    }

    public String getActionbarHunting() {
        return actionbarHunting;
    }
}
