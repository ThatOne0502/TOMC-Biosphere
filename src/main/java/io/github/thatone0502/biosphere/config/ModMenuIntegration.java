package io.github.thatone0502.biosphere.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

/** ModMenu 集成：让 ModMenu 识别并打开本模组的 Cloth Config 配置界面。 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(BiosphereConfig.class, parent).get();
    }
}
