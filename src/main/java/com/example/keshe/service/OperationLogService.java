package com.example.keshe.service;

import com.example.keshe.entity.OperationLog;
import java.util.List;

public interface OperationLogService {

    void saveLog(Long deviceId, Long userId, String command, String params);

    void saveLogWithUndo(Long deviceId, Long userId, String command, String params, String undoData);

    OperationLog findById(Long id);

    List<OperationLog> getLogsByDeviceId(Long deviceId);

    List<OperationLog> findAll();
}
