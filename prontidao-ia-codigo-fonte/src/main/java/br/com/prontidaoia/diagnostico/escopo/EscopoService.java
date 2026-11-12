package br.com.prontidaoia.diagnostico.escopo;

import br.com.prontidaoia.atividade.Atividade;
import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.AtividadeRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.StatusCiclo;
import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.diagnostico.recomendacao.TipoIntervencao;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Troca de escopo decidida pela gestão (seção 5.4).
 *
 * <p>Ao <b>salvar o escopo</b>:</p>
 * <ul>
 *   <li>cada intervenção de plataforma aceita troca a atividade A do time pela
 *       atividade da intervenção (a trilha de verificação);</li>
 *   <li>a troca vale <b>no ciclo aberto</b> se nenhum membro do time iniciou
 *       tentativa nele; senão, vale <b>a partir do próximo ciclo</b>;</li>
 *   <li>a nova atividade liberada guarda o {@code recomendacao_id} e a
 *       recomendação guarda o {@code ciclo_aplicacao_id};</li>
 *   <li>o alerta passa a EM_ACOMPANHAMENTO.</li>
 * </ul>
 * O nível da atividade não muda: a trilha tem o mesmo nível da atividade A.
 */
@Service
public class EscopoService {

    /** Atividade que cada intervenção de plataforma libera (decisão D12). */
    public static final Map<String, String> ATIVIDADE_DA_INTERVENCAO = Map.of(
            "TRILHA_VERIFICACAO", "TRILHA-VERIFICACAO",
            "TRILHA_VERIFICACAO_CHECKLIST", "TRILHA-VERIFICACAO");

    /** A atividade A do escopo padrão ("5A"), que a trilha substitui. */
    private static final Pattern ATIVIDADE_A = Pattern.compile("^\\d+A$");

    private final AlertaRepository alertas;
    private final RecomendacaoRepository recomendacoes;
    private final CicloRepository ciclos;
    private final AtividadeRepository atividades;
    private final AtividadeLiberadaRepository liberadas;
    private final EntityManager em;

    public EscopoService(AlertaRepository alertas, RecomendacaoRepository recomendacoes, CicloRepository ciclos,
                         AtividadeRepository atividades, AtividadeLiberadaRepository liberadas, EntityManager em) {
        this.alertas = alertas;
        this.recomendacoes = recomendacoes;
        this.ciclos = ciclos;
        this.atividades = atividades;
        this.liberadas = liberadas;
        this.em = em;
    }

    /** Resultado do salvamento: em que ciclo a troca vale. */
    public record EscopoSalvo(Alerta alerta, int cicloDeAplicacao, boolean noCicloAberto) {
    }

    /** Verdadeiro se algum membro do time já iniciou tentativa no ciclo. */
    @Transactional(readOnly = true)
    public boolean timeJaIniciou(Long timeId, Long cicloId) {
        return em.createQuery("""
                        select count(t) from Tentativa t
                        where t.atividadeLiberada.time.id = :time and t.atividadeLiberada.ciclo.id = :ciclo""",
                        Long.class)
                .setParameter("time", timeId).setParameter("ciclo", cicloId)
                .getSingleResult() > 0;
    }

    /** Salva o escopo com a troca sugerida, como ela vem selecionada na tela. */
    @Transactional
    public EscopoSalvo salvar(Long alertaId) {
        return salvar(alertaId, true);
    }

    /**
     * @param aplicarTroca falso quando a gestão desmarca a troca sugerida: as
     *                     intervenções ficam registradas e o alerta passa a ser
     *                     acompanhado, mas a atividade liberada não muda
     */
    @Transactional
    public EscopoSalvo salvar(Long alertaId, boolean aplicarTroca) {
        Alerta alerta = alertas.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));
        if (alerta.getStatus() != StatusAlerta.ABERTO) {
            throw new RegraVioladaException("O escopo deste alerta já foi salvo.");
        }
        List<Recomendacao> aceitas = recomendacoes.doAlerta(alertaId).stream()
                .filter(Recomendacao::foiAceita)
                .toList();
        if (aceitas.isEmpty()) {
            throw new RegraVioladaException("Aceite as recomendações antes de salvar o escopo.");
        }
        Ciclo aberto = ciclos.findByStatus(StatusCiclo.ABERTO)
                .orElseThrow(() -> new RegraVioladaException("Não há ciclo aberto para aplicar o escopo."));

        alerta.passarAAcompanhamento();
        Long timeId = alerta.getTime().getId();
        if (aplicarTroca && timeJaIniciou(timeId, aberto.getId())) {
            // Fica guardado: a abertura do próximo ciclo aplica (aplicarPendentes).
            return new EscopoSalvo(alerta, aberto.getNumero() + 1, false);
        }
        aplicar(alerta, aceitas, aberto, aplicarTroca);
        return new EscopoSalvo(alerta, aberto.getNumero(), true);
    }

    /**
     * Chamado pelo fechamento logo depois de abrir um ciclo: aplica as trocas
     * de escopo salvas quando o time já tinha começado o ciclo anterior.
     */
    @Transactional
    public void aplicarPendentes(Ciclo novoCiclo) {
        for (Alerta alerta : alertas.findByStatus(StatusAlerta.EM_ACOMPANHAMENTO)) {
            List<Recomendacao> pendentes = recomendacoes.doAlerta(alerta.getId()).stream()
                    .filter(Recomendacao::foiAceita)
                    .filter(r -> r.getCicloAplicacao() == null)
                    .toList();
            if (!pendentes.isEmpty()) {
                aplicar(alerta, pendentes, novoCiclo, true);
            }
        }
    }

    private void aplicar(Alerta alerta, List<Recomendacao> aceitas, Ciclo ciclo, boolean aplicarTroca) {
        Long timeId = alerta.getTime().getId();
        for (Recomendacao r : aceitas) {
            if (aplicarTroca && r.getIntervencao().getTipo() == TipoIntervencao.PLATAFORMA) {
                trocarAtividadeA(timeId, ciclo, r);
            }
            r.registrarAplicacao(ciclo);
        }
    }

    private void trocarAtividadeA(Long timeId, Ciclo ciclo, Recomendacao recomendacao) {
        String codigo = ATIVIDADE_DA_INTERVENCAO.get(recomendacao.getIntervencao().getCodigo());
        if (codigo == null) {
            return;
        }
        Atividade nova = atividades.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Atividade " + codigo + " não existe no catálogo."));

        List<AtividadeLiberada> escopo = liberadas.escopoDoTime(timeId, ciclo.getId());
        if (escopo.stream().anyMatch(l -> l.getAtividade().getId().equals(nova.getId()))) {
            return; // a trilha já está no escopo
        }
        Optional<AtividadeLiberada> atividadeA = escopo.stream()
                .filter(l -> ATIVIDADE_A.matcher(l.getAtividade().getCodigo()).matches())
                .findFirst();
        if (atividadeA.isEmpty()) {
            return;
        }
        AtividadeLiberada antiga = atividadeA.get();
        liberadas.delete(antiga);
        liberadas.flush();
        liberadas.save(new AtividadeLiberada(antiga.getTime(), ciclo, nova, recomendacao.getId()));
    }
}
