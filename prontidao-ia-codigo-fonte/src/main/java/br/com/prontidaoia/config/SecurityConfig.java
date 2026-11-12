package br.com.prontidaoia.config;

import br.com.prontidaoia.usuario.Papel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login por formulario com os dois papeis da secao 2.
 *
 * <p>A separacao por prefixo de URL e a primeira camada da regra de
 * privacidade da secao 4: tudo que o gestor ve fica sob {@code /gestao}, e
 * nenhuma rota de colaborador e alcancavel por ele.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/login", "/cadastro", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .requestMatchers("/gestao/**").hasRole(Papel.GESTOR.name())
                        .requestMatchers("/inicio", "/checkin/**", "/atividades/**", "/meu-historico/**", "/perfil", "/perfil/**")
                            .hasRole(Papel.COLABORADOR.name())
                        // /demo serve aos dois papeis e so existe no perfil demo.
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?erro")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .permitAll());

        return http.build();
    }
}
