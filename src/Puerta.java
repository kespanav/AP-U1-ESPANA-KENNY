import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Ordenes: entrar | salir | estado | falso (entrar con token incorrecto) | fin
public class Puerta {

    static int id;
    static Lamport reloj = new Lamport();
    static Bitacora log;

    public static void main(String[] args) {
        Config.cargar();
        id = Integer.parseInt(args[0]);
        log = new Bitacora("P" + id, id);
        String[] nombres = {"", "Norte", "Sur", "Este"};
        System.out.println("Puerta P" + id + " " + nombres[id] + " lista. Ordenes: entrar, salir, estado, falso, fin");
        log.escribir(reloj.tick(), "INICIO", "puerta " + nombres[id]);

        // Hilo de latidos: cada 2 s avisa "estoy viva"
        Thread latidos = new Thread(Puerta::enviarLatidos);
        latidos.setDaemon(true);
        latidos.start();

        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String orden = sc.nextLine().trim().toLowerCase();
            switch (orden) {
                case "entrar" -> pedir("ENTRAR", Config.token);
                case "salir"  -> pedir("SALIR", Config.token);
                case "estado" -> pedir("ESTADO", Config.token);
                case "falso"  -> pedir("ENTRAR", "TOKEN_FALSO");
                case "fin"    -> System.exit(0);
                default -> System.out.println("Orden no valida");
            }
        }
    }


    static void pedir(String tipo, String token) {
        long t = reloj.tick(); // Lamport: antes de enviar
        log.escribir(t, "ENVIA_" + tipo, "token=" + (token.equals(Config.token) ? "ok" : token));
        String resp = enviar(tipo + "|" + token + "|" + id + "|" + t);

        if (resp == null) {
            // E5: el coordinador no responde -> NO dejamos entrar a nadie.
            // Preferimos no superar el aforo (consistencia) antes que seguir operando.
            log.escribir(reloj.tick(), "COORDINADOR_NO_DISPONIBLE", tipo + " no realizada");
            System.out.println(">> Coordinador NO disponible. " + tipo
                    + " NO realizada: por seguridad no se deja pasar a nadie.");
            return;
        }


        String[] r = resp.split("\\|");
        long tr = reloj.recibir(Long.parseLong(r[5])); // Lamport: al recibir
        log.escribir(tr, "RECIBE_RESPUESTA", r[1] + " motivo=" + r[2] + " dentro=" + r[3]);

        if (tipo.equals("ESTADO")) {
            System.out.println(">> Personas dentro: " + r[3] + " / aforo " + r[4] + " | " + r[6]);
        } else {
            System.out.println(">> " + tipo + ": " + r[1]
                    + (r[2].equals("-") ? "" : " (motivo: " + r[2] + ")")
                    + " | dentro=" + r[3] + "/" + r[4]);
        }
    }


    static String enviar(String mensaje) {
        try (Socket s = new Socket(Config.host, Config.puerto);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            s.setSoTimeout(3000); // si no responde en 3 s, se considera caido
            out.println(mensaje);
            return in.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    static void enviarLatidos() {
        while (true) {
            long t = reloj.tick(); // el latido tambien lleva reloj de Lamport
            String resp = enviar("LATIDO|" + Config.token + "|" + id + "|" + t);
            if (resp != null) {
                String[] r = resp.split("\\|");
                reloj.recibir(Long.parseLong(r[5]));
            }
            try { Thread.sleep(Config.latidoMs); } catch (InterruptedException e) { }
        }
    }
}
