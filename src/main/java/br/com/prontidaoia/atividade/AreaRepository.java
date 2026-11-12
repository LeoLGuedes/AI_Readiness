package br.com.prontidaoia.atividade;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {

    Optional<Area> findByCodigo(String codigo);
}
