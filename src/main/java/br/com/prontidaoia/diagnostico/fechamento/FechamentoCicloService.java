package br.com.prontidaoia.diagnostico.fechamento;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.Arredondamento;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.config.CiclosProperties;
import br.com.prontidaoia.config.LimiaresProperties;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.AlertaService;
import br.com.prontidaoia.diagnostico.alerta.DisparoDeRegra;
import br.com.prontidaoia.diagnostico.alerta.PontoDoCiclo;
import br.com.prontidaoia.diagnostico.alerta.PrioridadeAlerta;
import br.com.prontidaoia.diagnostico.alerta.RegraConfiancaSemVerificacao;
import br.com.prontidaoia.diagnostico.alerta.RegraDivergenciaTecnicoConfianca;
import br.com.prontidaoia.diagnostico.alerta.RegraEncerramentoConfianca;
import br.com.prontidaoia.diagnostico.alerta.RegraEncerramentoDivergencia;
import br.com.prontidaoia.diagnostico.alerta.RegraPrioridade;
import br.com.prontidaoia.diagnostico.alerta.SerieDoTime;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.alerta.TipoAlerta;
import br.com.prontidaoia.diagnostico.escopo.EscopoService;
import br.com.prontidaoia.diagnostico.indicador.AmostraMinima;
import br.com.prontidaoia.diagnostico.indicador.CalculadoraIndicadores;
import br.com.prontidaoia.diagnostico.indicador.CalculadoraIndicadores.Indicadores;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.diagnostico.recomendacao.GeradorRecomendacoes;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Fechamento do ciclo</b> (seção 5.5). Transacional: ou as seis etapas
 * acontecem, ou nada muda.
 *
 * <ol>
 *   <li>marca o ciclo como FECHADO; o que não foi concluído fica como não realizado;</li>
 *   <li>confere a amostra de check-ins de cada time;</li>
 *   <li>calcula e grava {@code indicador_time_ciclo} de cada time;</li>
 *   <li>avalia o encerramento dos alertas existentes e depois aplica as regras A e B;</li>
 *   <li>gera as recomendações dos alertas novos;</li>
 *   <li>se ainda houver ciclos, abre o próximo copiando o escopo vigente e
 *       aplicando as trocas de escopo que ficaram para ele.</li>
 * </ol>
 *
 * <p>É o <b>único</b> caminho que grava indicadores e alertas: nem o seed nem
 * a simulação os inserem direto no banco.</p>
 */
@Service
public class FechamentoCicloService {

    private static final Logger LOG = LoggerFactory.getLogger(FechamentoCicloService.class);

    private final CicloRepository ciclos;
    private final CicloService cicloService;
    private final TimeRepository times;
    private final UsuarioRepository usuarios;
    private final ColetaDoCiclo coleta;
    private final IndicadorRepository indicadores;
    private final AlertaRepository alertas;
    private final AlertaService alertaService;
    private final RecomendacaoRepository recomendacoes;
    private final GeradorRecomendacoes gerador;
    private final EscopoService escopo;
    private final CiclosProperties configCiclos;
    private final Clock clock;

    private final AmostraMinima amostra;
    private final CalculadoraIndicadores calculadora;
    private final RegraDivergenciaTecnicoConfianca regraA;
    private final RegraConfiancaSemVerificacao regraB;
    private final RegraPrioridade regraPrioridade;
    private final RegraEncerramentoDivergencia encerramentoA;
    private final RegraEncerramentoConfianca encerramentoB;

    public FechamentoCicloService(CicloRepository ciclos,
                                  CicloService cicloService,
                                  TimeRepository times,
                                  UsuarioRepository usuarios,
                                  ColetaDoCiclo coleta,
                                  IndicadorRepository indicadores,
                                  AlertaRepository alertas,
                                  AlertaService alertaService,
                                  RecomendacaoRepository recomendacoes,
                                  GeradorRecomendacoes gerador,
                                  EscopoService escopo,
                                  CiclosProperties configCiclos,
                                  LimiaresProperties limiares,
                                  Clock clock) {
        this.ciclos = ciclos;
        this.cicloService = cicloService;
        this.times = times;
        this.usuarios = usuarios;
        this.coleta = coleta;
        this.indicadores = indicadores;
        this.alertas = alertas;
        this.alertaService = alertaService;
        this.recomendacoes = recomendacoes;
        this.gerador = gerador;
        this.escopo = escopo;
        this.configCiclos = configCiclos;
        this.clock = clock;

        // As regras são código puro; os limiares vêm de application.yml.
        this.amostra = new AmostraMinima(limiares.amostraMinimaCheckins());
        this.calculadora = new CalculadoraIndicadores(amostra);
        this.regraA = new RegraDivergenciaTecnicoConfianca(limiares.quedaMinimaConfianca());
        this.regraB = new RegraConfiancaSemVerificacao(limiares.confiancaAlta(), limiares.acertoMinimoVerificacao());
        this.regraPrioridade = new RegraPrioridade(limiares.quedaEngajamentoPrioridadeAlta());
        this.encerramentoA = new RegraEncerramentoDivergencia(limiares.ciclosParaEncerramento());
        this.encerramentoB = new RegraEncerramentoConfianca(limiares.ciclosParaEncerramento(), regraB);
    }

