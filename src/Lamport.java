// Reloj logico de Lamport.
// synchronized: varios hilos del mismo proceso lo usan a la vez.
public class Lamport {
    private long valor = 0;

    // Antes de un evento local o de un ENVIO: sumar 1
    public synchronized long tick() {
        valor = valor + 1;
        return valor;
    }

    // Al RECIBIR un mensaje: max(local, recibido) + 1
    public synchronized long recibir(long recibido) {
        valor = Math.max(valor, recibido) + 1;
        return valor;
    }
}
