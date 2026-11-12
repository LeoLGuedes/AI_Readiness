package br.com.prontidaoia.atividade;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Correção de uma atividade. Código puro: recebe o gabarito e as escolhas e
 * devolve o que acertou, sem banco e sem Spring.
 *
 * <p>Regra da seção 6: <b>só se finaliza com todas as questões respondidas</b>.
 * Por isso a correção recusa escolhas em branco em vez de contá-las como erro.</p>
 */
public final class Correcao {

    private Correcao() {
    }

    /** O que a correção precisa saber de cada questão. */
    public record ItemGabarito(long questaoId, int ordem, int indiceCorreto, int alternativas) {
    }

    /** Resultado da correção, por questão e no total. */
    public record Resultado(Map<Long, Boolean> acertouPorQuestao, int acertos, int total) {
    }

    /**
     * Corrige a atividade.
     *
     * @param gabarito as questões da atividade, em ordem
     * @param escolhas índice da alternativa escolhida, por id de questão
     * @throws RegraVioladaException se alguma questão ficou em branco ou tem
     *                               uma alternativa que não existe
     */
    public static Resultado corrigir(List<ItemGabarito> gabarito, Map<Long, Integer> escolhas) {
        List<Integer> emBranco = gabarito.stream()
                .filter(item -> escolhas.get(item.questaoId()) == null)
                .map(ItemGabarito::ordem)
                .toList();
        if (!emBranco.isEmpty()) {
            throw new RegraVioladaException("Responda todas as questões antes de finalizar. Faltam: "
                    + emBranco.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ".");
        }

        Map<Long, Boolean> acertou = new LinkedHashMap<>();
        int acertos = 0;
        for (ItemGabarito item : gabarito) {
            int escolhida = escolhas.get(item.questaoId());
            if (escolhida < 0 || escolhida >= item.alternativas()) {
                throw new RegraVioladaException("Alternativa inválida na questão " + item.ordem() + ".");
            }
            boolean certa = escolhida == item.indiceCorreto();
            acertou.put(item.questaoId(), certa);
            if (certa) {
                acertos++;
            }
        }
        return new Resultado(acertou, acertos, gabarito.size());
    }
}
