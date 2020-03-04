package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public interface OperationsApiService {

    ResponseEntity<List<String>> getOperationActions(String spaceGuid, String operationId);

    ResponseEntity<Void> executeOperationAction(HttpServletRequest request, String spaceGuid, String operationId, String actionId);

    ResponseEntity<List<Operation>> getOperations(String spaceGuid, String mtaId, List<String> states, Integer last);

    ResponseEntity<Operation> getOperation(String spaceGuid, String operationId, String embed);

    ResponseEntity<List<Log>> getOperationLogs(String spaceGuid, String operationId);

    ResponseEntity<String> getOperationLogContent(String spaceGuid, String operationId, String logId);

    ResponseEntity<Operation> startOperation(HttpServletRequest request, String spaceGuid, Operation operation);

}
