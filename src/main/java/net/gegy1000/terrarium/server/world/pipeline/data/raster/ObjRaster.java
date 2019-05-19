package net.gegy1000.terrarium.server.world.pipeline.data.raster;

import net.gegy1000.terrarium.server.world.pipeline.data.ColumnDataCache;
import net.gegy1000.terrarium.server.world.pipeline.data.DataKey;
import net.gegy1000.terrarium.server.world.pipeline.data.DataView;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;
import java.util.Optional;

public final class ObjRaster<T> extends AbstractRaster<T[]> {
    private ObjRaster(T[] data, int width, int height) {
        super(data, width, height);
    }

    @SuppressWarnings("unchecked")
    public static <T> ObjRaster<T> create(T value, int width, int height) {
        Object[] array = new Object[width * height];
        Arrays.fill(array, value);
        return new ObjRaster<>((T[]) array, width, height);
    }

    public static <T> ObjRaster<T> createSquare(T value, int size) {
        return create(value, size, size);
    }

    public static <T> ObjRaster<T> create(T value, DataView view) {
        return create(value, view.getWidth(), view.getHeight());
    }

    public static <T> Sampler<T> sampler(DataKey<ObjRaster<T>> key) {
        return new Sampler<>(key);
    }

    public void set(int x, int y, T value) {
        this.data[this.index(x, y)] = value;
    }

    public T get(int x, int y) {
        return this.data[this.index(x, y)];
    }

    public void transform(Transformer<T> transformer) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int index = this.index(x, y);
                this.data[index] = transformer.apply(this.data[index], x, y);
            }
        }
    }

    public void iterate(Iterator<T> iterator) {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                iterator.accept(this.data[this.index(x, y)], x, y);
            }
        }
    }

    @Override
    public ObjRaster<T> copy() {
        return new ObjRaster<>(Arrays.copyOf(this.data, this.data.length), this.width, this.height);
    }

    public interface Transformer<T> {
        T apply(T source, int x, int y);
    }

    public interface Iterator<T> {
        void accept(T value, int x, int y);
    }

    public static class Sampler<T> {
        private final DataKey<ObjRaster<T>> key;
        private T defaultValue;

        Sampler(DataKey<ObjRaster<T>> key) {
            this.key = key;
        }

        public Sampler setDefaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public T sample(ColumnDataCache dataCache, int x, int z) {
            ChunkPos columnPos = new ChunkPos(x >> 4, z >> 4);
            Optional<ObjRaster<T>> optional = dataCache.joinData(columnPos, this.key);
            if (optional.isPresent()) {
                ObjRaster<T> raster = optional.get();
                return raster.get(x & 0xF, z & 0xF);
            }
            return this.defaultValue;
        }

        public ObjRaster<T> sample(ColumnDataCache dataCache, DataView view) {
            ObjRaster<T> raster = ObjRaster.create(this.defaultValue, view);
            AbstractRaster.sampleInto(raster, dataCache, view, this.key);
            return raster;
        }
    }
}