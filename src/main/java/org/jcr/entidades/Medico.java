package org.jcr.entidades;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, of = {"matricula"}) // La igualdad incluye al padre y se basa en la matrícula
public class Medico extends Persona implements Serializable {

    private final Matricula matricula;
    private final EspecialidadMedica especialidad;
    private Departamento departamento; // Getter y Setter generados por Lombok
    private final List<Cita> citas = new ArrayList<>();

    // Getters para matricula y especialidad generados por @Getter


    public Medico(String nombre, String apellido, String dni, LocalDate fechaNacimiento,
                  TipoSangre tipoSangre, String numeroMatricula, EspecialidadMedica especialidad) {
        super(nombre, apellido, dni, fechaNacimiento, tipoSangre);
        this.matricula = new Matricula(numeroMatricula);
        this.especialidad = Objects.requireNonNull(especialidad, "La especialidad no puede ser nula");
    }


    public void addCita(Cita cita) {
        if (cita != null) {
            this.citas.add(cita);
        }
    }


    public List<Cita> getCitas() {
        return Collections.unmodifiableList(new ArrayList<>(citas));
    }


    @Override
    public String toString() {
        return "Medico{" +
                "nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", especialidad=" + especialidad.getDescripcion() +
                ", matricula=" + matricula.getNumero() +
                '}';
    }
}