package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.atividade.Area;
import br.com.prontidaoia.atividade.AreaRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.StatusCiclo;
import br.com.prontidaoia.comum.Arredondamento;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.indicador.CalculoErroPorArea;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.diagnostico.indicador.RespostaAvaliada;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quadro 1 do relatório: a visão geral dos times num ciclo fechado.
 *
 * <p>Só lê fotografias de indicador, alertas e respostas agregadas por área.
 * Nunca lê check-in (regra de privacidade da seção 4).</p>
 *
 * <p>Hierarquia de departamentos: a consulta recebe os times que quem pede
 * pode ver. A gerente de RH recebe todos; o líder, só o seu. O "maior erro"
 * e o "maior ganho" são calculados apenas sobre os times visíveis, para que um
 * líder não deduza nada dos outros departamentos.</p>
 */
@Service
public class VisaoGeralQuery {

    private final CicloRepository ciclos;
    private final TimeRepository times;
    private final IndicadorRepository indicadores;
    private final AlertaRepository alertas;
    private final ColetaDoCiclo coleta;
    private final AreaRepository areas;

    public VisaoGeralQuery(CicloRepository ciclos, TimeRepository times, IndicadorRepository indicadores,
                           AlertaRepository alertas, ColetaDoCiclo coleta, AreaRepository areas) {
        this.ciclos = ciclos;
        this.times = times;
        this.indicadores = indicadores;
        this.alertas = alertas;
        this.coleta = coleta;
        this.areas = areas;
    }

    public record CartaoTime(Long timeId, String nome, List<Kpi> kpis, List<SeloAlerta> alertas,
                             BigDecimal tempoMedioMin, int checkins, int membros) {
    }

    /** "Verificação das respostas da IA – 42% de erro". */
    public record MaiorErro(String area, int erro) {
    }

    /** "Financeiro: +19 pontos (34 → 53)". */
    public record GanhoDeConfianca(String time, int ganho, int de, int para) {
    }

    public record VisaoGeral(List<Ciclo> ciclosFechados, Ciclo ciclo, MaiorErro maiorErro,
                             List<CartaoTime> cartoes, GanhoDeConfianca destaque, boolean empresaToda) {

        public boolean temCiclo() {
            return ciclo != null;
        }

        /** "da empresa" para o RH; "do departamento" para o líder. */
        public String abrangencia() {
            return empresaToda ? "da empresa" : "do departamento";
        }
    }

    /** Visão da empresa toda (gerente de RH). */
    @Transactional(readOnly = true)
    public VisaoGeral doCiclo(Integer numero) {
        return doCiclo(numero, null);
    }

    /**
     * @param numero   ciclo pedido; nulo ou inexistente mostra o último fechado
     * @param visiveis ids dos times que quem pede pode ver; nulo significa
     *                 todos (gerente de RH)
     */
    @Transactional(readOnly = true)
    public VisaoGeral doCiclo(Integer numero, Set<Long> visiveis) {
        boolean empresaToda = visiveis == null;
        List<Ciclo> fechados = ciclos.findByStatusOrderByNumero(StatusCiclo.FECHADO);
        if (fechados.isEmpty()) {
            return new VisaoGeral(List.of(), null, null, List.of(), null, empresaToda);
        }
        Ciclo ciclo = fechados.stream()
                .filter(c -> c.getNumero().equals(numero))
                .findFirst()
                .orElse(fechados.getLast());

        Map<Long, IndicadorTimeCiclo> doCiclo = porTime(ciclo.getNumero());
        Map<Long, IndicadorTimeCiclo> anterior = porTime(ciclo.getNumero() - 1);
        Map<Long, IndicadorTimeCiclo> primeiro = porTime(1);

        List<CartaoTime> cartoes = new ArrayList<>();
        List<RespostaAvaliada> daEmpresa = new ArrayList<>();
        GanhoDeConfianca destaque = null;
        for (TimeEntity time : times.findAllByOrderByNome()) {
            if (!empresaToda && !visiveis.contains(time.getId())) {
                continue;
            }
            IndicadorTimeCiclo atual = doCiclo.get(time.getId());
            if (atual == null) {
                continue;
            }
            IndicadorTimeCiclo antes = anterior.get(time.getId());
            cartoes.add(new CartaoTime(time.getId(), time.getNome(), kpis(atual, antes),
                    alertas.vigentesNoCiclo(time.getId(), ciclo.getNumero()).stream().map(SeloAlerta::de).toList(),
                    atual.getTempoMedioMin(), atual.getNCheckins(), atual.getNMembros()));
            daEmpresa.addAll(coleta.respostas(time.getId(), ciclo.getId()));

            IndicadorTimeCiclo inicio = primeiro.get(time.getId());
            if (ciclo.getNumero() > 1 && inicio != null && inicio.getConfianca() != null
                    && atual.getConfianca() != null) {
                int de = Arredondamento.paraTela(inicio.getConfianca());
                int para = Arredondamento.paraTela(atual.getConfianca());
                if (destaque == null || para - de > destaque.ganho()) {
                    destaque = new GanhoDeConfianca(time.getNome(), para - de, de, para);
                }
            }
        }
        // Com um só departamento, "maior ganho entre os times" não faz sentido.
        return new VisaoGeral(fechados, ciclo, maiorErro(daEmpresa), cartoes,
                cartoes.size() > 1 ? destaque : null, empresaToda);
    }

    /** Os quatro indicadores do Quadro 1, na ordem do wireframe. */
    static List<Kpi> kpis(IndicadorTimeCiclo atual, IndicadorTimeCiclo antes) {
        return List.of(
                Kpi.de("Técnico", atual.getTecnico(), antes == null ? null : antes.getTecnico(), true),
                Kpi.de("Confiança", atual.getConfianca(), antes == null ? null : antes.getConfianca(), true),
                Kpi.de("Resistência", atual.getResistencia(), antes == null ? null : antes.getResistencia(), false),
                Kpi.de("Engajamento", atual.getEngajamento(), antes == null ? null : antes.getEngajamento(), true));
    }

    /** Área de maior erro, agrupando as respostas de todos os times (decisão D03). */
    private MaiorErro maiorErro(List<RespostaAvaliada> respostas) {
        Map<String, String> nomes = new HashMap<>();
        areas.findAll().forEach((Area a) -> nomes.put(a.getCodigo(), a.getNome()));
        Optional<Map.Entry<String, Double>> maior = CalculoErroPorArea.erroPorArea(respostas).entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue));
        return maior.map(e -> new MaiorErro(nomes.getOrDefault(e.getKey(), e.getKey()),
                Arredondamento.paraTela(e.getValue()))).orElse(null);
    }

    private Map<Long, IndicadorTimeCiclo> porTime(int numero) {
        Map<Long, IndicadorTimeCiclo> mapa = new HashMap<>();
        ciclos.findByNumero(numero).ifPresent(c ->
                indicadores.findByCicloId(c.getId()).forEach(i -> mapa.put(i.getTime().getId(), i)));
        return mapa;
    }
}
