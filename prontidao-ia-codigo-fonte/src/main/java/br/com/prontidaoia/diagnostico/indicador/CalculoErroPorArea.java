package br.com.prontidaoia.diagnostico.indicador;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Seção 5.1, linha 6. <b>Erro por área</b> = 100 − % de acerto por área.
 *
 * <p>Não tem fotografia no banco: é calculado na leitura, a partir das
 * respostas, que não mudam depois que o ciclo fecha.</p>
 */
public final class CalculoErroPorArea {

    private CalculoErroPorArea() {
    }

    /** % de acerto por código de área. Áreas sem resposta não aparecem. */
    public static Map<String, Double> acertoPorArea(List<RespostaAvaliada> respostas) {
        return respostas.stream().collect(Collectors.groupingBy(
                RespostaAvaliada::area,
                TreeMap::new,
                Collectors.averagingDouble(r -> r.correta() ? 100.0 : 0.0)));
    }

    /** % de erro por código de área. */
    public static Map<String, Double> erroPorArea(List<RespostaAvaliada> respostas) {
        Map<String, Double> erro = new TreeMap<>();
        acertoPorArea(respostas).forEach((area, acerto) -> erro.put(area, 100.0 - acerto));
        return erro;
    }
}
