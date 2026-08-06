package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    boolean existsByNumeroCliente(String numeroCliente);
}
