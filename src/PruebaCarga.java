import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class PruebaCarga {

    public static void main(String[] args) throws Exception {
        Config.cargar();
        AtomicInteger permitidas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        Lamport reloj = new Lamport();

        CountDownLatch salida = new CountDownLatch(1); // "pistola de salida"
        Thread[] hilos = new Thread[30];

        for (int i = 0; i < 30; i++) {
            int puerta = (i % 3) + 1; // 1,2,3,1,2,3... -> 10 hilos por puerta
            hilos[i] = new Thread(() -> {
                try {
                    salida.await(); // todos esperan aqui y arrancan juntos
                    String r = enviar("ENTRAR|" + Config.token + "|" + puerta + "|" + reloj.tick());
                    if (r != null && r.split("\\|")[1].equals("PERMITIDA")) permitidas.incrementAndGet();
                    else rechazadas.incrementAndGet();
                } catch (Exception e) {
                    rechazadas.incrementAndGet();
                }
            });
            hilos[i].start();
        }

        salida.countDown(); // ¡ya!
        for (Thread h : hilos) h.join();


        String estado = enviar("ESTADO|" + Config.token + "|0|" + reloj.tick());
        String dentro = estado == null ? "?" : estado.split("\\|")[3];

        System.out.println("===== RESULTADO PruebaCarga =====");
        System.out.println("Solicitudes enviadas : 30");
        System.out.println("Permitidas           : " + permitidas.get());
        System.out.println("Rechazadas           : " + rechazadas.get());
        System.out.println("Dentro (coordinador) : " + dentro + " (aforo " + Config.aforo + ")");
    }

    static String enviar(String mensaje) {
        try (Socket s = new Socket(Config.host, Config.puerto);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            out.println(mensaje);
            return in.readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
