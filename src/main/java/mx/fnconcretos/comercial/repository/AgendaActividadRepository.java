package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.AgendaActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendaActividadRepository extends JpaRepository<AgendaActividad, Long> {

    List<AgendaActividad> findByFechaHoraBetweenOrderByFechaHora(LocalDateTime desde, LocalDateTime hasta);

    List<AgendaActividad> findByClienteIdOrderByFechaHoraDesc(Long clienteId);

    List<AgendaActividad> findByEstatusNotInAndFechaHoraLessThanOrderByFechaHora(List<String> estatusCerrados, LocalDateTime ahora);
}
