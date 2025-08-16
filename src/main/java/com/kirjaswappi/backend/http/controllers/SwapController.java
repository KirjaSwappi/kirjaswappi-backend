/*
 * Copyright (c) 2024 KirjaSwappi or KirjaSwappi affiliate company. All rights reserved.
 * Author: Mahiuddin Al Kamal <mahiuddinalkamal>
 */
package com.kirjaswappi.backend.http.controllers;

import static com.kirjaswappi.backend.common.utils.Constants.*;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kirjaswappi.backend.http.dtos.requests.CreateSwapRequest;
import com.kirjaswappi.backend.http.dtos.requests.UpdateSwapStatusRequest;
import com.kirjaswappi.backend.http.dtos.responses.SwapRequestResponse;
import com.kirjaswappi.backend.service.SwapService;
import com.kirjaswappi.backend.service.entities.SwapRequest;
import com.kirjaswappi.backend.service.enums.SwapStatus;

@RestController
@RequestMapping(API_BASE + SWAP_REQUESTS)
@Validated
public class SwapController {
  @Autowired
  private SwapService swapService;

  @PostMapping
  @Operation(summary = "Create swap request for a book.", description = "Create swap request for a book.", responses = {
      @ApiResponse(responseCode = "200", description = "Swap request sent.") })
  public ResponseEntity<SwapRequestResponse> createSwapRequest(@Valid @RequestBody CreateSwapRequest request) {
    SwapRequest createdSwapRequest = swapService.createSwapRequest(request.toEntity());
    return ResponseEntity.status(HttpStatus.OK).body(new SwapRequestResponse(createdSwapRequest));
  }

  @DeleteMapping
  @Operation(summary = "Delete all swap requests.", description = "Delete all swap requests.", responses = {
      @ApiResponse(responseCode = "204", description = "Swap requests deleted.") })
  public ResponseEntity<Void> deleteAllSwapRequests() {
    swapService.deleteAllSwapRequests();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PutMapping(ID + STATUS)
  @Operation(summary = "Update swap request status.", description = "Update the status of a swap request. Only the receiver can change the status.", responses = {
      @ApiResponse(responseCode = "200", description = "Swap request status updated successfully."),
      @ApiResponse(responseCode = "400", description = "Invalid status transition or user not authorized."),
      @ApiResponse(responseCode = "404", description = "Swap request not found.") })
  public ResponseEntity<SwapRequestResponse> updateSwapRequestStatus(
      @PathVariable String id,
      @Valid @RequestBody UpdateSwapStatusRequest request,
      @RequestHeader("X-User-Id") String userId) {

    // Validate the request
    request.validate();

    SwapStatus newStatus = SwapStatus.fromCode(request.getStatus());
    SwapRequest updatedSwapRequest = swapService.updateSwapRequestStatus(id, newStatus, userId);

    return ResponseEntity.ok(new SwapRequestResponse(updatedSwapRequest));
  }
}