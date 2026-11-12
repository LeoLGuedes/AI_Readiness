package br.com.prontidaoia.checkin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos check-ins.
 *
 * <p>Todas as buscas daqui recebem o id da própria pessoa. As consultas
 * agregadas por time, para o gestor, ficam no pacote {@code diagnostico}.</p>
 */
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndCicloId(Long usuarioId, Long cicloId);

    Optional<Checkin> findByUsuarioIdAndCicloId(Long usuarioId, Long cicloId);

    List<Checkin> findByUsuarioId(Long usuarioId);
}
