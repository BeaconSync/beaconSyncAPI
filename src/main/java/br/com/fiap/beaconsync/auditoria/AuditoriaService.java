package br.com.fiap.beaconsync.auditoria;

import br.com.fiap.beaconsync.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int TAMANHO_MAXIMO_DETALHE = 1000;

    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhe) {
        AuditLog log = new AuditLog();
        if (usuarioId != null) {
            log.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        }
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        if (detalhe != null && detalhe.length() > TAMANHO_MAXIMO_DETALHE) {
            detalhe = detalhe.substring(0, TAMANHO_MAXIMO_DETALHE);
        }
        log.setDetalhe(detalhe);
        auditLogRepository.save(log);
    }
}