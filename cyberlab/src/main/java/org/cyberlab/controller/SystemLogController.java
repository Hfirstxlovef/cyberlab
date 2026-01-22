package org.cyberlab.controller;

import org.cyberlab.entity.SystemLog;
import org.cyberlab.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class SystemLogController {

    @Autowired
    private SystemLogRepository logRepository;

    // ✅ 原始查询接口（不分页）
    @GetMapping
    public List<SystemLog> getLogs(@RequestParam(defaultValue = "") String username,
                                   @RequestParam(defaultValue = "") String operation) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_admin"));

        if (isAdmin) {
            return logRepository.findByUsernameContainingAndOperationContaining(username, operation);
        } else {
            return logRepository.findByUsernameContainingAndOperationContaining(currentUser, operation);
        }
    }

    // ✅ 新增分页接口 /api/logs/page
    @GetMapping("/page")
    public Page<SystemLog> getLogsPage(@RequestParam(defaultValue = "") String username,
                                       @RequestParam(defaultValue = "") String operation,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "timestamp") String sortField,
                                       @RequestParam(defaultValue = "desc") String sortOrder) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_admin"));

        // ✅ 排序构造器
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortField).descending() :
                Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (isAdmin) {
            return logRepository.findByUsernameContainingAndOperationContaining(username, operation, pageable);
        } else {
            return logRepository.findByUsernameContainingAndOperationContaining(currentUser, operation, pageable);
        }
    }

    // ✅ 添加日志
    @PostMapping
    public void addLog(@RequestBody SystemLog log) {
        log.setTimestamp(LocalDateTime.now());
        logRepository.save(log);
    }

    @GetMapping("/test")
    public String testRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_admin"));
        return isAdmin ? "你是管理员" : "你不是管理员";
    }
}