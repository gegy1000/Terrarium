package net.gegy1000.terrarium.server.map.source;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.gegy1000.terrarium.Terrarium;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

public class TerrariumData {
    private final static String INFO_JSON = "https://gist.githubusercontent.com/gegy1000/0a0ac9ec610d6d9716d43820a0825a6d/raw/terrarium_info.json";

    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final File CACHE_ROOT = new File(".", "mods/terrarium/cache/");
    private static final File INFO_CACHE = new File(CACHE_ROOT, "terrarium_info.json");

    public static TerrariumData.Info info = new TerrariumData.Info("", "", "%s_%s.mat", "", "%s%s.hgt", "", "http://tile.openstreetmap.org", "%s/%s/%s.png");

    public static void loadInfo() {
        if (!CACHE_ROOT.exists()) {
            CACHE_ROOT.mkdirs();
        }

        try {
            URL url = new URL(INFO_JSON);
            info = TerrariumData.loadInfo(url.openStream());
            TerrariumData.cacheInfo(info);
        } catch (IOException e) {
            Terrarium.LOGGER.error("Failed to load remote Terrarium info, checking cache", e);
            TerrariumData.loadCachedInfo();
        }
    }

    private static void loadCachedInfo() {
        try (InputStream input = new FileInputStream(INFO_CACHE)) {
            info = loadInfo(input);
        } catch (IOException e) {
            Terrarium.LOGGER.error("Failed to load cached Terrarium info", e);
        }
    }

    private static TerrariumData.Info loadInfo(InputStream input) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(input))) {
            Info info = GSON.fromJson(reader, Info.class);
            return info.merge(TerrariumData.info);
        }
    }

    private static void cacheInfo(TerrariumData.Info info) {
        try (PrintWriter output = new PrintWriter(new FileOutputStream(INFO_CACHE))) {
            output.write(GSON.toJson(info));
        } catch (IOException e) {
            Terrarium.LOGGER.error("Failed to cache Terrarium info", e);
        }
    }

    public static class Info {
        @SerializedName("base_url")
        private String baseURL;
        @SerializedName("glob_endpoint")
        private String globEndpoint;
        @SerializedName("glob_query")
        private String globQuery;
        @SerializedName("heights_endpoint")
        private String heightsEndpoint;
        @SerializedName("heights_query")
        private String heightsQuery;
        @SerializedName("height_tiles")
        private String heightTiles;
        @SerializedName("raster_map_endpoint")
        private String rasterMapEndpoint;
        @SerializedName("raster_map_query")
        private String rasterMapQuery;

        public Info(String baseURL, String globEndpoint, String globQuery, String heightsEndpoint, String heightsQuery, String heightTiles, String rasterMapEndpoint, String rasterMapQuery) {
            this.baseURL = baseURL;
            this.globEndpoint = globEndpoint;
            this.globQuery = globQuery;
            this.heightsEndpoint = heightsEndpoint;
            this.heightsQuery = heightsQuery;
            this.heightTiles = heightTiles;
            this.rasterMapEndpoint = rasterMapEndpoint;
            this.rasterMapQuery = rasterMapQuery;
        }

        public String getBaseURL() {
            return this.baseURL;
        }

        public String getGlobEndpoint() {
            return this.globEndpoint;
        }

        public String getGlobQuery() {
            return this.globQuery;
        }

        public String getHeightsEndpoint() {
            return this.heightsEndpoint;
        }

        public String getHeightsQuery() {
            return this.heightsQuery;
        }

        public String getHeightTiles() {
            return this.heightTiles;
        }

        public String getRasterMapEndpoint() {
            return this.rasterMapEndpoint;
        }

        public String getRasterMapQuery() {
            return this.rasterMapQuery;
        }

        public Info merge(Info info) {
            if (this.baseURL == null) {
                this.baseURL = info.baseURL;
            }
            if (this.globEndpoint == null) {
                this.globEndpoint = info.globEndpoint;
            }
            if (this.globQuery == null) {
                this.globQuery = info.globQuery;
            }
            if (this.heightsEndpoint == null) {
                this.heightsEndpoint = info.heightsEndpoint;
            }
            if (this.heightsQuery == null) {
                this.heightsQuery = info.heightsQuery;
            }
            if (this.heightTiles == null) {
                this.heightTiles = info.heightTiles;
            }
            if (this.rasterMapEndpoint == null) {
                this.rasterMapEndpoint = info.rasterMapEndpoint;
            }
            if (this.rasterMapQuery == null) {
                this.rasterMapQuery = info.rasterMapQuery;
            }
            return this;
        }
    }
}
