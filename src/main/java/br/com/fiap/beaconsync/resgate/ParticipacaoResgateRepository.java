package br.com.fiap.beaconsync.resgate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipacaoResgateRepository extends JpaRepository<ParticipacaoResgate, Long> {

    boolean existsByOcorrenciaIdAndResgatistaId(Long ocorrenciaId, Long resgatistaId);

    boolean existsByOcorrenciaIdAndResgatistaUsuarioId(Long ocorrenciaId, Long usuarioId);
}