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
import com.unimag.bustransport.services.AssignmentService;
import com.unimag.bustransport.services.TripService;
import com.unimag.bustransport.services.mapper.AssignmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository repository;
    private final AssignmentMapper mapper;
    private final TripService tripService;
    private final UserRepository  userRepository;
    @Override
    public AssignmentDtos.AssignmentResponse createAssignment(AssignmentDtos.AssignmentCreateRequest request) {

        Assignment  assignment = mapper.toEntity(request);
        TripDtos.TripResponse trip = tripService.getTripDetails(assignment.getTrip().getId());
        Optional<Assignment> existingAssignment =repository.findByTripId(trip.id());
        if (existingAssignment.isPresent()) {
            throw new DuplicateResourceException("Assignment already exists");
        }
        User driver = userRepository.findById(assignment.getDriver().getId())
                .orElseThrow(()-> new NotFoundException(String.format("Driver with ID %d not found", assignment.getDriver().getId())));
        if (driver.getRole() != Role.ROLE_DRIVER){
            throw new DuplicateResourceException("Invalid Driver");
        }

        User dispatcher = userRepository.findById(assignment.getDispatcher().getId())
                .orElseThrow(()-> new NotFoundException(String.format("Dispatcher with ID %d not found", assignment.getDispatcher().getId())));
        if (dispatcher.getRole() != Role.ROLE_DISPATCHER){
            throw new DuplicateResourceException("Invalid Dispatcher");
        }
        if (driver.getStatus() != User.Status.ACTIVE){
            throw new RuntimeException("Driver is not active");
        }
        if (dispatcher.getStatus() != User.Status.ACTIVE){
            throw new RuntimeException("Dispatcher is not active");
        }

        assignment.setChecklistOk(false);
        Assignment assignmentSaved = repository.save(assignment);
        log.info("Assignment saved with ID {}", assignmentSaved.getId());
        return mapper.toResponse(assignmentSaved);
    }

    @Override
    public void updateAssignment(Long assignmentId, AssignmentDtos.AssignmentUpdateRequest request) {
        Assignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException(String.format("Assignment with ID %d not found", assignmentId)));
        mapper.updateEntityFromRequest(request, assignment);
        repository.save(assignment);
        log.info("Assignment with ID {} updated", assignmentId);
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = repository.findById(assignmentId)
                .orElseThrow(()-> new NotFoundException(String.format("Assignment with ID %d not found", assignmentId)));
        Trip trip = assignment.getTrip();
        if ( trip.getStatus() == Trip.Status.DEPARTED || trip.getStatus() == Trip.Status.BOARDING ){
            throw new IllegalArgumentException("Cannot delete Assignment for an active trip");
        }
        repository.delete(assignment);
        log.info("Assignment with ID {} deleted", assignmentId);
    }

    @Override
    public AssignmentDtos.AssignmentResponse getAssignment(Long assignmentId) {
        Assignment assignment = repository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException(String.format("Assignment with ID %d not found", assignmentId)));
        return mapper.toResponse(assignment);
    }

    @Override
    public AssignmentDtos.AssignmentResponse getAssignmentByTripId(Long tripId) {
        Assignment assignment = repository.findByTripId(tripId)
                .orElseThrow(() -> new NotFoundException(String.format("Assignment for trip with ID %d not found", tripId)));
        return mapper.toResponse(assignment);
    }

    @Override
    public List<AssignmentDtos.AssignmentResponse> getAssignmentByDriverId(Long driverId) {
        List<Assignment> assignments = repository.findByDriver(driverId);
        return assignments.stream().
                map(mapper::toResponse).toList();
    }
}
