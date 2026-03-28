package com.dae.creep;

import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CreepSpawnCounterData extends SavedData {
    private static final String DATA_ID = "dae_creep_spawn_counter";
    private static final Codec<CreepSpawnCounterData> CODEC = Codec.INT.xmap(CreepSpawnCounterData::new, CreepSpawnCounterData::getCount);
    private static final SavedDataType<CreepSpawnCounterData> TYPE =
        new SavedDataType<>(DATA_ID, CreepSpawnCounterData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private int count;

    public CreepSpawnCounterData() {
        this(0);
    }

    private CreepSpawnCounterData(int count) {
        this.count = Math.max(0, count);
    }

    public static CreepSpawnCounterData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public int incrementAndGet() {
        this.count++;
        this.setDirty();
        return this.count;
    }

    public int getCount() {
        return this.count;
    }
}
