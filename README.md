# AP-U1 · AforoSeguro (control de aforo distribuido)

Java 21 + sockets TCP (sin librerías externas). Un coordinador (id 0) y tres puertas: P1 Norte, P2 Sur, P3 Este.

## Estructura

```
src/                Coordinador, Puerta, PruebaCarga, OrdenTotal (+ Config, Lamport, Bitacora)
config.properties   aforo, token, host, puerto, tiempos de latido
logs/               coordinador.log, P1.log, P2.log, P3.log, orden_total.log
docs/informe.tex    informe (y informe.pdf)
docs/entrega_SGA.tex  página única para el SGA (y su PDF)
docs/img/           capturas E0 a E6
```

## 1. Compilar

Desde la carpeta raíz del proyecto (donde está `config.properties`):

```powershell
javac -encoding UTF-8 -d out src\*.java
```

## 2. Ejecutar (orden de arranque)

Abrir una consola por proceso, **siempre desde la carpeta raíz**:

| Orden | Proceso | Comando |
|---|---|---|
| 1 | Coordinador (normal) | `java -cp out Coordinador` |
| 1b | Coordinador (inseguro, solo E3) | `java -cp out Coordinador -inseguro` |
| 2 | Puerta P1 Norte | `java -cp out Puerta 1` |
| 3 | Puerta P2 Sur | `java -cp out Puerta 2` |
| 4 | Puerta P3 Este | `java -cp out Puerta 3` |
| — | Prueba de carga (coordinador recién iniciado) | `java -cp out PruebaCarga` |
| — | Orden total (al final) | `java -cp out OrdenTotal` |

Órdenes en la consola de una puerta: `entrar`, `salir`, `estado`, `falso` (envía ENTRAR con token incorrecto, para E2) y `fin`.

Para empezar de cero borre la carpeta `logs/` antes de arrancar.

## 3. Formato de los mensajes

Cada mensaje es **una línea de texto terminada en `\n`** (delimitación por separador de línea), con campos separados por `|`.

Solicitud (puerta → coordinador):

```
TIPO|token|puerta|lamport
ENTRAR|UTEQ2026|2|17
```

`TIPO` puede ser `ENTRAR`, `SALIR`, `ESTADO` o `LATIDO`.

Respuesta (coordinador → puerta):

```
RESPUESTA|resultado|motivo|dentro|aforo|lamport|puertas
RESPUESTA|PERMITIDA|-|8|10|19|P1=ACTIVA,P2=ACTIVA,P3=CAIDA
RESPUESTA|RECHAZADA|AFORO_COMPLETO|10|10|25|P1=ACTIVA,P2=ACTIVA,P3=ACTIVA
```

Motivos de rechazo: `AFORO_COMPLETO`, `TOKEN_INVALIDO`, `NADIE_DENTRO`.

Formato de los logs: `lamport;id_proceso;evento;detalle`.

## 4. Compilar el informe

Compilador: `pdflatex`. Archivo principal: `docs/informe.tex`. Paquetes usados: `inputenc`, `babel`, `geometry`, `graphicx`, `url` (vienen en cualquier MiKTeX/TeX Live).

```powershell
cd docs
pdflatex informe.tex
pdflatex informe.tex
pdflatex entrega_SGA.tex
```

(Se compila dos veces para que salgan bien las numeraciones de figuras/tablas.)

## 5. Código reutilizado

| Archivo de este repo | Repositorio propio de origen (URL) | Archivo y commit de origen | Que se cambio |
|---|---|---|---|

No se reutilizó código.
