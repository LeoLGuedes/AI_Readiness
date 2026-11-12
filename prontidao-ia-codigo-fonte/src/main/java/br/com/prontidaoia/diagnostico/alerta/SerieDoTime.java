package br.com.prontidaoia.diagnostico.alerta;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Os pontos de um time, ciclo a ciclo, para as regras consultarem c, c−1 e c−2. */
public final class SerieDoTime {

    private final Map<Integer, PontoDoCiclo> porCiclo;

    private SerieDoTime(Map<Integer, PontoDoCiclo> porCiclo) {
        this.porCiclo = porCiclo;
    }

    public static SerieDoTime de(List<PontoDoCiclo> pontos) {
        return new SerieDoTime(pontos.stream()
                .collect(Collectors.toMap(PontoDoCiclo::ciclo, Function.identity())));
    }

    public Optional<PontoDoCiclo> no(int ciclo) {
        return Optional.ofNullable(porCiclo.get(ciclo));
    }
}
