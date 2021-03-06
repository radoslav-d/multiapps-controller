package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class EnvironmentApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public EnvironmentApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy) {
        super(context, updateStrategy);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Map<String, String> env = application.getEnv();
        Map<String, String> existingEnv = existingApplication.getEnv();
        return !existingEnv.equals(env);
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Map<String, String> env = applyUpdateStrategy(existingApplication.getEnv(), application.getEnv());
        getControllerClient().updateApplicationEnv(application.getName(), env);
    }

    private Map<String, String> applyUpdateStrategy(Map<String, String> existingEnv, Map<String, String> env) {
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_TO_ENV, updateStrategy);
        return getElementUpdater().updateMap(existingEnv, env);
    }

}
