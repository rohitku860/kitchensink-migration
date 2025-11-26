package com.kitchensink.dto;

import java.util.List;

public class CursorPageResponse<T> {
    
    private List<T> content;
    private String nextCursor;
    private String previousCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    private int size;
    
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
}

