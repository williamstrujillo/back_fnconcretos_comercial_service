package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

    List<PedidoDetalle> findByPedidoId(Long pedidoId);
}
