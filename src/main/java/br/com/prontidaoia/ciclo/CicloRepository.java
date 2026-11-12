package br.com.prontidaoia.ciclo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    Optional<Ciclo> findByStatus(StatusCiclo status);

    Optional<Ciclo> findByNumero(Integer numero);

    List<Ciclo> findAllByOrderByNumero();

    List<Ciclo> findByStatusOrderByNumero(StatusCiclo status);

    Optional<Ciclo> findFirstByStatusOrderByNumeroDesc(StatusCiclo status);
}
