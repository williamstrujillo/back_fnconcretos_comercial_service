package mx.fnconcretos.comercial.exception;

/** Se lanza cuando una accion no es valida para el estatus actual del recurso (ej. convertir una cotizacion que no esta "Listo"). */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String message) {
        super(message);
    }
}
