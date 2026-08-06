package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {
    boolean existsByFolio(String folio);
}
