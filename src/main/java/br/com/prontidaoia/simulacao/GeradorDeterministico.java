package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.Questao;
import br.com.prontidaoia.atividade.QuestaoRepository;
import br.com.prontidaoia.atividade.Resposta;
import br.com.prontidaoia.atividade.RespostaRepository;
import br.com.prontidaoia.atividade.Tentativa;
import br.com.prontidaoia.atividade.TentativaRepository;
import br.com.prontidaoia.checkin.Checkin;
import br.com.prontidaoia.checkin.CheckinRepository;
import br.com.prontidaoia.checkin.Dificuldade;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.config.DemoProperties;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.RespostaAvaliada;
import br.com.prontidaoia.simulacao.PerfilTime.Metas;
import br.com.prontidaoia.simulacao.RoteiroCarla.AtividadeFixa;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera as respostas <b>individuais</b> de um ciclo (seção 8): a escala de cada
 * pessoa, o acerto de cada questão e o tempo de cada tentativa, de modo que os
 * totais do time atinjam as metas de {@link PerfilTime}.
 *
 * <p><b>Como os números batem (decisão D01):</b> as metas viram totais
 * inteiros por time. Uma confiança de 63 com 8 check-ins vira "a soma de
 * (resposta − 1) é 20", que dá 62,5. O gerador <b>desconta o que já existe</b>
 * (quem já respondeu) e o que está <b>reservado</b> (a Carla no ciclo 4) e
 * reparte só o restante entre quem falta. Por isso o total fecha tanto quando
 * o cenário é gerado de uma vez quanto quando parte das pessoas responde pela
 * tela.</p>
 *
 * <p>A semente fixa decide apenas o que não afeta limiar: quem erra cada
 * questão, quem deixa pendência e os segundos de cada tentativa.</p>
 *
 * <p>Nenhum indicador é gravado aqui. Eles saem sempre do
 * {@code FechamentoCicloService}.</p>
 */
@Component
public class GeradorDeterministico {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern SLOT_B = Pattern.compile("^\\d+B$");
    private static final List<String> AREAS_AJUSTAVEIS = List.of("FORMULACAO", "DADOS", "APLICACAO");

    private final CicloService ciclos;
    private final TimeRepository times;
    private final UsuarioRepository usuarios;
    private final CheckinRepository checkins;
    private final DificuldadeRepository dificuldades;
    private final AtividadeLiberadaRepository liberadas;
    private final QuestaoRepository questoes;
    private final TentativaRepository tentativas;
    private final RespostaRepository respostas;
    private final ColetaDoCiclo coleta;
    private final DemoProperties demo;
    private final Clock clock;

    public GeradorDeterministico(CicloService ciclos, TimeRepository times, UsuarioRepository usuarios,
                                 CheckinRepository checkins, DificuldadeRepository dificuldades,
                                 AtividadeLiberadaRepository liberadas, QuestaoRepository questoes,
                                 TentativaRepository tentativas, RespostaRepository respostas,
                                 ColetaDoCiclo coleta, DemoProperties demo, Clock clock) {
        this.ciclos = ciclos;
        this.times = times;
        this.usuarios = usuarios;
        this.checkins = checkins;
        this.dificuldades = dificuldades;
        this.liberadas = liberadas;
        this.questoes = questoes;
        this.tentativas = tentativas;
        this.respostas = respostas;
        this.coleta = coleta;
        this.demo = demo;
        this.clock = clock;
    }

    /** Quantas respostas foram geradas. */
    public record Geracao(int ciclo, int checkins, int atividades) {
    }

    /** Gera as respostas de quem ainda não respondeu no ciclo aberto. */
    @Transactional
    public Geracao gerarCicloAberto() {
        Ciclo ciclo = ciclos.cicloAberto()
                .orElseThrow(() -> new RegraVioladaException("Não há ciclo aberto para gerar respostas."));
        int totalCheckins = 0;
        int totalAtividades = 0;

        List<TimeEntity> todos = times.findAllByOrderByNome();
        for (int i = 0; i < todos.size(); i++) {
            TimeEntity time = todos.get(i);
            Metas metas = PerfilTime.de(time.getNome(), ciclo.getNumero());
            Random rnd = new Random(demo.semente() * 31 + ciclo.getNumero() * 1000L + i);
            List<Usuario> membros = usuarios.findByTimeIdAndAtivoTrueAndAutocadastroFalseOrderByNome(time.getId());
            boolean carlaReservada = ciclo.getNumero() == RoteiroCarla.CICLO_AO_VIVO
                    && PessoasDemo.TIME_ATENDIMENTO.equals(time.getNome());

            List<Usuario> respondemCheckin = membros.stream()
                    .filter(u -> !(ciclo.getNumero() == 4 && PessoasDemo.FALTANTE_CICLO_4.equals(u.getNome())))
                    .toList();

            totalCheckins += gerarCheckins(ciclo, metas, respondemCheckin, carlaReservada, rnd);
            totalAtividades += gerarAtividades(ciclo, time, metas, membros, respondemCheckin.size(),
                    carlaReservada, rnd);
        }
        return new Geracao(ciclo.getNumero(), totalCheckins, totalAtividades);
    }

