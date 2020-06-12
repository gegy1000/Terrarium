package net.gegy1000.earth.server.world.biome;

import net.gegy1000.earth.server.world.Rainfall;
import net.gegy1000.earth.server.world.Temperature;
import net.gegy1000.earth.server.world.cover.Cover;
import net.gegy1000.earth.server.world.cover.CoverMarkers;
import net.gegy1000.earth.server.world.ecology.GrowthPredictors;
import net.gegy1000.earth.server.world.ecology.soil.SoilSuborder;
import net.gegy1000.earth.server.world.geography.Landform;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

public final class BiomeClassifier {
    public static Biome classify(Context context) {
        if (context.isLand()) {
            return classifyLand(context);
        } else {
            return classifyWater(context);
        }
    }

    private static Biome classifyLand(Context context) {
        if (context.landform == Landform.BEACH) {
            return context.isFrozen() ? Biomes.COLD_BEACH : Biomes.BEACH;
        }

        if (context.isFrozen()) {
            return context.isForested() ? Biomes.COLD_TAIGA : Biomes.ICE_PLAINS;
        }
        if (context.isCold()) {
            return Biomes.TAIGA;
        }

        if (context.isWet() || context.isFlooded()) {
            if (context.cover == Cover.SALINE_FLOODED_FOREST) {
                return Biomes.SWAMPLAND;
            } else {
                return context.isForested() ? Biomes.JUNGLE : Biomes.JUNGLE_EDGE;
            }
        }

        if (context.isDry()) {
            // TODO: choose desert based on soil?
            return context.isBarren() ? Biomes.DESERT : Biomes.SAVANNA;
        }

        return context.isForested() ? Biomes.FOREST : Biomes.PLAINS;
    }

    private static Biome classifyWater(Context context) {
        if (context.isSea()) {
            return context.predictors.meanTemperature < -10 ? Biomes.FROZEN_OCEAN : Biomes.OCEAN;
        }

        return context.isFrozen() ? Biomes.FROZEN_RIVER : Biomes.RIVER;
    }

    public static class Context {
        public final GrowthPredictors predictors = new GrowthPredictors();
        public Landform landform;
        public SoilSuborder soilSuborder;
        public Cover cover;
        public int slope;

        public boolean isSea() {
            return this.landform == Landform.SEA;
        }

        public boolean isRiverOrLake() {
            return this.landform == Landform.LAKE_OR_RIVER;
        }

        public boolean isLand() {
            return this.landform.isLand();
        }

        public boolean isFrozen() {
            return Temperature.isFrozen(this.predictors.minTemperature, this.predictors.meanTemperature) || this.cover.is(CoverMarkers.FROZEN);
        }

        public boolean isCold() {
            return Temperature.isCold(this.predictors.meanTemperature) || this.isFrozen();
        }

        public boolean isForested() {
            return this.cover.is(CoverMarkers.FOREST);
        }

        public boolean isFlooded() {
            return this.cover.is(CoverMarkers.FLOODED);
        }

        public boolean isBarren() {
            return this.cover.is(CoverMarkers.BARREN);
        }

        public boolean isWet() {
            return Rainfall.isWet(this.predictors.annualRainfall);
        }

        public boolean isDry() {
            return Rainfall.isDry(this.predictors.annualRainfall);
        }
    }
}
