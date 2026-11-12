package br.com.prontidaoia.atividade;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    @Query("select q from Questao q join fetch q.area where q.atividade.id = :atividadeId order by q.ordem")
    List<Questao> daAtividade(Long atividadeId);
}