    // ------------------------------------------------------------------
    // Check-ins
    // ------------------------------------------------------------------

    private int gerarCheckins(Ciclo ciclo, Metas metas, List<Usuario> respondem, boolean carlaReservada,
                              Random rnd) {
        int n = respondem.size();
        int somaConfianca = (int) Math.round(metas.confianca() * 4 * n / 100.0);
        int somaReceio = (int) Math.round(metas.resistencia() * 4 * n / 100.0);
        Map<String, Integer> faltaMarcar = new LinkedHashMap<>(metas.dificuldades());

        List<Usuario> aGerar = new ArrayList<>();
        for (Usuario pessoa : respondem) {
            var existente = checkins.findByUsuarioIdAndCicloId(pessoa.getId(), ciclo.getId());
            if (existente.isPresent()) {
                Checkin c = existente.get();
                somaConfianca -= c.getConfianca() - 1;
                somaReceio -= c.getReceioSubstituicao() - 1;
                c.getDificuldades().forEach(d -> faltaMarcar.merge(d.getCodigo(), -1, Integer::sum));
            } else if (carlaReservada && ehCarla(pessoa)) {
                somaConfianca -= RoteiroCarla.CONFIANCA - 1;
                somaReceio -= RoteiroCarla.RECEIO - 1;
                RoteiroCarla.DIFICULDADES.forEach(d -> faltaMarcar.merge(d, -1, Integer::sum));
            } else {
                aGerar.add(pessoa);
            }
        }
        if (aGerar.isEmpty()) {
            return 0;
        }

        int[] confianca = espalhar(somaConfianca, aGerar.size(), rnd);
        int[] receio = espalhar(somaReceio, aGerar.size(), rnd);

        List<Set<Dificuldade>> marcadas = new ArrayList<>();
        aGerar.forEach(p -> marcadas.add(new LinkedHashSet<>()));
        for (Dificuldade d : dificuldades.findAllByOrderById()) {
            int vezes = Math.max(0, Math.min(aGerar.size(), faltaMarcar.getOrDefault(d.getCodigo(), 0)));
            List<Integer> ordem = embaralhados(aGerar.size(), rnd);
            for (int k = 0; k < vezes; k++) {
                marcadas.get(ordem.get(k)).add(d);
            }
        }

        for (int i = 0; i < aGerar.size(); i++) {
            checkins.save(new Checkin(aGerar.get(i), ciclo, confianca[i] + 1, receio[i] + 1,
                    marcadas.get(i), momento(ciclo, i)));
        }
        return aGerar.size();
    }

    /**
     * Reparte {@code total} em {@code pessoas} valores de 0 a 4 (resposta − 1),
     * com soma exata. A semente só varia a distribuição, nunca a soma.
     */
    static int[] espalhar(int total, int pessoas, Random rnd) {
        int soma = Math.max(0, Math.min(4 * pessoas, total));
        int[] valores = new int[pessoas];
        List<Integer> ordem = embaralhados(pessoas, rnd);
        for (int i = 0; i < pessoas; i++) {
            valores[ordem.get(i)] = soma / pessoas + (i < soma % pessoas ? 1 : 0);
        }
        for (int k = 0; k < pessoas / 2; k++) {
            int a = rnd.nextInt(pessoas);
            int b = rnd.nextInt(pessoas);
            if (a != b && valores[a] < 4 && valores[b] > 0) {
                valores[a]++;
                valores[b]--;
            }
        }
        return valores;
    }

    // ------------------------------------------------------------------
    // Atividades
    // ------------------------------------------------------------------

    /** Uma tentativa a gerar: quem, qual atividade liberada e as questões. */
    private record Vaga(Usuario pessoa, AtividadeLiberada liberada, List<Questao> questoes) {
    }

