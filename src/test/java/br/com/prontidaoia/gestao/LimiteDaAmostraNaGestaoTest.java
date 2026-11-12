package br.com.prontidaoia.gestao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.checkin.CheckinService;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.checkin.RespostaCheckin;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.config.LimiaresProperties;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.gestao.PrivacidadeGuard.ContagemDoTime;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioDetailsService;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Melhoria M1 (PBL 8): o limite exato da regra de privacidade, de ponta a
 * ponta. O mesmo time, no mesmo ciclo, com as mesmas respostas; muda só o
 * número de check-ins, 4 ou 5.
 *
 * <p>Antes desta melhoria só o lado "4 ocultam" era testado na tela
 * ({@code TelasDaGestaoTest.privacidadeSemAmostra}). Faltava provar que o
 * quinto check-in libera os dados, e que os dois lados valem nas três
 * camadas que o gestor alcança: a fotografia gravada no fechamento, o
 * {@link PrivacidadeGuard} e a página de detalhe do time.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
@DisplayName("Limite exato da amostra na gestão: 4 check-ins ocultam, 5 liberam")
class LimiteDaAmostraNaGestaoTest extends TesteComBanco {

    private static final String DIFICULDADE = "Receio de substituição";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioDetailsService detalhes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TimeRepository times;
    @Autowired private CheckinService checkins;
    @Autowired private DificuldadeRepository dificuldades;
    @Autowired private FechamentoCicloService fechamento;
    @Autowired private CicloService ciclos;
    @Autowired private IndicadorRepository indicadores;
    @Autowired private PrivacidadeGuard privacidade;
    @Autowired private LimiaresProperties limiares;

    /**
     * Ciclo 1 vazio. {@code n} pessoas do Marketing (8 membros) respondem o
     * check-in, todas marcando a mesma dificuldade; depois o ciclo é fechado.
     * Devolve a fotografia do Marketing no ciclo 1.
     */
    private IndicadorTimeCiclo marketingComCheckins(int n) {
        Long marketing = times.findByNome(PessoasDemo.TIME_MARKETING).orElseThrow().getId();
        List<Usuario> membros = usuarios.findByTimeIdOrderByNome(marketing);
        Long receio = dificuldades.findAllByOrderById().getFirst().getId();
        Long ciclo = ciclos.cicloAberto().orElseThrow().getId();
        for (int i = 0; i < n; i++) {
            checkins.registrar(membros.get(i).getId(), new RespostaCheckin(4, 2, List.of(receio), false));
        }
        fechamento.fecharCicloAberto();
        return indicadores.findByTimeIdAndCicloId(marketing, ciclo).orElseThrow();
    }

    private String detalheDoTime(IndicadorTimeCiclo indicador) throws Exception {
        return mockMvc.perform(get("/gestao/times/" + indicador.getTime().getId())
                        .with(user(detalhes.loadUserByUsername(PessoasDemo.email(PessoasDemo.GESTORA)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("o mínimo configurado é 5, o valor da seção 5.1")
    void minimoConfigurado() {
        // Se alguém mudar o application.yml, os números destes testes perdem o sentido.
        assertThat(limiares.amostraMinimaCheckins()).isEqualTo(5);
    }

    @Test
    @DisplayName("4 check-ins: fotografia sem confiança, guarda fechado e tela com aviso")
    void quatroCheckinsOcultam() throws Exception {
        IndicadorTimeCiclo quatro = marketingComCheckins(4);

        // 1. Fotografia gravada no fechamento.
        assertThat(quatro.getNCheckins()).isEqualTo(4);
        assertThat(quatro.getConfianca()).isNull();
        assertThat(quatro.getResistencia()).isNull();

        // 2. Única porta da gestão para os dados de check-in.
        assertThat(privacidade.temAmostra(quatro)).isFalse();
        assertThat(privacidade.dificuldadesDoTime(quatro)).isEmpty();

        // 3. Página que a Renata abre.
        String pagina = detalheDoTime(quatro);
        assertThat(pagina).contains("Amostra insuficiente: 4 de 8 check-ins, mínimo de 5");
        assertThat(pagina).doesNotContain(DIFICULDADE);
    }

    @Test
    @DisplayName("5 check-ins: fotografia com confiança, guarda aberto e tela com as dificuldades")
    void cincoCheckinsLiberam() throws Exception {
        IndicadorTimeCiclo cinco = marketingComCheckins(5);

        // 1. Fotografia gravada no fechamento: (4 − 1) / 4 × 100 = 75; (2 − 1) / 4 × 100 = 25.
        assertThat(cinco.getNCheckins()).isEqualTo(5);
        assertThat(cinco.getConfianca()).isEqualByComparingTo("75.0");
        assertThat(cinco.getResistencia()).isEqualByComparingTo("25.0");

        // 2. O guarda libera só contagens do time inteiro: 5 de 5 marcaram a dificuldade.
        assertThat(privacidade.temAmostra(cinco)).isTrue();
        Optional<List<ContagemDoTime>> contagens = privacidade.dificuldadesDoTime(cinco);
        assertThat(contagens).isPresent();
        assertThat(contagens.get())
                .filteredOn(c -> c.dificuldade().equals(DIFICULDADE))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.vezes()).isEqualTo(5);
                    assertThat(c.percentual()).isEqualTo(100);
                });

        // 3. Página que a Renata abre.
        String pagina = detalheDoTime(cinco);
        assertThat(pagina).contains("5 de 8 check-ins respondidos (mínimo de 5)");
        assertThat(pagina).contains(DIFICULDADE);
        assertThat(pagina).doesNotContain("Amostra insuficiente");
    }
}
