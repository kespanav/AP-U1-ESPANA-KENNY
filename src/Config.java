import java.io.FileInputStream;
import java.util.Properties;

// Lee el archivo config.properties una sola vez.
public class Config {
    public static int aforo;
    public static String token;
    public static String host;
    public static int puerto;
    public static int latidoMs;
    public static int timeoutMs;

    public static void cargar() {
        try {
            Properties p = new Properties();
            p.load(new FileInputStream("config.properties"));
            aforo = Integer.parseInt(p.getProperty("aforo"));
            token = p.getProperty("token");
            host = p.getProperty("host");
            puerto = Integer.parseInt(p.getProperty("puerto"));
            latidoMs = Integer.parseInt(p.getProperty("latido_ms"));
            timeoutMs = Integer.parseInt(p.getProperty("timeout_ms"));
        } catch (Exception e) {
            System.out.println("No se pudo leer config.properties: " + e.getMessage());
            System.exit(1);
        }
    }
}
