package com.rotproxy;

import com.rotproxy.config.ProxyConfig;
import com.rotproxy.proxy.ProxyManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class RotProxyMod implements ClientModInitializer {
    public static final String MOD_ID = "rotproxy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RotProxyMod INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        ProxyConfig.load();
        ProxyManager.initialize();
        ProxyManager.restoreConfiguredProxy();
        LOGGER.info("RotProxy initialized.");
    }

    public static RotProxyMod getInstance() {
        return INSTANCE;
    }
}
