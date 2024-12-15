package com.example.blood_donor.services.interfaces;

import com.example.blood_donor.dto.events.EventDetailDTO;
import com.example.blood_donor.dto.locations.EventQueryDTO;
import com.example.blood_donor.dto.events.EventSummaryDTO;
import com.example.blood_donor.models.response.ApiResponse;

import java.util.List;

public interface IEventService {
    ApiResponse<List<EventSummaryDTO>> getEventSummaries(EventQueryDTO query);
    ApiResponse<EventDetailDTO> getEventDetails(String eventId, String userId);
}
