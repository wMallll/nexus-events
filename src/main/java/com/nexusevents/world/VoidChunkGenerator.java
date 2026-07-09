package com.nexusevents.world;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Generador de chunks completamente vacios.
 *
 * <p>Sobrescribe {@code generateChunkData} (presente desde 1.9); en las
 * versiones modernas, Bukkit detecta la sobrescritura y usa la via de
 * compatibilidad, por lo que el mismo generador produce mundos void en
 * todo el rango de versiones soportado sin NMS.</p>
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        return createChunkData(world);
    }
}
