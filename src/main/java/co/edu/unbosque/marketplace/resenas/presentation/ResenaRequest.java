package co.edu.unbosque.marketplace.resenas.presentation;
import jakarta.validation.constraints.*;
public record ResenaRequest(@Min(1) @Max(5) int calificacion,@Size(max=1000) String comentario){}
