package org.jcr.entidades;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j; // Importante: Anotación para el logger

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j // Anotación de Lombok para inyectar un logger
public class CitaManager implements CitaService {
    private final List<Cita> citas = new ArrayList<>();
    private final Map<Paciente, List<Cita>> citasPorPaciente = new ConcurrentHashMap<>();
    private final Map<Medico, List<Cita>> citasPorMedico = new ConcurrentHashMap<>();
    private final Map<Sala, List<Cita>> citasPorSala = new ConcurrentHashMap<>();

    @Override
    public Cita programarCita(@NonNull Paciente paciente, @NonNull Medico medico, @NonNull Sala sala,
                              @NonNull LocalDateTime fechaHora, @NonNull BigDecimal costo) throws CitaException {

        validarCita(fechaHora, costo);

        if (!esMedicoDisponible(medico, fechaHora)) {
            throw new CitaException("El médico no está disponible en la fecha y hora solicitadas.");
        }
        if (!esSalaDisponible(sala, fechaHora)) {
            throw new CitaException("La sala no está disponible en la fecha y hora solicitadas.");
        }
        if (!medico.getEspecialidad().equals(sala.getDepartamento().getEspecialidad())) {
            throw new CitaException("La especialidad del médico no coincide con el departamento de la sala.");
        }

        Cita cita = new Cita(paciente, medico, sala, fechaHora, costo);
        citas.add(cita);

        // Los métodos de actualización ahora son mucho más simples
        actualizarIndicePaciente(paciente, cita);
        actualizarIndiceMedico(medico, cita);
        actualizarIndiceSala(sala, cita);

        paciente.addCita(cita);
        medico.addCita(cita);
        sala.addCita(cita);

        log.info("Cita programada exitosamente para el paciente {}", paciente.getDni());
        return cita;
    }

    private void validarCita(LocalDateTime fechaHora, BigDecimal costo) throws CitaException {
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new CitaException("No se puede programar una cita en el pasado.");
        }
        if (costo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CitaException("El costo debe ser mayor que cero.");
        }
    }

    private boolean esMedicoDisponible(Medico medico, LocalDateTime fechaHora) {
        // Usamos streams para una lógica más clara y concisa
        return citasPorMedico.getOrDefault(medico, Collections.emptyList()).stream()
                .noneMatch(citaExistente ->
                        Math.abs(citaExistente.getFechaHora().compareTo(fechaHora)) < 2); // 2 horas de diferencia
    }

    private boolean esSalaDisponible(Sala sala, LocalDateTime fechaHora) {
        // Usamos streams para una lógica más clara y concisa
        return citasPorSala.getOrDefault(sala, Collections.emptyList()).stream()
                .noneMatch(citaExistente ->
                        Math.abs(citaExistente.getFechaHora().compareTo(fechaHora)) < 2); // 2 horas de diferencia
    }

    // --- MÉTODOS SIMPLIFICADOS CON JAVA 8+ ---

    private void actualizarIndicePaciente(Paciente paciente, Cita cita) {
        citasPorPaciente.computeIfAbsent(paciente, k -> new ArrayList<>()).add(cita);
    }

    private void actualizarIndiceMedico(Medico medico, Cita cita) {
        citasPorMedico.computeIfAbsent(medico, k -> new ArrayList<>()).add(cita);
    }

    private void actualizarIndiceSala(Sala sala, Cita cita) {
        citasPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(cita);
    }

    @Override
    public List<Cita> getCitasPorPaciente(Paciente paciente) {
        return Collections.unmodifiableList(
                citasPorPaciente.getOrDefault(paciente, Collections.emptyList())
        );
    }

    @Override
    public List<Cita> getCitasPorMedico(Medico medico) {
        return Collections.unmodifiableList(
                citasPorMedico.getOrDefault(medico, Collections.emptyList())
        );
    }

    @Override
    public List<Cita> getCitasPorSala(Sala sala) {
        return Collections.unmodifiableList(
                citasPorSala.getOrDefault(sala, Collections.emptyList())
        );
    }

    // --- Métodos de guardado y carga ---

    @Override
    public void guardarCitas(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (Cita cita : citas) {
                writer.println(cita.toCsvString());
            }
        }
    }

    @Override
    public void cargarCitas(String filename, Map<String, Paciente> pacientes,
                            Map<String, Medico> medicos, Map<String, Sala> salas)
            throws IOException, CitaException {
        citas.clear();
        citasPorPaciente.clear();
        citasPorMedico.clear();
        citasPorSala.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Cita cita = Cita.fromCsvString(line, pacientes, medicos, salas);
                    citas.add(cita);
                    actualizarIndicePaciente(cita.getPaciente(), cita);
                    actualizarIndiceMedico(cita.getMedico(), cita);
                    actualizarIndiceSala(cita.getSala(), cita);
                } catch (CitaException e) {
                    // Usando el logger inyectado por Lombok en lugar de System.err
                    log.error("Error al cargar cita desde CSV: {} - {}", line, e.getMessage());
                    throw e;
                }
            }
        }
    }
}