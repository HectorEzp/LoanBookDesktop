package biblioteca;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Lee config.properties desde el directorio de trabajo. */
public class Config {
    private final Properties p = new Properties();

    public Config(String path) throws IOException {
        try (FileInputStream in = new FileInputStream(path)) {
            p.load(in);
        }
    }

    public String get(String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta la propiedad '" + key + "' en config.properties");
        }
        return v.trim();
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
