package com.nexusevents;

import com.nexusevents.bootstrap.PluginBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Punto de entrada del plugin.
 *
 * <p>Se mantiene deliberadamente minima: toda la logica de arranque,
 * registro de managers y apagado vive en {@link PluginBootstrap}.</p>
 */
public final class NexusEventsPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new PluginBootstrap(this);
        this.bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.disable();
            this.bootstrap = null;
        }
    }

    public PluginBootstrap getBootstrap() {
        return this.bootstrap;
    }
}
