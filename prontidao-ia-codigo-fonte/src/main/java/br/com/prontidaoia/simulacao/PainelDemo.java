package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leituras do painel {@code /demo}: o ciclo aberto e as pendências por time.
 * Mostra só contagens por time, nunca quem respondeu.
 */
@Component
public class PainelDemo {

    private final CicloService ciclos;
    private final TimeRepository times;
    private final UsuarioRepository usuarios;
    private final EntityManager em;

    public PainelDemo(CicloService ciclos, TimeRepository times, UsuarioRepository usuarios, EntityManager em) {
        this.ciclos = ciclos;
        this.times = times;
        this.usuarios = usuarios;
        this.em = em;
    }

    /** Pendências de um time no ciclo aberto. */
    public record PendenciasDoTime(String time, long checkins, long membros, long atividades, long esperadas) {
    }

    public record Situacao(Ciclo cicloAberto, List<PendenciasDoTime> times) {
    }

    @Transactional(readOnly = true)
    public Situacao situacao() {
        Ciclo aberto = ciclos.cicloAberto().orElse(null);
        if (aberto == null) {
            return new Situacao(null, List.of());
        }
        return new Situacao(aberto, times.findAllByOrderByNome().stream()
                .map(t -> pendencias(t, aberto))
                .toList());
    }

    private PendenciasDoTime pendencias(TimeEntity time, Ciclo ciclo) {
        long membros = usuarios.countByTimeIdAndAtivoTrue(time.getId());
        long checkins = em.createQuery("""
                        select count(c) from Checkin c
                        where c.usuario.time.id = :time and c.ciclo.id = :ciclo
                          and c.usuario.ativo = true""", Long.class)
                .setParameter("time", time.getId()).setParameter("ciclo", ciclo.getId())
                .getSingleResult();
        long atividades = em.createQuery("""
                        select count(t) from Tentativa t
                        where t.atividadeLiberada.time.id = :time and t.atividadeLiberada.ciclo.id = :ciclo
                          and t.finalizadaEm is not null and t.usuario.ativo = true""", Long.class)
                .setParameter("time", time.getId()).setParameter("ciclo", ciclo.getId())
                .getSingleResult();
        long liberadas = em.createQuery("""
                        select count(l) from AtividadeLiberada l
                        where l.time.id = :time and l.ciclo.id = :ciclo""", Long.class)
                .setParameter("time", time.getId()).setParameter("ciclo", ciclo.getId())
                .getSingleResult();
        return new PendenciasDoTime(time.getNome(), checkins, membros, atividades, membros * liberadas);
    }
}
