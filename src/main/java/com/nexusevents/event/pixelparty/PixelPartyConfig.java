package com.nexusevents.event.pixelparty;

import com.cryptomorin.xseries.XMaterial;
import com.nexusevents.configuration.model.TitleConfig;
import com.nexusevents.util.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuracion tipada de Pixel Party, parseada desde
 * {@code events/pixel-party.yml}.
 *
 * <p>La paleta usa nomenclatura moderna de materiales (RED_WOOL, etc.)
 * y se traduce automaticamente a lana con data values en 1.9 - 1.12.
 * Si la paleta configurada es invalida o tiene menos de dos colores,
 * se usa una paleta por defecto de ocho colores con warning.</p>
 */
public final class PixelPartyConfig {

    /**
     * Entrada de la paleta: material del bloque, nombre visible y tag
     * de color MiniMessage para los titulos.
     */
    public static final class PaletteColor {
        private final XMaterial material;
        private final String name;
        private final String colorTag;

        PaletteColor(XMaterial material, String name, String colorTag) {
            this.material = material;
            this.name = name;
            this.colorTag = colorTag;
        }

        public XMaterial getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public String getColorTag() {
            return colorTag;
        }
    }

    private final int maxDurationSeconds;
    private final int tileSize;
    private final int roundTimeStartSeconds;
    private final int roundTimeMinSeconds;
    private final int roundTimeDecreaseSeconds;
    private final int pauseSeconds;
    private final int decisionMinSeconds;
    private final int decisionMaxSeconds;
    private final int fallDistance;
    private final boolean giveTargetItem;
    private final List<PaletteColor> palette;
    private final TitleConfig colorTitle;
    private final TitleConfig clearedTitle;
    private final String actionbarShowing;
    private final String actionbarPause;
    private final String actionbarDeciding;

    private PixelPartyConfig(int maxDurationSeconds, int tileSize, int roundTimeStartSeconds,
                             int roundTimeMinSeconds, int roundTimeDecreaseSeconds, int pauseSeconds,
                             int decisionMinSeconds, int decisionMaxSeconds,
                             int fallDistance, boolean giveTargetItem, List<PaletteColor> palette,
                             TitleConfig colorTitle, TitleConfig clearedTitle,
                             String actionbarShowing, String actionbarPause, String actionbarDeciding) {
        this.maxDurationSeconds = maxDurationSeconds;
        this.tileSize = tileSize;
        this.roundTimeStartSeconds = roundTimeStartSeconds;
        this.roundTimeMinSeconds = roundTimeMinSeconds;
        this.roundTimeDecreaseSeconds = roundTimeDecreaseSeconds;
        this.pauseSeconds = pauseSeconds;
        this.decisionMinSeconds = decisionMinSeconds;
        this.decisionMaxSeconds = decisionMaxSeconds;
        this.fallDistance = fallDistance;
        this.giveTargetItem = giveTargetItem;
        this.palette = Collections.unmodifiableList(palette);
        this.colorTitle = colorTitle;
        this.clearedTitle = clearedTitle;
        this.actionbarShowing = actionbarShowing;
        this.actionbarPause = actionbarPause;
        this.actionbarDeciding = actionbarDeciding;
    }

    /**
     * Parsea la configuracion completa del evento.
     *
     * @param file   archivo events/pixel-party.yml.
     * @param logger logger para valores invalidos.
     * @return configuracion validada.
     */
    public static PixelPartyConfig parse(FileConfiguration file, Logger logger) {
        return new PixelPartyConfig(
                TimeUtil.parseSeconds(file.getString("settings.max-duration", ""), 240),
                Math.max(1, file.getInt("settings.tile-size", 3)),
                Math.max(1, TimeUtil.parseSeconds(file.getString("settings.round-time-start", ""), 8)),
                Math.max(1, TimeUtil.parseSeconds(file.getString("settings.round-time-min", ""), 2)),
                Math.max(0, TimeUtil.parseSeconds(file.getString("settings.round-time-decrease", ""), 1)),
                Math.max(1, TimeUtil.parseSeconds(file.getString("settings.pause-between-rounds", ""), 4)),
                Math.max(1, TimeUtil.parseSeconds(file.getString("settings.decision-time-min", ""), 2)),
                Math.max(1, TimeUtil.parseSeconds(file.getString("settings.decision-time-max", ""), 3)),
                Math.max(1, file.getInt("settings.fall-distance", 4)),
                file.getBoolean("settings.give-target-item", true),
                parsePalette(file, logger),
                titleOr(file, "titles.color", "<color><bold><colorname>", "<gray>¡Buscá ese color!"),
                titleOr(file, "titles.cleared", "<red><bold>¡AHORA!", "<gray>Los bloques incorrectos desaparecieron"),
                file.getString("actionbar.showing",
                        "<gray>Ronda <yellow><round> <dark_gray>| <color><colorname> <dark_gray>| <gray><time> <dark_gray>| <gray>Vivos: <green><alive>"),
                file.getString("actionbar.pause",
                        "<gray>Ronda <yellow><round> <dark_gray>| <gray>Siguiente color en <yellow><time> <dark_gray>| <gray>Vivos: <green><alive>"),
                file.getString("actionbar.deciding",
                        "<gray>Ronda <yellow><round> <dark_gray>| <light_purple>Eligiendo color... <dark_gray>| <gray>Vivos: <green><alive>")
        );
    }