    private int gerarAtividades(Ciclo ciclo, TimeEntity time, Metas metas, List<Usuario> membros,
                                int checkinsPlanejados, boolean carlaReservada, Random rnd) {
        List<AtividadeLiberada> escopo = liberadas.escopoDoTime(time.getId(), ciclo.getId());
        Map<Long, List<Questao>> questoesPorLiberada = new HashMap<>();
        escopo.forEach(l -> questoesPorLiberada.put(l.getId(), questoes.daAtividade(l.getAtividade().getId())));

        // A Carla do ciclo 3 tem acerto fixo (seção 8.3): entra antes do sorteio.
        Usuario carla = membros.stream().filter(GeradorDeterministico::ehCarla).findFirst().orElse(null);
        if (carla != null) {
            for (AtividadeFixa fixa : RoteiroCarla.fixasGeradas(ciclo.getNumero())) {
                AtividadeLiberada liberada = noSlot(escopo, fixa.slot());
                if (tentativas.findByUsuarioIdAndAtividadeLiberadaId(carla.getId(), liberada.getId()).isEmpty()) {
                    registrarAtividadeFixa(carla, liberada, questoesPorLiberada.get(liberada.getId()), fixa,
                            momento(ciclo, 30), rnd);
                }
            }
        }

        // O que já existe e o que está reservado.
        Set<String> ocupadas = new HashSet<>();
        int finalizadas = 0;
        long segundosExistentes = 0;
        for (Usuario pessoa : membros) {
            for (AtividadeLiberada liberada : escopo) {
                var tentativa = tentativas.findByUsuarioIdAndAtividadeLiberadaId(pessoa.getId(), liberada.getId());
                if (tentativa.isPresent()) {
                    ocupadas.add(chave(pessoa, liberada));
                    if (tentativa.get().estaFinalizada()) {
                        finalizadas++;
                        segundosExistentes += tentativa.get().duracao().toSeconds();
                    }
                }
            }
        }
        Map<String, Integer> acertosReservados = new HashMap<>();
        Map<String, Integer> respostasReservadas = new HashMap<>();
        int reservadas = 0;
        long segundosReservados = 0;
        if (carlaReservada && carla != null) {
            for (AtividadeFixa fixa : RoteiroCarla.ATIVIDADES_CICLO_4) {
                AtividadeLiberada liberada = noSlot(escopo, fixa.slot());
                if (ocupadas.add(chave(carla, liberada))) {
                    reservadas++;
                    segundosReservados += fixa.segundos();
                    fixa.acertos().forEach((area, n) -> acertosReservados.merge(area, n, Integer::sum));
                    questoesPorLiberada.get(liberada.getId())
                            .forEach(q -> respostasReservadas.merge(q.getArea().getCodigo(), 1, Integer::sum));
                }
            }
        }

        // Quantas tentativas o engajamento pede: concluídas − check-ins.
        int esperadas = membros.size() * (1 + escopo.size());
        int metaDeTentativas = (int) Math.round(metas.engajamento() * esperadas / 100.0) - checkinsPlanejados;
        int aGerar = Math.max(0, metaDeTentativas - finalizadas - reservadas);

        List<Vaga> livres = new ArrayList<>();
        for (Usuario pessoa : membros) {
            for (AtividadeLiberada liberada : escopo) {
                if (!ocupadas.contains(chave(pessoa, liberada))) {
                    livres.add(new Vaga(pessoa, liberada, questoesPorLiberada.get(liberada.getId())));
                }
            }
        }
        Collections.shuffle(livres, rnd);
        List<Vaga> vagas = livres.subList(0, Math.min(aGerar, livres.size()));
        if (vagas.isEmpty()) {
            return 0;
        }

        // Acertos por área: meta do time menos o que já existe e o reservado.
        List<RespostaAvaliada> existentes = coleta.respostas(time.getId(), ciclo.getId());
        Map<String, Integer> respostasPorArea = new HashMap<>(respostasReservadas);
        Map<String, Integer> acertosJaFeitos = new HashMap<>(acertosReservados);
        for (RespostaAvaliada r : existentes) {
            respostasPorArea.merge(r.area(), 1, Integer::sum);
            if (r.correta()) {
                acertosJaFeitos.merge(r.area(), 1, Integer::sum);
            }
        }
        Map<String, List<int[]>> slotsPorArea = new LinkedHashMap<>();
        for (int v = 0; v < vagas.size(); v++) {
            List<Questao> qs = vagas.get(v).questoes();
            for (int q = 0; q < qs.size(); q++) {
                String area = qs.get(q).getArea().getCodigo();
                respostasPorArea.merge(area, 1, Integer::sum);
                slotsPorArea.computeIfAbsent(area, a -> new ArrayList<>()).add(new int[]{v, q});
            }
        }
        Map<String, Integer> metaDeAcertos = metaDeAcertosPorArea(metas, respostasPorArea);

        boolean[][] acerta = new boolean[vagas.size()][];
        for (int v = 0; v < vagas.size(); v++) {
            acerta[v] = new boolean[vagas.get(v).questoes().size()];
        }
        slotsPorArea.forEach((area, slots) -> {
            int faltam = metaDeAcertos.getOrDefault(area, 0) - acertosJaFeitos.getOrDefault(area, 0);
            faltam = Math.max(0, Math.min(slots.size(), faltam));
            Collections.shuffle(slots, rnd);
            for (int k = 0; k < faltam; k++) {
                acerta[slots.get(k)[0]][slots.get(k)[1]] = true;
            }
        });

        // Tempo: a média do time bate com a meta.
        int total = finalizadas + reservadas + vagas.size();
        long restante = Math.round(metas.tempoMin() * 60 * total) - segundosExistentes - segundosReservados;
        long[] segundos = new long[vagas.size()];
        long soma = 0;
        for (int v = 0; v < vagas.size() - 1; v++) {
            segundos[v] = Math.max(180, restante / vagas.size() + rnd.nextInt(121) - 60);
            soma += segundos[v];
        }
        segundos[vagas.size() - 1] = Math.max(180, restante - soma);

        for (int v = 0; v < vagas.size(); v++) {
            Vaga vaga = vagas.get(v);
            gravarTentativa(vaga.pessoa(), vaga.liberada(), vaga.questoes(), acerta[v],
                    momento(ciclo, v), segundos[v], rnd);
        }
        return vagas.size();
    }

