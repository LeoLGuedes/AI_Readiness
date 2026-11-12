package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.atividade.Atividade;
import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.AtividadeRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.escopo.EscopoService;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.diagnostico.recomendacao.TipoIntervencao;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tela de escopo: atual × proposto e as intervenções humanas a agendar. */
@Service
public class EscopoQuery {

    private static final Pattern ATIVIDADE_A = Pattern.compile("^\\d+A$");

    private final TimeRepository times;
    private final AlertaRepository alertas;
    private final RecomendacaoRepository recomendacoes;
    private final AtividadeLiberadaRepository liberadas;
    private final AtividadeRepository atividades;
    private final CicloService ciclos;
    private final EscopoService escopo;

    public EscopoQuery(TimeRepository times, AlertaRepository alertas, RecomendacaoRepository recomendacoes,
                       AtividadeLiberadaRepository liberadas, AtividadeRepository atividades, CicloService ciclos,
                       EscopoService escopo) {
        this.times = times;
        this.alertas = alertas;
        this.recomendacoes = recomendacoes;
        this.liberadas = liberadas;
        this.atividades = atividades;
        this.ciclos = ciclos;
        this.escopo = escopo;
    }

    public record ItemHumano(Long recomendacaoId, String texto, String executor, LocalDate dataPrevista) {
    }

    public record TelaEscopo(Long timeId, String time, Long alertaId, SeloAlerta selo, Ciclo cicloAberto,
                             List<String> atual, List<String> proposto, String troca,
                             boolean valeNoCicloAberto, List<ItemHumano> humanas) {

        public boolean temAlerta() {
            return alertaId != null;
        }

        public boolean temTroca() {
            return troca != null;
        }

        /** O escopo ainda não foi salvo para este alerta. */
        public boolean podeSalvar() {
            return temAlerta() && cicloAberto != null && selo.status() == StatusAlerta.ABERTO;
        }
    }

    @Transactional(readOnly = true)
    public TelaEscopo doTime(Long timeId) {
        TimeEntity time = times.findById(timeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Time não encontrado."));
        Ciclo aberto = ciclos.cicloAberto().orElse(null);
        List<AtividadeLiberada> escopoAtual = aberto == null
                ? List.of() : liberadas.escopoDoTime(timeId, aberto.getId());
        List<String> atual = escopoAtual.stream().map(EscopoQuery::descricao).toList();

        // O alerta mais recente do time que tem recomendação aceita.
        Optional<Alerta> alerta = alertas.naoResolvidosDoTime(timeId).stream()
                .filter(a -> recomendacoes.doAlerta(a.getId()).stream().anyMatch(Recomendacao::foiAceita))
                .max(Comparator.comparing(Alerta::getId));
        if (alerta.isEmpty()) {
            return new TelaEscopo(timeId, time.getNome(), null, null, aberto, atual, atual, null, true, List.of());
        }

        List<Recomendacao> aceitas = recomendacoes.doAlerta(alerta.get().getId()).stream()
                .filter(Recomendacao::foiAceita).toList();

        List<String> proposto = new ArrayList<>(atual);
        String troca = null;
        Optional<Recomendacao> plataforma = aceitas.stream()
                .filter(r -> r.getIntervencao().getTipo() == TipoIntervencao.PLATAFORMA)
                .findFirst();
        if (plataforma.isPresent()) {
            String codigo = EscopoService.ATIVIDADE_DA_INTERVENCAO.get(plataforma.get().getIntervencao().getCodigo());
            Optional<Atividade> nova = codigo == null ? Optional.empty() : atividades.findByCodigo(codigo);
            for (int i = 0; i < escopoAtual.size() && nova.isPresent(); i++) {
                String atividadeAtual = escopoAtual.get(i).getAtividade().getCodigo();
                if (ATIVIDADE_A.matcher(atividadeAtual).matches()) {
                    proposto.set(i, nova.get().getCodigo() + " · " + nova.get().getTitulo());
                    troca = atual.get(i) + " → " + proposto.get(i);
                }
            }
        }

        boolean valeAgora = aberto != null && !escopo.timeJaIniciou(timeId, aberto.getId());
        List<ItemHumano> humanas = aceitas.stream()
                .filter(r -> r.getIntervencao().getTipo() == TipoIntervencao.HUMANA)
                .map(r -> new ItemHumano(r.getId(), r.textoVigente(), r.getIntervencao().getExecutor(),
                        r.getDataPrevista()))
                .toList();
        return new TelaEscopo(timeId, time.getNome(), alerta.get().getId(), SeloAlerta.de(alerta.get()), aberto,
                atual, proposto, troca, valeAgora, humanas);
    }

    private static String descricao(AtividadeLiberada l) {
        return l.getAtividade().getCodigo() + " · " + l.getAtividade().getTitulo();
    }
}