    private static List<PaletteColor> parsePalette(FileConfiguration file, Logger logger) {
        List<PaletteColor> palette = new ArrayList<>();
        for (Map<?, ?> entry : file.getMapList("palette")) {
            Object material = entry.get("material");
            Object name = entry.get("name");
            Object color = entry.get("color");
            if (material == null) {
                continue;
            }
            XMaterial resolved = XMaterial.matchXMaterial(String.valueOf(material)).orElse(null);
            if (resolved == null || resolved.parseMaterial() == null) {
                logger.warning("pixel-party: material de paleta invalido '" + material + "'. Se ignora.");
                continue;
            }
            palette.add(new PaletteColor(resolved,
                    name != null ? String.valueOf(name) : resolved.name(),
                    color != null ? String.valueOf(color) : "<white>"));
        }
        if (palette.size() < 2) {
            logger.warning("pixel-party: la paleta configurada tiene menos de 2 colores validos. "
                    + "Se usa la paleta por defecto.");
            return defaultPalette();
        }
        return palette;
    }

    private static List<PaletteColor> defaultPalette() {
        List<PaletteColor> palette = new ArrayList<>();
        palette.add(new PaletteColor(XMaterial.RED_WOOL, "Rojo", "<red>"));
        palette.add(new PaletteColor(XMaterial.ORANGE_WOOL, "Naranja", "<gold>"));
        palette.add(new PaletteColor(XMaterial.YELLOW_WOOL, "Amarillo", "<yellow>"));
        palette.add(new PaletteColor(XMaterial.LIME_WOOL, "Verde", "<green>"));
        palette.add(new PaletteColor(XMaterial.LIGHT_BLUE_WOOL, "Celeste", "<aqua>"));
        palette.add(new PaletteColor(XMaterial.BLUE_WOOL, "Azul", "<blue>"));
        palette.add(new PaletteColor(XMaterial.PURPLE_WOOL, "Violeta", "<dark_purple>"));
        palette.add(new PaletteColor(XMaterial.PINK_WOOL, "Rosa", "<light_purple>"));
        return palette;
    }

    private static TitleConfig titleOr(FileConfiguration file, String path,
                                       String fallbackTitle, String fallbackSubtitle) {
        ConfigurationSection section = file.getConfigurationSection(path);
        TitleConfig parsed = TitleConfig.parse(section);
        if (section == null || !parsed.isEnabled()) {
            return TitleConfig.of(fallbackTitle, fallbackSubtitle, 0, 30, 5);
        }
        return parsed;
    }

    /**
     * Tiempo de busqueda de color para una ronda dada, con la
     * aceleracion configurada aplicada.
     *
     * @param round numero de ronda (desde 1).
     * @return segundos de la ronda.
     */
    public int roundTimeFor(int round) {
        int time = roundTimeStartSeconds - (round - 1) * roundTimeDecreaseSeconds;
        return Math.max(roundTimeMinSeconds, time);
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getPauseSeconds() {
        return pauseSeconds;
    }

    /**
     * Suspenso minimo de la fase de decision del color.
     *
     * @return segundos minimos.
     */
    public int getDecisionMinSeconds() {
        return decisionMinSeconds;
    }

    /**
     * Suspenso maximo de la fase de decision del color.
     *
     * @return segundos maximos (la duracion real es aleatoria).
     */
    public int getDecisionMaxSeconds() {
        return Math.max(decisionMinSeconds, decisionMaxSeconds);
    }

    public int getFallDistance() {
        return fallDistance;
    }

    public boolean isGiveTargetItem() {
        return giveTargetItem;
    }

    public List<PaletteColor> getPalette() {
        return palette;
    }

    public TitleConfig getColorTitle() {
        return colorTitle;
    }

    public TitleConfig getClearedTitle() {
        return clearedTitle;
    }

    public String getActionbarShowing() {
        return actionbarShowing;
    }

    public String getActionbarPause() {
        return actionbarPause;
    }

    public String getActionbarDeciding() {
        return actionbarDeciding;
    }
}
