package br.com.fiap.beaconsync.ia;

import java.time.LocalDateTime;
import java.util.List;

public record BoletimResponseDTO(
        Long alertaId,
        String alertaTitulo,
        LocalDateTime geradoEm,
        String origem,
        String texto,
        List<BoletimDistritoDTO> distritos
) {}