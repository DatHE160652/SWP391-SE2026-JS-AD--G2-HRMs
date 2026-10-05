package com.hrms.model;

import java.util.List;

public final class RegistrationRequestPage {
    private final List<RegistrationRequest> items;
    private final long total;
    private final int page;
    private final int size;

    public RegistrationRequestPage(List<RegistrationRequest> items, long total, int page, int size) {
        this.items = List.copyOf(items);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<RegistrationRequest> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalPages() { return Math.max(1, (total + size - 1) / size); }
    public long getFrom() { return items.isEmpty() ? 0 : (long) (page - 1) * size + 1; }
    public long getTo() { return items.isEmpty() ? 0 : getFrom() + items.size() - 1; }
    public boolean getHasPrevious() { return page > 1; }
    public boolean getHasNext() { return page < getTotalPages(); }
}
