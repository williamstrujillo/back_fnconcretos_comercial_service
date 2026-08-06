package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoAutorizacionRepository extends JpaRepository<PedidoAutorizacion, Long> {
    List<PedidoAutorizacion> findByPedidoId(Long pedidoId);

    Optional<PedidoAutorizacion> findByPedidoIdAndTipo(Long pedidoId, String tipo);

    List<PedidoAutorizacion> findByResultadoAndTipo(String resultado, String tipo);
}
