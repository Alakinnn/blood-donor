package com.example.blood_donor.server.models;

import java.util.List;

public class PagedResults<T> {
    private final List<T> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;

    public PagedResults(List<T> items, int totalCount, int page, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    // Getters

    public List<T> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }
}
