package br.com.fiap.beaconsync.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

@Component
public class ImagemPerfil {
    public byte[] normalizar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw invalida();
        if (arquivo.getSize() > 2 * 1024 * 1024)
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 2 MiB.");
        try (var input =
                     ImageIO.createImageInputStream(new ByteArrayInputStream(arquivo.getBytes()))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalida();
            ImageReader reader = readers.next();
            try {
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png")) throw invalida();
                reader.setInput(input, true, true);
                int largura = reader.getWidth(0), altura = reader.getHeight(0);
                if (largura <= 0 || altura <= 0 || (long) largura * altura > 12_000_000)
                    throw invalida();
                BufferedImage original = reader.read(0);
                double escala = Math.min(1.0, 1024.0 / Math.max(largura, altura));
                BufferedImage normalizada =
                        new BufferedImage(
                                Math.max(1, (int) (largura * escala)),
                                Math.max(1, (int) (altura * escala)),
                                BufferedImage.TYPE_INT_RGB);
                Graphics2D g = normalizada.createGraphics();
                try {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, normalizada.getWidth(), normalizada.getHeight());
                    g.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(
                            original, 0, 0, normalizada.getWidth(), normalizada.getHeight(), null);
                } finally {
                    g.dispose();
                }
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(normalizada, "jpeg", output)) throw invalida();
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw invalida();
        }
    }

    private ResponseStatusException invalida() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Envie JPEG ou PNG válido, com até 12 milhões de pixels.");
    }
}