package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.CotizacionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotizacionDetalleRepository extends JpaRepository<CotizacionDetalle, Long> {

    List<CotizacionDetalle> findByCotizacionId(Long cotizacionId);

    void deleteByCotizacionId(Long cotizacionId);
}
