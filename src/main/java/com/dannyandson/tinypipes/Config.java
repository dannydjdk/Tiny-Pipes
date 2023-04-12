package com.dannyandson.tinypipes;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Config {

    public static ForgeConfigSpec SERVER_CONFIG;

    public static final String CATEGORY_BALANCE = "balance";
    public static ForgeConfigSpec.IntValue ENERGY_THROUGHPUT;
    public static ForgeConfigSpec.IntValue FLUID_THROUGHPUT;
    public static ForgeConfigSpec.IntValue ITEM_THROUGHPUT;
    public static ForgeConfigSpec.DoubleValue SPEED_UPGRADE_MULTIPLIER;
    public static ForgeConfigSpec.IntValue SPEED_UPGRADE_MAX;
    public static ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();

        serverBuilder.comment("Balance Settings").push(CATEGORY_BALANCE);

        ENERGY_THROUGHPUT = serverBuilder.comment("""
                        How much forge energy per tick should each connection allow before upgrades?
                        Keep in mind that multiple tiny pipes can be connected to each block (up to 64), and pipes have unlimited throughput.
                        Full sized pipes can be upgrades up to 32x this base speed (by default).
                        (default: 256 fe/tick)""")
                .defineInRange("fe_per_tick", 256, 1, Integer.MAX_VALUE);

        FLUID_THROUGHPUT = serverBuilder.comment("""
                        How many millibuckets of fluid per second should each connection allow before upgrades? (1000mB = 1 bucket)
                        Keep in mind that multiple tiny pipes can be connected to each block (up to 64), and pipes have unlimited throughput.
                        Full sized pipes can be upgrades up to 32x this base speed (by default).
                        (default: 500mB per second, which is about 25mB per tick)""")
                .defineInRange("mb_per_second", 500, 4, Integer.MAX_VALUE);

        ITEM_THROUGHPUT = serverBuilder.comment("""
                        How many items per second can each connection transfer before upgrade?
                        Keep in mind that multiple tiny pipes can be connected to each block (up to 64), and pipes have unlimited throughput.
                        Full sized pipes can be upgrades up to 32x this base speed (by default).
                        Also, items move through pipes instantaneously.
                        For context, vanilla hoppers transfer 2.5 items per second.
                        (default: 2 items/second)""")
                .defineInRange("items_per_second", 2, 1, 64);

        SPEED_UPGRADE_MULTIPLIER = serverBuilder.comment("""
                        How much should each speed upgrade multiply the throughput?
                        Speed upgrade multipliers are cumulative. So, if a speed upgrade multiplies by 2, 2 upgrades multiply by 4,
                        3 multiply by 8, etc.
                        (default: 2.0 times)""")
                .defineInRange("speed_upgrade_multiplier", 2.0, 1.25, 16);

        SPEED_UPGRADE_MAX = serverBuilder.comment("""
                        How many speed upgrades can be applied to a single pipe?
                        (default: 5 )""")
                .defineInRange("speed_upgrade_max", 5, 1, 64);

        DEBUG_LOGGING = serverBuilder.comment("""
                        Add specific debugging information to the log file for troubleshooting.
                        No need to enable this unless asked to by a dev.
                        (default: false)""")
                .define("debug_logging", false);

        serverBuilder.pop();

        SERVER_CONFIG = serverBuilder.build();

    }




}
