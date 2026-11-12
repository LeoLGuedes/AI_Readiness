package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.atividade.Area;
import br.com.prontidaoia.atividade.AreaRepository;
import br.com.prontidaoia.comum.Arredondamento;
import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.config.CiclosProperties;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.DetalheAlerta;
import br.com.prontidaoia.diagnostico.alerta.PrioridadeAlerta;
import br.com.prontidaoia.diagnostico.indicador.CalculoErroPorArea;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.gestao.PrivacidadeGuard;
import br.com.prontidaoia.gestao.PrivacidadeGuard.ContagemDoTime;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quadro 2 do relatório: o detalhe de um time.
 *
 * <p>Percepção só chega aqui agregada: os indicadores gravados e, pelo
 * {@link PrivacidadeGuard}, a contagem de dificuldades com amostra mínima.</p>
 */
@Service
public class DetalheTimeQuery {

    private final TimeRepository times;
    private final UsuarioRepository usuarios;
    private final IndicadorRepository indicadores;
    private final AlertaRepository alertas;
    private final RecomendacaoRepository recomendacoes;
    private final ColetaDoCiclo coleta;
    private final AreaRepository areas;
    private final PrivacidadeGuard privacidade;
    private final CiclosProperties configCiclos;

    public DetalheTimeQuery(TimeRepository times, UsuarioRepository usuarios, IndicadorRepository indicadores,
                            AlertaRepository alertas, RecomendacaoRepository recomendacoes, ColetaDoCiclo coleta,
                            AreaRepository areas, PrivacidadeGuard privacidade, CiclosProperties configCiclos) {
        this.times = times;
        this.usuarios = usuarios;
        this.indicadores = indicadores;
        this.alertas = alertas;
        this.recomendacoes = recomendacoes;
        this.coleta = coleta;
        this.areas = areas;
        this.privacidade = privacidade;
        this.configCiclos = configCiclos;
    }

    /** A caixa do alerta, com a regra explicada pelos valores gravados no fechamento. */
    public record CaixaAlerta(SeloAlerta selo, int cicloGerado, Integer cicloResolucao, String frase,
                              Integer quedaEngajamento) {
    }

    /** Séries do gráfico de linhas: um valor por ciclo, nulo onde não há dado. */
    public record Series(List<String> rotulos, List<Integer> tecnico, List<Integer> confianca,
                         List<Integer> resistencia, List<Integer> engajamento, Integer cicloIntervencao) {
    }

    public record Barra(String rotulo, int valor, String legenda) {
    }

    public record Produtividade(BigDecimal primeiro, BigDecimal atual, BigDecimal diferenca, int tecnicoAtual,
                                int cicloAtual) {
    }

    public record Amostra(int checkins, int membros, int minimo, boolean suficiente) {
    }

    /** "Comparar antes e depois": o ciclo anterior à intervenção ao lado dos seguintes. */
    public record Comparacao(int cicloIntervencao, List<Integer> ciclos, List<LinhaComparacao> linhas) {
    }

    public record LinhaComparacao(String indicador, List<Integer> valores) {
    }

    public record DetalheTime(
            Long timeId, String nome, long membros, int cicloFoco, int totalCiclos, List<Integer> ciclosFechados,
            List<CaixaAlerta> alertas, Series series, List<Barra> erroPorArea, List<Barra> dificuldades,
            Produtividade produtividade, Amostra amostra, Comparacao comparacao) {

        public boolean temDados() {
            return cicloFoco > 0;
        }
    }

