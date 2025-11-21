package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.AssignmentDtos;
import com.unimag.bustransport.api.dto.TripDtos;
import com.unimag.bustransport.domain.entities.Assignment;
import com.unimag.bustransport.domain.entities.Role;
import com.unimag.bustransport.domain.entities.Trip;
import com.unimag.bustransport.domain.entities.User;
import com.unimag.bustransport.domain.repositories.AssignmentRepository;
import com.unimag.bustransport.domain.repositories.UserRepository;
import com.unimag.bustransport.exception.DuplicateResourceException;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.TripService;
import com.unimag.bustransport.services.mapper.AssignmentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private TripService tripService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private AssignmentMapper assignmentMapper = Mappers.getMapper(AssignmentMapper.class);

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private User givenDriver() {
        return User.builder()
                .id(1L)
                .name("Driver Test")
                .email("driver@example.com")
                .role(Role.ROLE_DRIVER)
                .status(User.Status.ACTIVE)
                .passwordHash("hash")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private User givenDispatcher() {
        return User.builder()
                .id(2L)
                .name("Dispatcher Test")
                .email("dispatcher@example.com")
                .role(Role.ROLE_DISPATCHER)
                .status(User.Status.ACTIVE)
                .passwordHash("hash")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private TripDtos.TripResponse givenTripResponse() {
        TripDtos.TripResponse.RouteSummary routeSummary =
                new TripDtos.TripResponse.RouteSummary(1L, "R001", "Origin", "Destination");

        TripDtos.TripResponse.BusSummary busSummary =
                new TripDtos.TripResponse.BusSummary(1L, "ABC123", 40);

        return new TripDtos.TripResponse(
                1L,
                routeSummary,
                busSummary,
                LocalDate.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                120, // durationMinutes
                null, // overbookingPercent
                "SCHEDULED",
                0, // soldSeats
                40 // availableSeats
        );
    }

    private Assignment givenAssignment(User driver, User dispatcher) {
        Trip trip = Trip.builder()
                .id(1L)
                .date(LocalDate.now())
                .status(Trip.Status.SCHEDULED)
                .build();

        return Assignment.builder()
                .id(1L)
                .trip(trip)
                .driver(driver)
                .dispatcher(dispatcher)
                .checklistOk(false)
                .assignedAt(OffsetDateTime.now())
                .build();
    }

    private AssignmentDtos.AssignmentCreateRequest givenCreateRequest() {
        return new AssignmentDtos.AssignmentCreateRequest(
                1L,
                1L,
                2L,
                false
        );
    }

    private AssignmentDtos.AssignmentUpdateRequest givenUpdateRequest() {
        return new AssignmentDtos.AssignmentUpdateRequest(true);
    }


    @Test
    @DisplayName("Debe crear assignment correctamente")
    void shouldCreateAssignment() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();
        Assignment savedAssignment = givenAssignment(driver, dispatcher);

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(2L)).thenReturn(Optional.of(dispatcher));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(savedAssignment);

        // When
        AssignmentDtos.AssignmentResponse response = assignmentService.createAssignment(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(tripService, times(1)).getTripDetails(1L);
        verify(assignmentRepository, times(1)).findByTripId(1L);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
        verify(assignmentRepository, times(1)).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando assignment ya existe para el trip")
    void shouldThrowExceptionWhenAssignmentAlreadyExists() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();
        Assignment existingAssignment = givenAssignment(driver, dispatcher);

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.of(existingAssignment));

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Assignment already exists");

        verify(assignmentRepository, times(1)).findByTripId(1L);
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando driver no existe")
    void shouldThrowExceptionWhenDriverNotFound() {
        // Given
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Driver with ID 1 not found");

        verify(userRepository, times(1)).findById(1L);
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando driver no tiene rol DRIVER")
    void shouldThrowExceptionWhenDriverHasInvalidRole() {
        // Given
        User invalidDriver = givenDriver();
        invalidDriver.setRole(Role.ROLE_PASSENGER);
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(invalidDriver));

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Invalid Driver");

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando driver no está activo")
    void shouldThrowExceptionWhenDriverNotActive() {
        // Given
        User inactiveDriver = givenDriver();
        inactiveDriver.setStatus(User.Status.INACTIVE);
        User dispatcher = givenDispatcher();
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveDriver));
        when(userRepository.findById(2L)).thenReturn(Optional.of(dispatcher));

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Driver is not active");

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando dispatcher no existe")
    void shouldThrowExceptionWhenDispatcherNotFound() {
        // Given
        User driver = givenDriver();
        TripDtos.TripResponse tripResponse = givenTripResponse();
        AssignmentDtos.AssignmentCreateRequest request = givenCreateRequest();

        when(tripService.getTripDetails(1L)).thenReturn(tripResponse);
        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentService.createAssignment(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Dispatcher with ID 2 not found");

        verify(userRepository, times(1)).findById(2L);
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }


    @Test
    @DisplayName("Debe actualizar assignment correctamente")
    void shouldUpdateAssignment() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        Assignment existingAssignment = givenAssignment(driver, dispatcher);
        AssignmentDtos.AssignmentUpdateRequest request = givenUpdateRequest();

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(existingAssignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(existingAssignment);

        // When
        assignmentService.updateAssignment(1L, request);

        // Then
        verify(assignmentRepository, times(1)).findById(1L);
        verify(assignmentRepository, times(1)).save(existingAssignment);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar assignment inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentAssignment() {
        // Given
        AssignmentDtos.AssignmentUpdateRequest request = givenUpdateRequest();

        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> assignmentService.updateAssignment(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Assignment with ID 999 not found");

        verify(assignmentRepository, times(1)).findById(999L);
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }


    @Test
    @DisplayName("Debe eliminar assignment correctamente")
    void shouldDeleteAssignment() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        Assignment assignment = givenAssignment(driver, dispatcher);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        doNothing().when(assignmentRepository).delete(assignment);

        // When
        assignmentService.deleteAssignment(1L);

        // Then
        verify(assignmentRepository, times(1)).findById(1L);
        verify(assignmentRepository, times(1)).delete(assignment);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar assignment de trip activo")
    void shouldThrowExceptionWhenDeletingAssignmentForActiveTrip() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        Assignment assignment = givenAssignment(driver, dispatcher);
        assignment.getTrip().setStatus(Trip.Status.DEPARTED);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> assignmentService.deleteAssignment(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete Assignment for an active trip");

        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }


    @Test
    @DisplayName("Debe obtener assignment por ID")
    void shouldGetAssignmentById() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        Assignment assignment = givenAssignment(driver, dispatcher);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));

        // When
        AssignmentDtos.AssignmentResponse response = assignmentService.getAssignment(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);

        verify(assignmentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debe obtener assignment por trip ID")
    void shouldGetAssignmentByTripId() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        Assignment assignment = givenAssignment(driver, dispatcher);

        when(assignmentRepository.findByTripId(1L)).thenReturn(Optional.of(assignment));

        // When
        AssignmentDtos.AssignmentResponse response = assignmentService.getAssignmentByTripId(1L);

        // Then
        assertThat(response).isNotNull();
        verify(assignmentRepository, times(1)).findByTripId(1L);
    }

    @Test
    @DisplayName("Debe obtener assignments por driver ID")
    void shouldGetAssignmentsByDriverId() {
        // Given
        User driver = givenDriver();
        User dispatcher = givenDispatcher();
        List<Assignment> assignments = List.of(
                givenAssignment(driver, dispatcher),
                givenAssignment(driver, dispatcher)
        );

        when(assignmentRepository.findByDriver(1L)).thenReturn(assignments);

        // When
        List<AssignmentDtos.AssignmentResponse> result =
                assignmentService.getAssignmentByDriverId(1L);

        // Then
        assertThat(result).hasSize(2);
        verify(assignmentRepository, times(1)).findByDriver(1L);
    }
}