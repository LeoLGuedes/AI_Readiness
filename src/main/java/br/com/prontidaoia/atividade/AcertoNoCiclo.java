package br.com.prontidaoia.atividade;

/** Quantas respostas e quantos acertos uma pessoa teve num ciclo. */
public record AcertoNoCiclo(Long cicloId, long respostas, long acertos) {

    public double percentual() {
        return respostas == 0 ? 0 : acertos * 100.0 / respostas;
    }
}
