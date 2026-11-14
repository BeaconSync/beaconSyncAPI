package br.com.fiap.beaconsync.alerta;

import java.util.List;

public record MapaResponseDTO(
        Long alertaId,
        String titulo,
        StatusAlerta status,
        long totalSemSeguranca,
        List<MapaDistritoDTO> distritos
) {}