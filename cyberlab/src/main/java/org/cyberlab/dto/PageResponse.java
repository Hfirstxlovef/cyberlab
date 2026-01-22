package org.cyberlab.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 通用分页响应DTO
 * 用于替代直接返回Spring Data的Page对象，确保JSON结构稳定性
 *
 * @param <T> 数据类型
 */
public class PageResponse<T> {
    private List<T> content;           // 当前页数据列表
    private int pageNumber;             // 当前页码(从0开始)
    private int pageSize;               // 每页大小
    private long totalElements;         // 总记录数
    private int totalPages;             // 总页数
    private boolean first;              // 是否第一页
    private boolean last;               // 是否最后一页
    private boolean empty;              // 是否空数据

    // 无参构造函数
    public PageResponse() {}

    // 从Spring Data Page对象构造
    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.empty = page.isEmpty();
    }

    // 静态工厂方法：从Page创建PageResponse
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
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

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
