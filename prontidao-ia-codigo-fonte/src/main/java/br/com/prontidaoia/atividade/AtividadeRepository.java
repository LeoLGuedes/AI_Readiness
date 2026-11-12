package br.com.prontidaoia.atividade;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    Optional<Atividade> findByCodigo(String codigo);

    List<Atividade> findByCodigoIn(Collection<String> codigos);
}
