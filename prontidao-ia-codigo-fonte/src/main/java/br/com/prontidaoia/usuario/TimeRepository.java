package br.com.prontidaoia.usuario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<TimeEntity, Long> {

    Optional<TimeEntity> findByNome(String nome);

    List<TimeEntity> findAllByOrderByNome();
}
