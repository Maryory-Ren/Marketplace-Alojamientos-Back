package co.edu.unbosque.marketplace.publicaciones.presentation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RechazoPublicacionRequest(@NotBlank @Size(min=10,max=500) String motivo) {}
