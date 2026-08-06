package mx.fnconcretos.comercial.repository;

import mx.fnconcretos.comercial.entity.Obra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ObraRepository extends JpaRepository<Obra, Long>, JpaSpecificationExecutor<Obra> {
}
