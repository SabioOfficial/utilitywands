package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final ComponentType<Boolean> REPEL_MODE = register("magnet_repel_mode", builder -> builder.codec(Codec.BOOL).packetCodec(PacketCodecs.BOOL));

    private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Wandsofcombat.MOD_ID, name), (builderOperator.apply(ComponentType.builder())).build());
    }

    public static void initialize() {

    }
}