package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.OperationInFinalStateHandler;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("enterTestingPhaseListener")
public class EnterTestingPhaseListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private OperationService operationService;

    @Inject
    public EnterTestingPhaseListener(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        updateOperationState(correlationId);
        getStepLogger().debug(Messages.ENTERING_TESTING_PHASE);

    }

    private void updateOperationState(String processId) {
        Operation operation = operationService.createQuery()
                                              .processId(processId)
                                              .singleResult();
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(Operation.State.ACTION_REQUIRED)
                                      .build();
        operationService.update(operation, operation);
    }

}