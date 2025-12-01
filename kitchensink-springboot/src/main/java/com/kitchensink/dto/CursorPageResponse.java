package com.kitchensink.dto;

import java.util.List;

public class CursorPageResponse<T> {
    
    private List<T> content;
    private String nextCursor;
    private String previousCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    private int size;
    private long totalElements;
    private int totalPages;
    private int number;
    private String nextScrollId;
    private String prevScrollId;
    
    public CursorPageResponse() {
    }
    
    public CursorPageResponse(List<T> content, String nextCursor, String previousCursor, 
                             boolean hasNext, boolean hasPrevious, int size) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.size = size;
        this.totalElements = content != null ? content.size() : 0;
        this.totalPages = 1;
        this.number = 0;
    }
    
    public CursorPageResponse(List<T> content, String nextCursor, String previousCursor, 
                             boolean hasNext, boolean hasPrevious, int size, long totalElements, int totalPages, int number,
                             String nextScrollId, String prevScrollId) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.previousCursor = previousCursor;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.number = number;
        this.nextScrollId = nextScrollId;
        this.prevScrollId = prevScrollId;
    }
    
    public List<T> getContent() {
        return content;
    }
    
    public void setContent(List<T> content) {
        this.content = content;
    }
    
    public String getNextCursor() {
        return nextCursor;
    }
    
    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
    
    public String getPreviousCursor() {
        return previousCursor;
    }
    
    public void setPreviousCursor(String previousCursor) {
        this.previousCursor = previousCursor;
    }
    
    public boolean isHasNext() {
        return hasNext;
    }
    
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public boolean isHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public int getNumber() {
        return number;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    
    public String getNextScrollId() {
        return nextScrollId;
    }
    
    public void setNextScrollId(String nextScrollId) {
        this.nextScrollId = nextScrollId;
    }
    
    public String getPrevScrollId() {
        return prevScrollId;
    }
    
    public void setPrevScrollId(String prevScrollId) {
        this.prevScrollId = prevScrollId;
    }
}

