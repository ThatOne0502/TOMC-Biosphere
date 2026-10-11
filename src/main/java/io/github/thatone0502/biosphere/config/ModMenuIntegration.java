package io.github.thatone0502.biosphere.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.thatone0502.biosphere.client.BiosphereModConfigScreen;

/** ModMenu 集成：让 ModMenu 识别并打开本模组的自定义单页 Cloth Config 配置界面。 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BiosphereModConfigScreen::create;
    }
}
