package br.com.fiap.beaconsync.pessoa;

import br.com.fiap.beaconsync.auth.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MinhasSolicitacoesService {
    private final AuthorizationService authorizationService;
    private final MinhasSolicitacoesRepository repository;

    @Transactional(readOnly = true)
    public Page<MinhaSolicitacaoVinculoDTO> consultar(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe page a partir de zero e size entre 1 e 100.");
        }
        Long usuarioId = authorizationService.usuarioLogadoId();
        return repository.consultar(usuarioId, PageRequest.of(page, size))
                .map(item -> new MinhaSolicitacaoVinculoDTO(item.getId(), item.getParentesco(),
                        StatusVinculo.valueOf(item.getStatus()), item.getCriadaEm(), item.getDecididaEm()));
    }
}
