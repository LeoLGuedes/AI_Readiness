package br.com.prontidaoia.simulacao;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Elenco fixo do cenário da seção 3: uma gestora, três times de oito pessoas.
 *
 * <p>Os nomes são fictícios e a empresa é a "Seguradora Aurora" (decisão 11 de
 * {@code docs/decisoes.md}). A lista é uma constante porque o cenário precisa
 * ser reproduzível: a mesma pessoa tem que cair no mesmo time a cada
 * "Restaurar cenário inicial".</p>
 */
public final class PessoasDemo {

    public static final String DOMINIO = "aurora.com.br";

    public static final String TIME_ATENDIMENTO = "Atendimento";
    public static final String TIME_FINANCEIRO = "Financeiro";
    public static final String TIME_MARKETING = "Marketing";

    /** Gerente de RH (papel GESTOR): acompanha os três departamentos. */
    public static final String GESTORA = "Renata Guimarães";

    /** Líderes de departamento (papel LIDER): cada um vê só o próprio time. */
    public static final String LIDER_ATENDIMENTO = "Mônica Fagundes";
    public static final String LIDER_FINANCEIRO = "Ricardo Pimenta";
    public static final String LIDER_MARKETING = "Beatriz Arantes";

    /** Atendente que faz o check-in e as atividades ao vivo na demonstração. */
    public static final String CARLA = "Carla Menezes";

    /**
     * Quem pula o check-in do ciclo 4, para que o Atendimento feche com 7 de 8
     * (seção 8.2). Não pode ser a Carla.
     */
    public static final String FALTANTE_CICLO_4 = "Marcos Vinícius Lima";

    private static final List<String> ATENDIMENTO = List.of(
            CARLA,
            "Bruno Tavares",
            "Patrícia Alves",
            "Diego Nogueira",
            "Simone Barreto",
            "Rafael Amorim",
            "Juliana Peixoto",
            FALTANTE_CICLO_4);

    private static final List<String> FINANCEIRO = List.of(
            "Eduardo Salgado",
            "Vanessa Cordeiro",
            "Thiago Rabelo",
            "Larissa Fontes",
            "Gustavo Serra",
            "Camila Duarte",
            "Rodrigo Bastos",
            "Fernanda Quintela");

    private static final List<String> MARKETING = List.of(
            "Letícia Prado",
            "André Bomfim",
            "Priscila Moraes",
            "Henrique Valadares",
            "Tatiane Rocha",
            "Wesley Godoi",
            "Isabela Cunha",
            "Otávio Ramalho");

    private PessoasDemo() {
    }

    /** Os três times, na ordem em que aparecem nas telas do gestor. */
    public static List<TimeDemo> times() {
        return List.of(
                new TimeDemo(TIME_ATENDIMENTO, LIDER_ATENDIMENTO, ATENDIMENTO),
                new TimeDemo(TIME_FINANCEIRO, LIDER_FINANCEIRO, FINANCEIRO),
                new TimeDemo(TIME_MARKETING, LIDER_MARKETING, MARKETING));
    }

    /**
     * Monta o e-mail a partir do nome: "Carla Menezes" vira
     * {@code carla.menezes@aurora.com.br}, sem acentos.
     */
    public static String email(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String[] partes = semAcento.trim().split("\\s+");
        String primeiro = partes[0];
        String ultimo = partes[partes.length - 1];
        return (primeiro + "." + ultimo).toLowerCase(Locale.ROOT) + "@" + DOMINIO;
    }

    /**
     * Um time do cenário, o nome do seu líder e as oito pessoas que o compõem.
     * O líder não é membro: não entra nos indicadores do time.
     */
    public record TimeDemo(String nome, String lider, List<String> membros) {
    }
}
