package io.github.thatone0502.biosphere.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/** 服务端权威配置：外置 config/biosphere.json，由 ModMenu（Cloth Config）编辑。 */
@Config(name = "biosphere")
public class BiosphereConfig implements ConfigData {

    @ConfigEntry.Category("modules")
    @ConfigEntry.Gui.Tooltip
    public boolean passiveAttackEnabled = true;

    @ConfigEntry.Category("modules")
    @ConfigEntry.Gui.Tooltip
    public boolean environmentalChangeEnabled = true;

    @ConfigEntry.Category("modules")
    @ConfigEntry.Gui.Tooltip
    public boolean spawnDataEnabled = true;

    @ConfigEntry.Category("passive_attack")
    @ConfigEntry.Gui.Tooltip
    public float meleeDamage = 3.0f;

    @ConfigEntry.Category("passive_attack")
    @ConfigEntry.Gui.Tooltip
    public double meleeSpeed = 1.5;

    @ConfigEntry.Category("passive_attack")
    @ConfigEntry.Gui.Tooltip
    public int meleeCooldownTicks = 20;

    @ConfigEntry.Category("passive_attack")
    @ConfigEntry.Gui.Tooltip
    public double avoidSpeed = 1.2;

    @ConfigEntry.Category("modules")
    @ConfigEntry.Gui.Tooltip
    public double mushroomConversionChance = 0.2;

    @ConfigEntry.Category("creefern")
    @ConfigEntry.Gui.Tooltip
    public boolean creefernOverride = false;

    @ConfigEntry.Category("creefern")
    @ConfigEntry.Gui.Tooltip
    public boolean creefernForceOverride = false;

    @ConfigEntry.Category("creefern")
    @ConfigEntry.Gui.Tooltip
    public boolean creefernAuto = false;

    @ConfigEntry.Category("creefern")
    @ConfigEntry.Gui.Tooltip
    public float creefernMinHealth = 20.0f;

    @ConfigEntry.Category("creefern")
    @ConfigEntry.Gui.Tooltip
    public float creefernMaxHealth = 64.5f;

    /**
     * 苦力怕新设计有效开关：
     * override=false 时以 auto 为准；override=true 时 forceOverride 生效
     * （true=强制开启，false=强制跳过）。
     */
    public boolean creefernEffective() {
        return creefernOverride ? creefernForceOverride : creefernAuto;
    }
}