    /**
     * @param ate último ciclo do intervalo do gráfico e ciclo em foco dos
     *            blocos; nulo mostra o último ciclo fechado
     */
    @Transactional(readOnly = true)
    public DetalheTime doTime(Long timeId, Integer ate) {
        TimeEntity time = times.findById(timeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Time não encontrado."));
        long membros = usuarios.countByTimeIdAndAtivoTrue(timeId);
        int total = configCiclos.total();

        Map<Integer, IndicadorTimeCiclo> serie = new TreeMap<>();
        indicadores.serieDoTime(timeId).forEach(i -> serie.put(i.getCiclo().getNumero(), i));
        List<Integer> fechados = new ArrayList<>(serie.keySet());
        if (fechados.isEmpty()) {
            return new DetalheTime(timeId, time.getNome(), membros, 0, total, List.of(), List.of(), null,
                    List.of(), null, null, null, null);
        }
        int foco = ate != null && serie.containsKey(ate) ? ate : fechados.getLast();
        IndicadorTimeCiclo atual = serie.get(foco);
        Integer intervencao = recomendacoes.primeiroCicloDeIntervencaoDoTime(timeId).orElse(null);

        return new DetalheTime(
                timeId, time.getNome(), membros, foco, total, fechados,
                alertas.vigentesNoCiclo(timeId, foco).stream().map(DetalheTimeQuery::caixa).toList(),
                series(serie, total, foco, intervencao),
                erroPorArea(timeId, atual),
                privacidade.dificuldadesDoTime(atual).map(DetalheTimeQuery::barrasDeDificuldade).orElse(null),
                produtividade(serie, atual, foco),
                new Amostra(atual.getNCheckins(), atual.getNMembros(), privacidade.minimo(),
                        privacidade.temAmostra(atual)),
                comparacao(serie, intervencao));
    }

    private static CaixaAlerta caixa(Alerta a) {
        DetalheAlerta d = a.getDetalhe();
        Integer queda = null;
        List<BigDecimal> engajamento = d.valores().get("engajamento");
        if (a.getPrioridade() == PrioridadeAlerta.ALTA && engajamento != null && engajamento.size() == 2) {
            queda = Arredondamento.paraTela(engajamento.get(0)) - Arredondamento.paraTela(engajamento.get(1));
        }
        return new CaixaAlerta(SeloAlerta.de(a), a.getCiclo().getNumero(),
                a.getCicloResolucao() == null ? null : a.getCicloResolucao().getNumero(), d.frase(), queda);
    }

    private static Series series(Map<Integer, IndicadorTimeCiclo> serie, int total, int ate, Integer intervencao) {
        List<String> rotulos = new ArrayList<>();
        List<Integer> tecnico = new ArrayList<>();
        List<Integer> confianca = new ArrayList<>();
        List<Integer> resistencia = new ArrayList<>();
        List<Integer> engajamento = new ArrayList<>();
        for (int c = 1; c <= total; c++) {
            rotulos.add("C" + c);
            IndicadorTimeCiclo i = c <= ate ? serie.get(c) : null;
            tecnico.add(tela(i, IndicadorTimeCiclo::getTecnico));
            confianca.add(tela(i, IndicadorTimeCiclo::getConfianca));
            resistencia.add(tela(i, IndicadorTimeCiclo::getResistencia));
            engajamento.add(tela(i, IndicadorTimeCiclo::getEngajamento));
        }
        return new Series(rotulos, tecnico, confianca, resistencia, engajamento, intervencao);
    }

    private List<Barra> erroPorArea(Long timeId, IndicadorTimeCiclo atual) {
        Map<String, Double> erro = CalculoErroPorArea.erroPorArea(
                coleta.respostas(timeId, atual.getCiclo().getId()));
        List<Barra> barras = new ArrayList<>();
        areas.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .forEach((Area a) -> {
                    Double valor = erro.get(a.getCodigo());
                    if (valor != null) {
                        int pct = Arredondamento.paraTela(valor);
                        barras.add(new Barra(a.getNome(), pct, pct + "%"));
                    }
                });
        return barras;
    }

    private static List<Barra> barrasDeDificuldade(List<ContagemDoTime> contagens) {
        return contagens.stream()
                .map(c -> new Barra(c.dificuldade(), c.percentual(),
                        c.vezes() + " (" + c.percentual() + "% de " + c.respostas() + " respostas)"))
                .toList();
    }

    private static Produtividade produtividade(Map<Integer, IndicadorTimeCiclo> serie, IndicadorTimeCiclo atual,
                                               int foco) {
        BigDecimal primeiro = serie.values().iterator().next().getTempoMedioMin();
        return new Produtividade(primeiro, atual.getTempoMedioMin(),
                atual.getTempoMedioMin().subtract(primeiro), Arredondamento.paraTela(atual.getTecnico()), foco);
    }

    private static Comparacao comparacao(Map<Integer, IndicadorTimeCiclo> serie, Integer intervencao) {
        if (intervencao == null || !serie.containsKey(intervencao - 1)) {
            return null;
        }
        List<Integer> ciclos = new ArrayList<>();
        ciclos.add(intervencao - 1);
        serie.keySet().stream().filter(c -> c >= intervencao).forEach(ciclos::add);

        List<LinhaComparacao> linhas = List.of(
                linha("Técnico", ciclos, serie, IndicadorTimeCiclo::getTecnico),
                linha("Confiança", ciclos, serie, IndicadorTimeCiclo::getConfianca),
                linha("Resistência", ciclos, serie, IndicadorTimeCiclo::getResistencia),
                linha("Engajamento", ciclos, serie, IndicadorTimeCiclo::getEngajamento));
        return new Comparacao(intervencao, ciclos, linhas);
    }

    private static LinhaComparacao linha(String nome, List<Integer> ciclos, Map<Integer, IndicadorTimeCiclo> serie,
                                         Function<IndicadorTimeCiclo, BigDecimal> campo) {
        List<Integer> valores = new ArrayList<>();
        ciclos.forEach(c -> valores.add(tela(serie.get(c), campo)));
        return new LinhaComparacao(nome, Collections.unmodifiableList(valores));
    }

    private static Integer tela(IndicadorTimeCiclo i, Function<IndicadorTimeCiclo, BigDecimal> campo) {
        if (i == null || campo.apply(i) == null) {
            return null;
        }
        return Arredondamento.paraTela(campo.apply(i));
    }
}
