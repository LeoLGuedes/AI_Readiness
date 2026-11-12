package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.config.LimiaresProperties;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.alerta.StatusAlerta;
import br.com.prontidaoia.diagnostico.alerta.TipoAlerta;
import br.com.prontidaoia.diagnostico.recomendacao.Recomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.diagnostico.recomendacao.StatusRecomendacao;
import br.com.prontidaoia.diagnostico.recomendacao.TipoIntervencao;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quadro 3 do relatório: as recomendações de um alerta. */
@Service
public class RecomendacaoQuery {

    private final AlertaRepository alertas;
    private final RecomendacaoRepository recomendacoes;
    private final LimiaresProperties limiares;

    public RecomendacaoQuery(AlertaRepository alertas, RecomendacaoRepository recomendacoes,
                             LimiaresProperties limiares) {
        this.alertas = alertas;
        this.recomendacoes = recomendacoes;
        this.limiares = limiares;
    }

    public record Item(Long id, String texto, String textoDoCatalogo, String executor, boolean humana,
                       String dificuldade, int respostas, int total, StatusRecomendacao status,
                       LocalDate dataPrevista) {

        /** "5 de 7 respostas". */
        public String contagem() {
            return respostas + " de " + total + " respostas";
        }

        public int percentual() {
            return total == 0 ? 0 : (int) Math.round(respostas * 100.0 / total);
        }

        public boolean temDificuldade() {
            return dificuldade != null;
        }

        public String rotuloStatus() {
            return switch (status) {
                case SUGERIDA -> "Sugerida";
                case ACEITA -> "Aceita";
                case AJUSTADA -> "Ajustada";
                case ADIADA -> "Adiada";
            };
        }
    }

    public record TelaRecomendacao(Long alertaId, Long timeId, String time, SeloAlerta selo, int ciclo,
                                   String frase, String regra, List<Item> itens) {

        /** "Sabe usar, mas não se sente pronto – Atendimento – ciclo 4". */
        public String titulo() {
            return selo.nome() + " – " + time + " – ciclo " + ciclo;
        }

        /** A gestão ainda pode decidir: o alerta está aberto. */
        public boolean decidivel() {
            return selo.status() == StatusAlerta.ABERTO;
        }
    }

    @Transactional(readOnly = true)
    public TelaRecomendacao doAlerta(Long alertaId) {
        Alerta alerta = alertas.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));
        List<Item> itens = recomendacoes.doAlerta(alertaId).stream().map(RecomendacaoQuery::item).toList();
        return new TelaRecomendacao(alerta.getId(), alerta.getTime().getId(), alerta.getTime().getNome(),
                SeloAlerta.de(alerta), alerta.getCiclo().getNumero(), alerta.getDetalhe().frase(),
                regra(alerta.getTipo()), itens);
    }

    private static Item item(Recomendacao r) {
        return new Item(r.getId(), r.textoVigente(), r.getIntervencao().getDescricao(),
                r.getIntervencao().getExecutor(), r.getIntervencao().getTipo() == TipoIntervencao.HUMANA,
                r.getDificuldade() == null ? null : r.getDificuldade().getDescricao(),
                r.getNRespostas(), r.getTotalRespostas(), r.getStatus(), r.getDataPrevista());
    }

    /** A regra ativa em linguagem simples, com os limiares configurados. */
    private String regra(TipoAlerta tipo) {
        if (tipo == TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA) {
            return "Regra A: o técnico subiu em dois ciclos seguidos e a confiança caiu pelo menos "
                    + limiares.quedaMinimaConfianca() + " pontos em cada um deles.";
        }
        return "Regra B: confiança de pelo menos " + limiares.confiancaAlta()
                + " com acerto em verificação abaixo de " + limiares.acertoMinimoVerificacao()
                + "%, em dois ciclos seguidos.";
    }
}
