package br.com.fiap.beaconsync.resgate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ParticipacaoResgateRepository extends JpaRepository<ParticipacaoResgate, Long> {

    boolean existsByOcorrenciaIdAndResgatistaId(Long ocorrenciaId, Long resgatistaId);

    boolean existsByOcorrenciaIdAndResgatistaUsuarioId(Long ocorrenciaId, Long usuarioId);
    boolean existsByResgatistaUsuarioIdAndOcorrenciaStatusIn(
            Long usuarioId, Collection<StatusOcorrencia> status);
}