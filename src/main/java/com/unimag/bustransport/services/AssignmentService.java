package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.AssignmentDtos;

import java.util.List;

public interface AssignmentService {
    AssignmentDtos.AssignmentResponse createAssignment(AssignmentDtos.AssignmentCreateRequest request);
    void updateAssignment(Long assignmentId,AssignmentDtos.AssignmentUpdateRequest request);
    void deleteAssignment(Long assignmentId);
    AssignmentDtos.AssignmentResponse getAssignment(Long assignmentId);
    AssignmentDtos.AssignmentResponse getAssignmentByTripId(Long tripId);
    List<AssignmentDtos.AssignmentResponse> getAssignmentByDriverId(Long driverId);

}
