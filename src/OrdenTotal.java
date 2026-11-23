import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Une los 4 logs y los ordena por (lamport, id_proceso) -> logs/orden_total.log
// Uso: java -cp out OrdenTotal
public class OrdenTotal {

    public static void main(String[] args) throws Exception {
        String[] archivos = {"coordinador", "P1", "P2", "P3"};
        List<String> lineas = new ArrayList<>();

        for (String nombre : archivos) {
            Path ruta = Path.of("logs", nombre + ".log");
            if (Files.exists(ruta)) {
                for (String l : Files.readAllLines(ruta)) {
                    if (!l.isBlank()) lineas.add(l);
                }
            }
        }

        // Ordenar: primero por lamport; si empatan, por id de proceso
        lineas.sort((a, b) -> {
            String[] x = a.split(";");
            String[] y = b.split(";");
            int c = Long.compare(Long.parseLong(x[0]), Long.parseLong(y[0]));
            if (c != 0) return c;
            return Integer.compare(Integer.parseInt(x[1]), Integer.parseInt(y[1]));
        });

        try (FileWriter fw = new FileWriter(new File("logs/orden_total.log"))) {
            for (String l : lineas) fw.write(l + System.lineSeparator());
        }
        System.out.println("Generado logs/orden_total.log con " + lineas.size() + " eventos");
    }
}
