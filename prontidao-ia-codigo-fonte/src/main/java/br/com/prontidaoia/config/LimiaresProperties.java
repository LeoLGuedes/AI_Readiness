package br.com.prontidaoia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limiares das regras de negocio da secao 5 da especificacao.
 *
 * <p>Nenhuma classe de regra guarda numero no codigo: todos os limiares entram
 * por aqui, a partir de {@code application.yml}, para que a defesa oral possa
 * apontar um unico arquivo quando perguntarem "de onde vem esse 5?".</p>
 *
 * @param amostraMinimaCheckins        secao 5.1 - abaixo disso, confianca e
 *                                     resistencia ficam nulas e as regras A e B
 *                                     nao sao avaliadas
 * @param quedaMinimaConfianca         secao 5.2 A - queda de confianca exigida
 *                                     em cada um dos dois intervalos
 * @param confiancaAlta                secao 5.2 B - piso de confianca
 * @param acertoMinimoVerificacao      secao 5.2 B - teto de acerto em VERIFICACAO
 * @param quedaEngajamentoPrioridadeAlta secao 5.2 - queda de engajamento que
 *                                     faz o alerta nascer com prioridade ALTA
 * @param ciclosParaEncerramento       secao 5.2 - ciclos seguidos exigidos para
 *                                     encerrar um alerta
 * @param dificuldadesPorRecomendacao  secao 5.3 - quantas dificuldades mais
 *                                     marcadas viram recomendacao no alerta A
 */
@ConfigurationProperties(prefix = "prontidao.limiares")
public record LimiaresProperties(
        int amostraMinimaCheckins,
        int quedaMinimaConfianca,
        int confiancaAlta,
        int acertoMinimoVerificacao,
        int quedaEngajamentoPrioridadeAlta,
        int ciclosParaEncerramento,
        int dificuldadesPorRecomendacao) {
}
