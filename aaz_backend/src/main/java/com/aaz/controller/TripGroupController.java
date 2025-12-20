package com.aaz.controller;

import com.aaz.dto.*;
import com.aaz.security.services.UserDetailsImpl;
import com.aaz.service.TripGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TripGroupController {

    private final TripGroupService tripGroupService;

    @GetMapping
    public ResponseEntity<List<TripGroupResponse>> getMyTrips(Authentication auth) {

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();
        return ResponseEntity.ok(tripGroupService.getUserTripGroups(userId));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripGroupResponse> getTripById(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripGroupService.getTripGroupById(tripId));
    }

    @PostMapping
    public ResponseEntity<TripGroupResponse> createTrip(
            @RequestBody TripGroupRequest request,
            Authentication auth) {
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();
        return ResponseEntity.ok(tripGroupService.createTripGroup(request, userId));
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<TripGroupResponse> updateTrip(
            @PathVariable Long tripId,
            @RequestBody TripGroupRequest request) {
        return ResponseEntity.ok(tripGroupService.updateTripGroup(tripId, request));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long tripId,
            Authentication auth) {
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();
        tripGroupService.deleteTripGroup(tripId, userId);
        return ResponseEntity.noContent().build();
    }
}