import java.io.File;
import java.io.FileWriter;

// Escribe eventos en logs/<proceso>.log con el formato:
// lamport;id_proceso;evento;detalle
public class Bitacora {
    private final String archivo;
    private final int idProceso;

    public Bitacora(String nombreProceso, int idProceso) {
        new File("logs").mkdirs();
        this.archivo = "logs/" + nombreProceso + ".log";
        this.idProceso = idProceso;
    }

    public synchronized void escribir(long lamport, String evento, String detalle) {
        String linea = lamport + ";" + idProceso + ";" + evento + ";" + detalle;
        try (FileWriter fw = new FileWriter(archivo, true)) {
            fw.write(linea + System.lineSeparator());
        } catch (Exception e) {
            System.out.println("Error escribiendo log: " + e.getMessage());
        }
    }
}
