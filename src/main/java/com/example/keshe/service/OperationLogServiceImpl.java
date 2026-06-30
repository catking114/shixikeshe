package com.example.keshe.service;

import com.example.keshe.entity.OperationLog;
import com.example.keshe.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogRepository logRepository;

    @Override
    public void saveLog(Long deviceId, Long userId, String command, String params) {
        saveLogWithUndo(deviceId, userId, command, params, null);
    }

    @Override
    public void saveLogWithUndo(Long deviceId, Long userId, String command, String params, String undoData) {
        OperationLog log = new OperationLog();
        log.setDeviceId(deviceId);
        log.setUserId(userId);
        log.setCommand(command);
        log.setParams(params);
        log.setUndoData(undoData);
        log.setOperateTime(LocalDateTime.now());
        logRepository.save(log);
    }

    @Override
    public OperationLog findById(Long id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("操作日志不存在，ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationLog> getLogsByDeviceId(Long deviceId) {
        return logRepository.findByDeviceIdOrderByOperateTimeDesc(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationLog> findAll(Pageable pageable) {
        PageRequest pr = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "operateTime"));
        return logRepository.findAll(pr);
    }
}
