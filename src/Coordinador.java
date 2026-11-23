import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Coordinador {

    static int dentro = 0;                       // personas dentro ahora
    static boolean inseguro = false;
    static final Object candado = new Object();  // candado para el contador


    static String[] estadoPuerta = {"", "CAIDA", "CAIDA", "CAIDA"};
    static long[] ultimoLatido = new long[4];

    static Lamport reloj = new Lamport();
    static Bitacora log = new Bitacora("coordinador", 0);

    public static void main(String[] args) throws Exception {
        Config.cargar();
        if (args.length > 0 && args[0].equals("-inseguro")) {
            inseguro = true;
        }
        ServerSocket servidor = new ServerSocket(Config.puerto);
        System.out.println("Coordinador escuchando en puerto " + Config.puerto
                + " | aforo=" + Config.aforo + (inseguro ? " | MODO INSEGURO" : " | modo normal"));
        log.escribir(reloj.tick(), "INICIO", inseguro ? "modo inseguro" : "modo normal");


        new Thread(Coordinador::vigilarLatidos).start();


        while (true) {
            Socket cliente = servidor.accept();
            new Thread(() -> atender(cliente)).start();
        }
    }


    static void atender(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            String linea;
            while ((linea = in.readLine()) != null) {
                out.println(procesar(linea));
            }
        } catch (Exception e) {
            // la puerta cerro la conexion: no pasa nada
        }
    }


    static String procesar(String linea) {
        String[] p = linea.split("\\|");
        String tipo = p[0];
        String token = p.length > 1 ? p[1] : "";
        int puerta = p.length > 2 ? Integer.parseInt(p[2]) : -1;
        long lamportRecibido = p.length > 3 ? Long.parseLong(p[3]) : 0;


        long t = reloj.recibir(lamportRecibido);


        if (!token.equals(Config.token)) {
            log.escribir(t, "TOKEN_INVALIDO", "puerta=" + puerta + " tipo=" + tipo + " token=" + token);
            System.out.println("[!] Token invalido desde puerta " + puerta);
            return respuesta("RECHAZADA", "TOKEN_INVALIDO");
        }


        if (tipo.equals("LATIDO")) {
            recibirLatido(puerta, t);
            return respuesta("OK", "-");
        }

        log.escribir(t, "RECIBE_" + tipo, "puerta=" + puerta);

        switch (tipo) {
            case "ENTRAR":
                boolean ok = inseguro ? entrarInseguro() : entrarSeguro();
                return ok ? respuesta("PERMITIDA", "-") : respuesta("RECHAZADA", "AFORO_COMPLETO");
            case "SALIR":
                return salir() ? respuesta("PERMITIDA", "-") : respuesta("RECHAZADA", "NADIE_DENTRO");
            case "ESTADO":
                return respuesta("PERMITIDA", "-");
            default:
                return respuesta("RECHAZADA", "TIPO_DESCONOCIDO");
        }
    }


    static boolean entrarSeguro() {
        synchronized (candado) {
            if (dentro < Config.aforo) {
                dentro++;
                return true;
            }
            return false;
        }
    }


    static boolean entrarInseguro() {
        int leido = dentro;                 // consulta
        if (leido >= Config.aforo) return false;
        try { Thread.sleep(50); } catch (InterruptedException e) { }
        dentro = dentro + 1;                // actualizacion (sin candado)
        return true;
    }

    static boolean salir() {
        synchronized (candado) {
            if (dentro > 0) {
                dentro--;
                return true;
            }
            return false;
        }
    }

    static void recibirLatido(int puerta, long t) {
        if (puerta < 1 || puerta > 3) return;
        synchronized (estadoPuerta) {
            ultimoLatido[puerta] = System.currentTimeMillis();
            if (!estadoPuerta[puerta].equals("ACTIVA")) {
                estadoPuerta[puerta] = "ACTIVA";
                log.escribir(t, "PUERTA_ACTIVA", "P" + puerta);
                System.out.println("Puerta P" + puerta + " ACTIVA");
            }
        }
    }


    static void vigilarLatidos() {
        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            long ahora = System.currentTimeMillis();
            synchronized (estadoPuerta) {
                for (int i = 1; i <= 3; i++) {
                    if (estadoPuerta[i].equals("ACTIVA") && ahora - ultimoLatido[i] > Config.timeoutMs) {
                        estadoPuerta[i] = "CAIDA";
                        log.escribir(reloj.tick(), "PUERTA_CAIDA", "P" + i + " sin latidos por " + (ahora - ultimoLatido[i]) + " ms");
                        System.out.println("Puerta P" + i + " CAIDA");
                    }
                }
            }
        }
    }


    static String respuesta(String resultado, String motivo) {
        String puertas;
        synchronized (estadoPuerta) {
            puertas = "P1=" + estadoPuerta[1] + ",P2=" + estadoPuerta[2] + ",P3=" + estadoPuerta[3];
        }
        long t = reloj.tick(); // regla de Lamport antes de enviar
        if (!resultado.equals("OK")) { // no se registran las respuestas a latidos (serian demasiadas)
            log.escribir(t, "ENVIA_RESPUESTA", resultado + " motivo=" + motivo + " dentro=" + dentro);
        }
        return "RESPUESTA|" + resultado + "|" + motivo + "|" + dentro + "|" + Config.aforo + "|" + t + "|" + puertas;
    }
}
