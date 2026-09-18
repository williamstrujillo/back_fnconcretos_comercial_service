package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.AsesorComercial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AsesorComercialRepository extends JpaRepository<AsesorComercial, Long> {

    Optional<AsesorComercial> findByUsuarioId(Long usuarioId);
}
