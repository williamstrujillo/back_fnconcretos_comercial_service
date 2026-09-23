package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BitacoraEventoRepository extends JpaRepository<BitacoraEvento, Long> {
    List<BitacoraEvento> findByEntidadTipoAndEntidadIdOrderByCreadoEnDesc(String entidadTipo, Long entidadId);
}
