package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.VisitaObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VisitaObraRepository extends JpaRepository<VisitaObra, Long> {
    List<VisitaObra> findByAsesorIdAndFechaVisita(Long asesorId, LocalDate fechaVisita);
}