    /** Fecha o ciclo aberto. */
    @Transactional
    public ResumoDoFechamento fecharCicloAberto() {
        Ciclo ciclo = cicloService.cicloAberto()
                .orElseThrow(() -> new RegraVioladaException("Não há ciclo aberto para fechar."));
        int c = ciclo.getNumero();

        // Etapa 1. Grava já, porque a etapa 6 abre outro ciclo e o índice único
        // só aceita um ciclo ABERTO por vez.
        ciclo.fechar();
        ciclos.saveAndFlush(ciclo);

        List<TimeEntity> todosOsTimes = times.findAllByOrderByNome();

        // Etapas 2 e 3.
        Map<TimeEntity, Indicadores> doCiclo = new LinkedHashMap<>();
        Instant agora = Instant.now(clock);
        for (TimeEntity time : todosOsTimes) {
            int membros = (int) usuarios.countByTimeIdAndAtivoTrue(time.getId());
            Indicadores valores = calculadora.calcular(coleta.doTime(time.getId(), ciclo.getId(), membros));
            indicadores.save(new IndicadorTimeCiclo(time, ciclo, valores, agora));
            doCiclo.put(time, valores);
        }
        indicadores.flush();

        // Etapa 4.
        List<String> novos = new ArrayList<>();
        List<String> atualizados = new ArrayList<>();
        List<String> resolvidos = new ArrayList<>();
        List<AlertaNovo> paraRecomendar = new ArrayList<>();

        for (TimeEntity time : todosOsTimes) {
            if (!amostra.suficiente(doCiclo.get(time).checkins())) {
                LOG.info("Ciclo {}, time {}: amostra insuficiente, regras não avaliadas.", c, time.getNome());
                continue;
            }
            SerieDoTime serie = serieDo(time);

            for (Alerta alerta : alertas.naoResolvidosDoTime(time.getId())) {
                if (deveEncerrar(alerta, serie, c)) {
                    alertaService.resolver(alerta, ciclo);
                    resolvidos.add(descricao(time, alerta.getTipo()));
                }
            }

            PrioridadeAlerta prioridade = regraPrioridade.prioridade(serie, c);
            for (Optional<DisparoDeRegra> disparo : List.of(regraA.avaliar(serie, c), regraB.avaliar(serie, c))) {
                disparo.ifPresent(d -> {
                    AlertaService.Registro registro = alertaService.registrar(time, ciclo, d, prioridade);
                    if (registro.novo()) {
                        novos.add(descricao(time, d.tipo()) + " (" + registro.alerta().getPrioridade() + ")");
                        paraRecomendar.add(new AlertaNovo(registro.alerta(), time));
                    } else {
                        atualizados.add(descricao(time, d.tipo()));
                    }
                });
            }
        }

        // Etapa 5.
        for (AlertaNovo novo : paraRecomendar) {
            if (novo.alerta().getTipo() == TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA) {
                gerador.paraAlertaA(novo.alerta(),
                        coleta.contagemDeDificuldades(novo.time().getId(), ciclo.getId()),
                        doCiclo.get(novo.time()).checkins());
            } else {
                gerador.paraAlertaB(novo.alerta());
            }
        }

        // Etapa 6.
        Integer aberto = null;
        if (c < configCiclos.total()) {
            Ciclo proximo = cicloService.abrirProximo(ciclo);
            // Trocas de escopo salvas quando o time já tinha começado o ciclo (seção 5.4).
            escopo.aplicarPendentes(proximo);
            aberto = proximo.getNumero();
        }

        ResumoDoFechamento resumo = new ResumoDoFechamento(c, aberto, novos, atualizados, resolvidos);
        LOG.info("Ciclo {} fechado: {}", c, resumo);
        return resumo;
    }

    private boolean deveEncerrar(Alerta alerta, SerieDoTime serie, int c) {
        if (alerta.getTipo() == TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA) {
            // Só se acompanha o efeito depois que a gestão aplicou a intervenção.
            return alerta.getStatus() == StatusAlerta.EM_ACOMPANHAMENTO
                    && recomendacoes.primeiroCicloDeAplicacao(alerta.getId())
                            .map(aplicacao -> encerramentoA.deveEncerrar(serie, c, aplicacao))
                            .orElse(false);
        }
        return encerramentoB.deveEncerrar(serie, c);
    }

    /** Monta a série de pontos do time a partir das fotografias gravadas. */
    private SerieDoTime serieDo(TimeEntity time) {
        Map<Integer, Double> verificacao = coleta.acertoEmVerificacaoPorCiclo(time.getId());
        return SerieDoTime.de(indicadores.serieDoTime(time.getId()).stream()
                .map(i -> {
                    int numero = i.getCiclo().getNumero();
                    Double acerto = verificacao.get(numero);
                    return new PontoDoCiclo(numero, i.getTecnico(), i.getConfianca(), i.getEngajamento(),
                            acerto == null ? null : Arredondamento.umaCasa(acerto));
                })
                .toList());
    }

    private static String descricao(TimeEntity time, TipoAlerta tipo) {
        return tipo.getNomeExibido() + " – " + time.getNome();
    }

    private record AlertaNovo(Alerta alerta, TimeEntity time) {
    }
}
