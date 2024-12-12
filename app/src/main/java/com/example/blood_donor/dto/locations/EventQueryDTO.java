package com.example.blood_donor.dto.locations;

import java.util.List;

public class EventQueryDTO {
    private Double latitude;
    private Double longitude;
    private Double zoomLevel;
    private String searchTerm;
    private List<String> bloodTypes;
    private String sortBy;
    private String sortOrder;
    private Integer page;
    private Integer pageSize;

    // Update constructor to match these fields exactly
    public EventQueryDTO(
            Double latitude,
            Double longitude,
            Double zoomLevel,
            String searchTerm,
            List<String> bloodTypes,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer pageSize
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoomLevel = zoomLevel;
        this.searchTerm = searchTerm;
        this.bloodTypes = bloodTypes;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.page = page;
        this.pageSize = pageSize;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(Double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public List<String> getBloodTypes() {
        return bloodTypes;
    }

    public void setBloodTypes(List<String> bloodTypes) {
        this.bloodTypes = bloodTypes;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

