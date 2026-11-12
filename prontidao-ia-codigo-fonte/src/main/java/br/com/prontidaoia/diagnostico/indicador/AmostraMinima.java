package br.com.prontidaoia.diagnostico.indicador;

/**
 * Seção 5.1, "amostra mínima". Com menos check-ins que o mínimo (5):
 * <ul>
 *   <li>confiança e resistência ficam nulas;</li>
 *   <li>as dificuldades não são exibidas ao gestor;</li>
 *   <li>as regras A e B não são avaliadas para o time naquele ciclo.</li>
 * </ul>
 *
 * <p>É também a proteção de privacidade da seção 4: com poucas respostas, a
 * média deixaria de esconder quem respondeu o quê.</p>
 */
public final class AmostraMinima {

    private final int minimoDeCheckins;

    public AmostraMinima(int minimoDeCheckins) {
        this.minimoDeCheckins = minimoDeCheckins;
    }

    public boolean suficiente(int checkins) {
        return checkins >= minimoDeCheckins;
    }

    public int minimo() {
        return minimoDeCheckins;
    }
}
