package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.CotizacionCompartidaToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CotizacionCompartidaTokenRepository extends JpaRepository<CotizacionCompartidaToken, Long> {
    Optional<CotizacionCompartidaToken> findByCotizacionId(Long cotizacionId);

    Optional<CotizacionCompartidaToken> findByToken(String token);

    boolean existsByToken(String token);
}
