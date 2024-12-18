package com.example.blood_donor.server.services.interfaces;

import com.example.blood_donor.server.dto.events.EventDetailDTO;
import com.example.blood_donor.server.dto.locations.EventQueryDTO;
import com.example.blood_donor.server.dto.events.EventSummaryDTO;
import com.example.blood_donor.server.models.PagedResults;
import com.example.blood_donor.server.models.response.ApiResponse;

import java.util.List;

public interface IEventService {
    ApiResponse<PagedResults<EventSummaryDTO>> getEventSummaries(EventQueryDTO query);
    ApiResponse<EventDetailDTO> getEventDetails(String eventId, String userId);
}
