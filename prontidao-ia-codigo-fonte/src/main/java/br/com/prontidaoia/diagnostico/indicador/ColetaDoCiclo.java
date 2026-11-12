package br.com.prontidaoia.diagnostico.indicador;

import br.com.prontidaoia.atividade.Area;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Leituras que o fechamento faz de um time num ciclo.
 *
 * <p><b>Privacidade (seção 4):</b> as consultas de check-in daqui devolvem só
 * valores e contagens, nunca quem respondeu. Nenhuma delas é chamada por rota
 * do gestor; o gestor só vê o resultado agregado, gravado no indicador.</p>
 */
@Repository
public class ColetaDoCiclo {

    private final EntityManager em;

    public ColetaDoCiclo(EntityManager em) {
        this.em = em;
    }

    public DadosDoTimeNoCiclo doTime(Long timeId, Long cicloId, int membros) {
        List<Object[]> checkins = em.createQuery("""
                        select c.confianca, c.receioSubstituicao from Checkin c
                        where c.usuario.time.id = :time and c.ciclo.id = :ciclo
                          and c.usuario.ativo = true""", Object[].class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getResultList();

        List<Duration> duracoes = em.createQuery("""
                        select t.iniciadaEm, t.finalizadaEm from Tentativa t
                        where t.atividadeLiberada.time.id = :time and t.atividadeLiberada.ciclo.id = :ciclo
                          and t.finalizadaEm is not null and t.usuario.ativo = true""", Object[].class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getResultList().stream()
                .map(l -> Duration.between((Instant) l[0], (Instant) l[1]))
                .toList();

        Long liberadas = em.createQuery("""
                        select count(l) from AtividadeLiberada l
                        where l.time.id = :time and l.ciclo.id = :ciclo""", Long.class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getSingleResult();

        return new DadosDoTimeNoCiclo(
                respostas(timeId, cicloId),
                checkins.stream().map(l -> (Integer) l[0]).toList(),
                checkins.stream().map(l -> (Integer) l[1]).toList(),
                duracoes,
                membros,
                liberadas.intValue());
    }

    /** Respostas das tentativas finalizadas do time no ciclo, com a área. */
    public List<RespostaAvaliada> respostas(Long timeId, Long cicloId) {
        return em.createQuery("""
                        select new br.com.prontidaoia.diagnostico.indicador.RespostaAvaliada(q.area.codigo, r.correta)
                        from Resposta r join r.questao q join r.tentativa t
                        where t.atividadeLiberada.time.id = :time and t.atividadeLiberada.ciclo.id = :ciclo
                          and t.finalizadaEm is not null and t.usuario.ativo = true""", RespostaAvaliada.class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getResultList();
    }

    /**
     * % de acerto em VERIFICACAO do time em cada ciclo (chave: número do
     * ciclo). Usado pela regra B, que precisa do ciclo atual e do anterior.
     */
    public Map<Integer, Double> acertoEmVerificacaoPorCiclo(Long timeId) {
        List<Object[]> linhas = em.createQuery("""
                        select t.atividadeLiberada.ciclo.numero, count(r),
                               sum(case when r.correta = true then 1 else 0 end)
                        from Resposta r join r.questao q join r.tentativa t
                        where t.atividadeLiberada.time.id = :time and q.area.codigo = :area
                          and t.finalizadaEm is not null and t.usuario.ativo = true
                        group by t.atividadeLiberada.ciclo.numero""", Object[].class)
                .setParameter("time", timeId).setParameter("area", Area.VERIFICACAO)
                .getResultList();

        Map<Integer, Double> porCiclo = new HashMap<>();
        for (Object[] l : linhas) {
            long total = (Long) l[1];
            long acertos = ((Number) l[2]).longValue();
            porCiclo.put((Integer) l[0], acertos * 100.0 / total);
        }
        return porCiclo;
    }

    /**
     * Quantas vezes cada dificuldade foi marcada nos check-ins do time no
     * ciclo (chave: id da dificuldade). Só aparecem as marcadas ao menos uma vez.
     */
    public Map<Long, Long> contagemDeDificuldades(Long timeId, Long cicloId) {
        List<Object[]> linhas = em.createQuery("""
                        select d.id, count(c) from Checkin c join c.dificuldades d
                        where c.usuario.time.id = :time and c.ciclo.id = :ciclo
                          and c.usuario.ativo = true
                        group by d.id""", Object[].class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getResultList();

        Map<Long, Long> contagem = new HashMap<>();
        for (Object[] l : linhas) {
            contagem.put((Long) l[0], (Long) l[1]);
        }
        return contagem;
    }
}
