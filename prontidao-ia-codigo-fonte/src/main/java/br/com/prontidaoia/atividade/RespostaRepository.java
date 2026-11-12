package br.com.prontidaoia.atividade;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RespostaRepository extends JpaRepository<Resposta, Long> {

    @Query("select r from Resposta r join fetch r.questao q where r.tentativa.id = :tentativaId order by q.ordem")
    List<Resposta> daTentativa(Long tentativaId);

    /** Acertos por tentativa, para mostrar "8 de 10" nas pendências. */
    @Query("select count(r) from Resposta r where r.tentativa.id = :tentativaId and r.correta = true")
    long acertosDaTentativa(Long tentativaId);

    /**
     * Acerto da própria pessoa por ciclo, só nas tentativas finalizadas
     * (mesma base do técnico da seção 5.1, mas de uma pessoa só).
     */
    @Query("""
            select new br.com.prontidaoia.atividade.AcertoNoCiclo(
                t.atividadeLiberada.ciclo.id,
                count(r),
                sum(case when r.correta = true then 1 else 0 end))
            from Resposta r join r.tentativa t
            where t.usuario.id = :usuarioId and t.finalizadaEm is not null
            group by t.atividadeLiberada.ciclo.id""")
    List<AcertoNoCiclo> acertoDaPessoaPorCiclo(Long usuarioId);
}
