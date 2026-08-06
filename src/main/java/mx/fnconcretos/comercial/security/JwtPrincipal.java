package mx.fnconcretos.comercial.security;

import java.util.List;

/**
 * Principal construido a partir de los claims del JWT emitido por
 * auth-service. comercial-service nunca genera tokens, solo los valida.
 */
public record JwtPrincipal(
        Long usuarioId,
        String user,
        Long idEmpleado,
        String rol,
        List<String> permisos
) {
    public boolean tienePermiso(String codigo) {
        return permisos != null && permisos.contains(codigo);
    }
}
