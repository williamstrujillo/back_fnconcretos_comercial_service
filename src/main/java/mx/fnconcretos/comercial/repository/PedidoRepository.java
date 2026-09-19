package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    boolean existsByFolio(String folio);

    /** Candidatos a recordatorio de vencimiento de credito: a credito, con dias de credito
     * capturados, fecha programada capturada, no avisados todavia, y no rechazados (un pedido
     * rechazado no genera deuda). */
    List<Pedido> findByCondicionPagoAndDiasCreditoIsNotNullAndFechaProgramadaIsNotNullAndRecordatorioCreditoEnviadoFalseAndEstatusGeneralNot(
            String condicionPago, String estatusGeneralExcluido);
}
