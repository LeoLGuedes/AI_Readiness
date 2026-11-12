package br.com.prontidaoia.diagnostico.recomendacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecomendacaoRepository extends JpaRepository<Recomendacao, Long> {

    @Query("""
            select r from Recomendacao r join fetch r.intervencao left join fetch r.dificuldade
            where r.alerta.id = :alertaId order by r.id""")
    List<Recomendacao> doAlerta(Long alertaId);

    /** Número do primeiro ciclo em que alguma intervenção do alerta valeu. */
    @Query("select min(r.cicloAplicacao.numero) from Recomendacao r where r.alerta.id = :alertaId")
    Optional<Integer> primeiroCicloDeAplicacao(Long alertaId);

    /** Primeiro ciclo em que uma intervenção valeu para o time: o marcador do gráfico. */
    @Query("""
            select min(r.cicloAplicacao.numero) from Recomendacao r
            where r.alerta.time.id = :timeId""")
    Optional<Integer> primeiroCicloDeIntervencaoDoTime(Long timeId);
}