    /**
     * Acertos esperados por área. No ciclo 4 vem da tabela de erro por área;
     * nos demais, verificação vem da meta e as outras três áreas se ajustam
     * para o total bater com o técnico. Nos dois casos, formulação, dados e
     * aplicação absorvem o arredondamento do total.
     */
    private static Map<String, Integer> metaDeAcertosPorArea(Metas metas, Map<String, Integer> respostasPorArea) {
        Map<String, Double> acerto = new HashMap<>();
        if (metas.acertoPorArea() != null) {
            acerto.putAll(metas.acertoPorArea());
        } else {
            double outras = (4 * metas.tecnico() - metas.verificacao()) / 3;
            acerto.put("VERIFICACAO", metas.verificacao());
            acerto.put("FORMULACAO", outras + 2);
            acerto.put("DADOS", outras + 1);
            acerto.put("APLICACAO", outras - 3);
        }

        Map<String, Integer> meta = new HashMap<>();
        int totalRespostas = 0;
        for (var e : respostasPorArea.entrySet()) {
            double pct = Math.max(0, Math.min(100, acerto.getOrDefault(e.getKey(), metas.tecnico())));
            meta.put(e.getKey(), (int) Math.round(pct * e.getValue() / 100.0));
            totalRespostas += e.getValue();
        }
        // Ajusta o total para bater com o técnico, sem mexer em verificação
        // (a regra B depende dela). No ciclo 4, cada área fica a no máximo
        // 1,9 ponto da tabela de erro por área.
        double folga = metas.acertoPorArea() != null ? 1.9 : 100;
        int diferenca = (int) Math.round(metas.tecnico() * totalRespostas / 100.0)
                - meta.values().stream().mapToInt(Integer::intValue).sum();
        boolean mexeu = true;
        while (diferenca != 0 && mexeu) {
            mexeu = false;
            for (String area : AREAS_AJUSTAVEIS) {
                if (diferenca == 0 || !meta.containsKey(area)) {
                    continue;
                }
                int respostas = respostasPorArea.get(area);
                double alvo = acerto.getOrDefault(area, metas.tecnico());
                int minimo = Math.max(0, (int) Math.ceil((alvo - folga) * respostas / 100.0));
                int maximo = Math.min(respostas, (int) Math.floor((alvo + folga) * respostas / 100.0));
                int passo = Integer.signum(diferenca);
                int novo = meta.get(area) + passo;
                if (novo >= minimo && novo <= maximo) {
                    meta.put(area, novo);
                    diferenca -= passo;
                    mexeu = true;
                }
            }
        }
        return meta;
    }

