package br.com.prontidaoia.diagnostico.alerta;

import br.com.prontidaoia.comum.Arredondamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Regra B, confiança sem verificação</b> (seção 5.2):
 *
 * <pre>
 *   confiança ≥ 75 e acerto em VERIFICACAO &lt; 50%,
 *   no ciclo c e no ciclo c−1
 * </pre>
 *
 * Nome exibido: "Confiança alta sem senso crítico".
 *
 * <p>No cenário da seção 8.4, o Marketing dispara a partir do ciclo 2
 * (confiança 78 e 81, verificação 38% e 40%).</p>
 */
public final class RegraConfiancaSemVerificacao {

    private final BigDecimal confiancaAlta;
    private final BigDecimal acertoMinimo;

    public RegraConfiancaSemVerificacao(int confiancaAlta, int acertoMinimoVerificacao) {
        this.confiancaAlta = BigDecimal.valueOf(confiancaAlta);
        this.acertoMinimo = BigDecimal.valueOf(acertoMinimoVerificacao);
    }

    /**
     * A condição num único ciclo. O alerta exige dois ciclos seguidos; o
     * encerramento usa esta mesma condição (decisão D37).
     */
    public boolean condicaoNoCiclo(PontoDoCiclo ponto) {
        return ponto.temAmostra()
                && ponto.acertoVerificacao() != null
                && ponto.confianca().compareTo(confiancaAlta) >= 0
                && ponto.acertoVerificacao().compareTo(acertoMinimo) < 0;
    }

    public Optional<DisparoDeRegra> avaliar(SerieDoTime serie, int c) {
        Optional<PontoDoCiclo> atual = serie.no(c);
        Optional<PontoDoCiclo> anterior = serie.no(c - 1);
        if (atual.isEmpty() || anterior.isEmpty()
                || !condicaoNoCiclo(atual.get()) || !condicaoNoCiclo(anterior.get())) {
            return Optional.empty();
        }
        PontoDoCiclo p0 = atual.get();
        PontoDoCiclo p1 = anterior.get();

        String frase = "Confiança de " + tela(p1.confianca()) + " e " + tela(p0.confianca())
                + " nos ciclos " + (c - 1) + " e " + c + ", com acerto em verificação de "
                + tela(p1.acertoVerificacao()) + "% e " + tela(p0.acertoVerificacao()) + "%.";

        return Optional.of(new DisparoDeRegra(TipoAlerta.CONFIANCA_SEM_VERIFICACAO, new DetalheAlerta(
                "B",
                c,
                Map.of("confianca", List.of(p1.confianca(), p0.confianca()),
                        "verificacao", List.of(p1.acertoVerificacao(), p0.acertoVerificacao()),
                        "engajamento", List.of(p1.engajamento(), p0.engajamento())),
                frase)));
    }

    private static int tela(BigDecimal valor) {
        return Arredondamento.paraTela(valor);
    }
}
