package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.ClienteObra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteObraRepository extends JpaRepository<ClienteObra, Long> {
    List<ClienteObra> findByObraId(Long obraId);

    boolean existsByClienteIdAndObraId(Long clienteId, Long obraId);

    Optional<ClienteObra> findByClienteIdAndObraId(Long clienteId, Long obraId);
}