    // ------------------------------------------------------------------
    // Gravação
    // ------------------------------------------------------------------

    /**
     * Grava uma atividade com acertos fixos por área (roteiro da Carla). Em
     * cada área, as questões finais são as erradas; assim a questão do prazo de
     * reembolso, a primeira de verificação na 4A, sai certa.
     */
    public void registrarAtividadeFixa(Usuario pessoa, AtividadeLiberada liberada, List<Questao> qs,
                                       AtividadeFixa fixa, Instant inicio, Random rnd) {
        Map<String, Integer> vistasPorArea = new HashMap<>();
        boolean[] acerta = new boolean[qs.size()];
        for (int i = 0; i < qs.size(); i++) {
            String area = qs.get(i).getArea().getCodigo();
            int posicao = vistasPorArea.merge(area, 1, Integer::sum);
            acerta[i] = posicao <= fixa.acertos().getOrDefault(area, 0);
        }
        var existente = tentativas.findByUsuarioIdAndAtividadeLiberadaId(pessoa.getId(), liberada.getId());
        if (existente.isPresent()) {
            // A pessoa abriu a atividade na tela e não terminou: termina pelo roteiro.
            Tentativa aberta = existente.get();
            gravarRespostas(aberta, qs, acerta, fixa.segundos(), rnd);
            aberta.finalizar(aberta.getIniciadaEm().plusSeconds(fixa.segundos()));
            return;
        }
        gravarTentativa(pessoa, liberada, qs, acerta, inicio, fixa.segundos(), rnd);
    }

    private void gravarTentativa(Usuario pessoa, AtividadeLiberada liberada, List<Questao> qs, boolean[] acerta,
                                 Instant inicio, long segundos, Random rnd) {
        Tentativa tentativa = tentativas.save(new Tentativa(pessoa, liberada, inicio));
        gravarRespostas(tentativa, qs, acerta, segundos, rnd);
        tentativa.finalizar(inicio.plusSeconds(segundos));
    }

    private void gravarRespostas(Tentativa tentativa, List<Questao> qs, boolean[] acerta, long segundos,
                                 Random rnd) {
        long porQuestao = segundos / qs.size();
        for (int i = 0; i < qs.size(); i++) {
            Questao q = qs.get(i);
            int alternativas = q.getAlternativas().size();
            int escolhida = acerta[i]
                    ? q.getIndiceCorreto()
                    : (q.getIndiceCorreto() + 1 + rnd.nextInt(alternativas - 1)) % alternativas;
            int tempo = (int) (i == qs.size() - 1 ? segundos - porQuestao * (qs.size() - 1) : porQuestao);
            respostas.save(new Resposta(tentativa, q, escolhida, acerta[i], tempo));
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /**
     * Um instante dentro do ciclo para a {@code i}-ésima resposta. Ciclos que
     * começam hoje ou no futuro (gerados por "Avançar um ciclo") usam horários
     * recentes, para nenhuma resposta ficar com data no futuro.
     */
    Instant momento(Ciclo ciclo, int i) {
        Instant noCiclo = ciclo.getInicio().atTime(9, 0).atZone(FUSO).toInstant()
                .plus(Duration.ofMinutes(17L * (i % 40)));
        Instant limite = Instant.now(clock).minus(Duration.ofHours(1));
        return noCiclo.isBefore(limite) ? noCiclo : limite.minus(Duration.ofMinutes(5L * (40 - i % 40)));
    }

    /** A atividade do escopo na posição A ou B. A trilha ocupa a posição A. */
    static AtividadeLiberada noSlot(List<AtividadeLiberada> escopo, String slot) {
        return escopo.stream()
                .filter(l -> SLOT_B.matcher(l.getAtividade().getCodigo()).matches() == "B".equals(slot))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Escopo sem atividade na posição " + slot));
    }

    static boolean ehCarla(Usuario pessoa) {
        return PessoasDemo.CARLA.equals(pessoa.getNome());
    }

    private static String chave(Usuario pessoa, AtividadeLiberada liberada) {
        return pessoa.getId() + ":" + liberada.getId();
    }

    private static List<Integer> embaralhados(int n, Random rnd) {
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ordem.add(i);
        }
        Collections.shuffle(ordem, rnd);
        return ordem;
    }
}
