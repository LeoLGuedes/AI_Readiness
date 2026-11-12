package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.config.DemoProperties;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No perfil {@code demo}, deixa a aplicação pronta para usar assim que sobe.
 *
 * <ul>
 *   <li>Semeia o elenco e as atividades, se faltarem.</li>
 *   <li>Com o banco sem ciclos, monta o cenário da seção 8.2 (ciclo 4 aberto).
 *       Com {@code prontidao.demo.restaurar-ao-iniciar} desligado, como nos
 *       testes, abre só o ciclo 1 vazio (decisão D29).</li>
 *   <li>Com ciclos já existentes, não mexe em nada: o cenário só é refeito
 *       pelo botão "Restaurar cenário inicial".</li>
 * </ul>
 */
@Component
@Profile("demo")
public class IniciadorDemo implements ApplicationRunner {

    private final SeedPessoas seedPessoas;
    private final SeedAtividades seedAtividades;
    private final CicloRepository ciclos;
    private final CicloService cicloService;
    private final CenarioInicial cenario;
    private final DemoProperties demo;
    private final Clock clock;

    public IniciadorDemo(SeedPessoas seedPessoas, SeedAtividades seedAtividades, CicloRepository ciclos,
                         CicloService cicloService, CenarioInicial cenario, DemoProperties demo, Clock clock) {
        this.seedPessoas = seedPessoas;
        this.seedAtividades = seedAtividades;
        this.ciclos = ciclos;
        this.cicloService = cicloService;
        this.cenario = cenario;
        this.demo = demo;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPessoas.semear();
        seedAtividades.semear();
        if (ciclos.count() > 0) {
            return;
        }
        if (demo.restaurarAoIniciar()) {
            cenario.restaurar();
        } else {
            cicloService.abrirPrimeiroCiclo(LocalDate.now(clock));
        }
    }
}
