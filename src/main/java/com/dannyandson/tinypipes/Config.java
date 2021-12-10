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

    static {
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();

        serverBuilder.comment("Balance Settings").push(CATEGORY_BALANCE);

        ENERGY_THROUGHPUT = serverBuilder.comment(
                        "How much forge energy per tick should each connection allow?" +
                        "\nKeep in mind that multiple pipes can be connected to each block (up to 64), and pipes have unlimited throughput." +
                        "\n(default: 128 fe/tick)")
                .defineInRange("fe_per_tick", 256, 1, Integer.MAX_VALUE);

        FLUID_THROUGHPUT = serverBuilder.comment(
                        "How many millibuckets of fluid per second should each connection allow? (1000mB = 1 bucket)" +
                        "\nKeep in mind that multiple pipes can be connected to each block (up to 64), and pipes have unlimited throughput." +
                        "\n(default: 500mB per second, which is about 25mB per tick)")
                .defineInRange("mb_per_second", 500, 4, Integer.MAX_VALUE);

        ITEM_THROUGHPUT = serverBuilder.comment(
                        "How many items per second can each connection transfer?" +
                        "\nKeep in mind that multiple pipes can be connected to each block (up to 64), and pipes have unlimited throughput." +
                        "\nAlso, items move through pipes instantaneously." +
                        "\nFor context, vanilla hoppers transfer 2.5 items per second." +
                        "\n(default: 2 items/second)")
                .defineInRange("items_per_second", 2, 1, 64);

        serverBuilder.pop();

        SERVER_CONFIG = serverBuilder.build();

    }
}
