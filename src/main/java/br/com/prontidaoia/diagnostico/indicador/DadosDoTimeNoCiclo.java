package br.com.prontidaoia.diagnostico.indicador;

import java.time.Duration;
import java.util.List;

/**
 * Tudo o que o fechamento lê de um time num ciclo, já sem identificar
 * ninguém: as listas de check-in não dizem de quem é cada resposta.
 *
 * @param respostas            respostas das tentativas finalizadas
 * @param confiancas           pergunta 1 de cada check-in
 * @param receios              pergunta 2 de cada check-in
 * @param duracoes             duração de cada tentativa finalizada
 * @param membros              pessoas no time
 * @param atividadesLiberadas  atividades no escopo do time no ciclo
 */
public record DadosDoTimeNoCiclo(
        List<RespostaAvaliada> respostas,
        List<Integer> confiancas,
        List<Integer> receios,
        List<Duration> duracoes,
        int membros,
        int atividadesLiberadas) {

    public int checkins() {
        return confiancas.size();
    }

    public int tentativasFinalizadas() {
        return duracoes.size();
    }
}
