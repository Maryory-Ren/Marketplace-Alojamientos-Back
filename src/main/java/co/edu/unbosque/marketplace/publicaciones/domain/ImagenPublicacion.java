package co.edu.unbosque.marketplace.publicaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImagenPublicacion {

    @Column(nullable = false, length = 500)
    private String ruta;

    @Column(nullable = false)
    private int orden;

    public ImagenPublicacion(String ruta, int orden) {
        this.ruta = ruta;
        this.orden = orden;
    }
}
