package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.ContactoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactoClienteRepository extends JpaRepository<ContactoCliente, Long> {
    List<ContactoCliente> findByClienteId(Long clienteId);
}
