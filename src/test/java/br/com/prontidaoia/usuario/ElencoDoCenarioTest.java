package br.com.prontidaoia.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.simulacao.PessoasDemo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confere o elenco da seção 3: uma gestora, três times de oito pessoas.
 *
 * <p>Também é o teste que garante que a migration V1 subiu e que o JPA valida
 * o esquema: sem isso, a aplicação nem chegaria a semear.</p>
 */
@SpringBootTest
@ActiveProfiles("demo")
// Transacional para que o time (mapeado como LAZY) possa ser lido nas asserções.
@Transactional
@DisplayName("Elenco do cenário simulado")
class ElencoDoCenarioTest extends TesteComBanco {

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private TimeRepository times;

    @Test
    @DisplayName("tem três times, cada um com oito colaboradores")
    void tresTimesDeOito() {
        assertThat(times.findAllByOrderByNome())
                .extracting(TimeEntity::getNome)
                .containsExactly(
                        PessoasDemo.TIME_ATENDIMENTO,
                        PessoasDemo.TIME_FINANCEIRO,
                        PessoasDemo.TIME_MARKETING);

        times.findAllByOrderByNome().forEach(time ->
                assertThat(usuarios.countByTimeId(time.getId()))
                        .as("membros do time %s", time.getNome())
                        .isEqualTo(8));
    }

    @Test
    @DisplayName("Renata é a única gestora e não pertence a time nenhum")
    void gestoraUnicaSemTime() {
        var gestores = usuarios.findByPapelOrderByNome(Papel.GESTOR);

        assertThat(gestores).hasSize(1);
        assertThat(gestores.getFirst().getNome()).isEqualTo(PessoasDemo.GESTORA);
        assertThat(gestores.getFirst().getTime()).isNull();
    }

    @Test
    @DisplayName("Carla é atendente e está no time Atendimento")
    void carlaNoAtendimento() {
        var carla = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow();

        assertThat(carla.getPapel()).isEqualTo(Papel.COLABORADOR);
        assertThat(carla.getTime().getNome()).isEqualTo(PessoasDemo.TIME_ATENDIMENTO);
    }

    @Test
    @DisplayName("o e-mail é montado a partir do nome, sem acentos")
    void emailSemAcento() {
        assertThat(PessoasDemo.email("Renata Guimarães")).isEqualTo("renata.guimaraes@aurora.com.br");
        assertThat(PessoasDemo.email("Marcos Vinícius Lima")).isEqualTo("marcos.lima@aurora.com.br");
    }
}
