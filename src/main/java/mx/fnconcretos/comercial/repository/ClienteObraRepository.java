package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.ClienteObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteObraRepository extends JpaRepository<ClienteObra, Long> {
    List<ClienteObra> findByObraId(Long obraId);

    boolean existsByClienteIdAndObraId(Long clienteId, Long obraId);
}
