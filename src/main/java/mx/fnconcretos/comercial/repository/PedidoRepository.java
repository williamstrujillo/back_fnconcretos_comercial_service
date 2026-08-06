package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    boolean existsByFolio(String folio);
}
