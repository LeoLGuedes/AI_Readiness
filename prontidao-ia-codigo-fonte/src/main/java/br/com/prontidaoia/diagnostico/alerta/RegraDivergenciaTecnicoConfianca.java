package br.com.prontidaoia.diagnostico.alerta;

import br.com.prontidaoia.comum.Arredondamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Regra A, divergência técnico × confiança</b> (seção 5.2), aplicada no
 * fechamento do ciclo {@code c}:
 *
 * <pre>
 *   técnico(c) &gt; técnico(c−1) &gt; técnico(c−2)
 *   e confiança(c−2) − confiança(c−1) ≥ 5
 *   e confiança(c−1) − confiança(c)   ≥ 5
 * </pre>
 *
 * Nome exibido: "Sabe usar, mas não se sente pronto".
 *
 * <p>No cenário da seção 8.4, o Atendimento dispara no ciclo 4
 * (confiança 59 → 53 → 47) e não no ciclo 3, porque de 63 para 59 a queda é
 * só de 4 pontos.</p>
 */
public final class RegraDivergenciaTecnicoConfianca {

    private final BigDecimal quedaMinima;

    public RegraDivergenciaTecnicoConfianca(int quedaMinimaConfianca) {
        this.quedaMinima = BigDecimal.valueOf(quedaMinimaConfianca);
    }

    public Optional<DisparoDeRegra> avaliar(SerieDoTime serie, int c) {
        Optional<PontoDoCiclo> atual = serie.no(c);
        Optional<PontoDoCiclo> anterior = serie.no(c - 1);
        Optional<PontoDoCiclo> antesDoAnterior = serie.no(c - 2);
        if (atual.isEmpty() || anterior.isEmpty() || antesDoAnterior.isEmpty()) {
            return Optional.empty();
        }
        PontoDoCiclo p0 = atual.get();
        PontoDoCiclo p1 = anterior.get();
        PontoDoCiclo p2 = antesDoAnterior.get();
        if (!p0.temAmostra() || !p1.temAmostra() || !p2.temAmostra()) {
            return Optional.empty();
        }

        boolean tecnicoSobeDuasVezes =
                p0.tecnico().compareTo(p1.tecnico()) > 0 && p1.tecnico().compareTo(p2.tecnico()) > 0;
        boolean confiancaCaiDuasVezes =
                p2.confianca().subtract(p1.confianca()).compareTo(quedaMinima) >= 0
                && p1.confianca().subtract(p0.confianca()).compareTo(quedaMinima) >= 0;

        if (!tecnicoSobeDuasVezes || !confiancaCaiDuasVezes) {
            return Optional.empty();
        }

        String frase = "Técnico subiu de " + tela(p2.tecnico()) + " para " + tela(p1.tecnico())
                + " e " + tela(p0.tecnico()) + "; confiança caiu de " + tela(p2.confianca())
                + " para " + tela(p1.confianca()) + " e " + tela(p0.confianca()) + ".";

        return Optional.of(new DisparoDeRegra(TipoAlerta.DIVERGENCIA_TECNICO_CONFIANCA, new DetalheAlerta(
                "A",
                c,
                Map.of("tecnico", List.of(p2.tecnico(), p1.tecnico(), p0.tecnico()),
                        "confianca", List.of(p2.confianca(), p1.confianca(), p0.confianca()),
                        "engajamento", List.of(p1.engajamento(), p0.engajamento())),
                frase)));
    }

    private static int tela(BigDecimal valor) {
        return Arredondamento.paraTela(valor);
    }
}
