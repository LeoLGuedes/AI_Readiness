package br.com.prontidaoia.diagnostico.indicador;

/**
 * Seção 5.1, linha 4. <b>Engajamento</b> = concluídas / esperadas × 100.
 *
 * <ul>
 *   <li>esperadas = n_membros × (1 + atividades liberadas ao time no ciclo);</li>
 *   <li>concluídas = check-ins + tentativas finalizadas.</li>
 * </ul>
 *
 * <p>Exemplo da seção 8.4: 8 membros e 2 atividades dão 24 pendências; 7
 * check-ins e 12 tentativas dão 19 de 24, ou 79,2.</p>
 */
public final class CalculoEngajamento {

    private CalculoEngajamento() {
    }

    public static int esperadas(int membros, int atividadesLiberadas) {
        return membros * (1 + atividadesLiberadas);
    }

    public static double calcular(int membros, int atividadesLiberadas, int checkins, int tentativasFinalizadas) {
        int esperadas = esperadas(membros, atividadesLiberadas);
        if (esperadas == 0) {
            return 0.0;
        }
        return (checkins + tentativasFinalizadas) * 100.0 / esperadas;
    }
}
