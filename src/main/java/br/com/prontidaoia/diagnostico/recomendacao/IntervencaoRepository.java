package br.com.prontidaoia.diagnostico.recomendacao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntervencaoRepository extends JpaRepository<Intervencao, Long> {

    Optional<Intervencao> findByCodigo(String codigo);
}
