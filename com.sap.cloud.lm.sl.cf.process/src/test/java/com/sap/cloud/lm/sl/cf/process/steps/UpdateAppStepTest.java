package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationStagingUpdater;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ArgumentMatcherProvider;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateAppStepTest extends AbstractStepTest<UpdateAppStep> {

    private final StepInput input;

    private List<String> notRequiredServices = new ArrayList<>();
    private List<String> expectedServicesToBind = new ArrayList<>();

    private ApplicationStagingUpdater applicationUpdaterMock = Mockito.mock(ApplicationStagingUpdater.class);

    private PlatformType platform;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ContextExtensionDao dao;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "update-app-step-input-1.json", PlatformType.XS2
            },
            {
                "update-app-step-input-2.json", PlatformType.XS2
            },
            {
                "update-app-step-input-3.json", PlatformType.CF
            },
            {
                "update-app-step-input-4.json", PlatformType.XS2
            },
            {
                "update-app-step-input-5.json", PlatformType.CF
            },
            {
                "update-app-step-input-6.json", PlatformType.XS2
            },
            {
                "update-app-step-input-7.json", PlatformType.XS2
            },
            {
                "update-app-step-input-8.json", PlatformType.CF
            },
            {
                "update-app-step-input-9.json", PlatformType.XS2
            },
            {
                "update-app-step-input-10.json", PlatformType.XS2
            },
            {
                "update-app-step-input-11.json", PlatformType.CF
            },
            // Existing app has binding with null parameters and defined service binding is without parameters
            {
                "update-app-step-input-12.json", PlatformType.CF
            },
            // Existing app has binding with empty parameters and defined service binding is without parameters
            {
                "update-app-step-input-13.json", PlatformType.CF
            },
            // Existing app has binding with parameters and defined service binding is without parameters
            {
                "update-app-step-input-14.json", PlatformType.CF
            },
            // Existing app has binding with null parameters and defined service binding is with defined parameters
            {
                "update-app-step-input-15.json", PlatformType.CF
            },
// @formatter:on
        });
    }

    public UpdateAppStepTest(String input, PlatformType platform) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(input, UpdateAppStepTest.class), StepInput.class);
        this.platform = platform;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        validateUnbindServices();
        validateBindServices();
        validateUpdateComponents();
    }

    private void validateUpdateComponents() {
        String appName = input.application.name;
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        if (input.updateStaging && platform == PlatformType.XS2) {
            Mockito.verify(client).updateApplicationStaging(Mockito.eq(appName),
                Mockito.argThat(ArgumentMatcherProvider.getStagingMatcher(cloudApp.getStaging())));
        }
        if (input.updateMemory) {
            Mockito.verify(client).updateApplicationMemory(appName, cloudApp.getMemory());
        }
        if (input.updateDiskQuota) {
            Mockito.verify(client).updateApplicationDiskQuota(appName, cloudApp.getDiskQuota());
        }
        if (input.updateUris) {
            Mockito.verify(client).updateApplicationUris(appName, cloudApp.getUris());
        }
        if (input.updateEnv) {
            Mockito.verify(client).updateApplicationEnv(appName, cloudApp.getEnvAsMap());
        }

        if (platform == PlatformType.CF) {
            Mockito.verify(applicationUpdaterMock).updateApplicationStaging(eq(client), eq(cloudApp.getName()),
                (StagingExtended) Matchers.argThat(ArgumentMatcherProvider.getStagingMatcher(cloudApp.getStaging())));
        }
    }

    private void validateBindServices() {
        Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp().getBindingParameters();
        for (String serviceToBind : expectedServicesToBind) {
            if (currentBindingParameters != null && currentBindingParameters.get(serviceToBind) != null) {
                Mockito.verify(clientExtensions).bindService(input.existingApplication.name, serviceToBind,
                    currentBindingParameters.get(serviceToBind));
            } else {
                Mockito.verify(client).bindService(input.existingApplication.name, serviceToBind);
            }
        }
    }

    private void validateUnbindServices() {
        for (String notRquiredService : notRequiredServices) {
            Mockito.verify(client).unbindService(input.existingApplication.name, notRquiredService);
        }
    }

    private void prepareClient() {
        prepareDiscontinuedServices();

        prepareServicesToBind();

        step.platformTypeSupplier = () -> platform;

        prepareExistingServiceBindings();
    }

    private void prepareExistingServiceBindings() {
        for (String serviceName : input.existingServiceBindings.keySet()) {
            CloudServiceInstance cloudServiceInstance = Mockito.mock(CloudServiceInstance.class);
            List<CloudServiceBinding> serviceBindings = new ArrayList<CloudServiceBinding>();
            for (SimpleBinding simpleBinding : input.existingServiceBindings.get(serviceName)) {
                serviceBindings.add(simpleBinding.toCloudServiceBinding());
            }
            Mockito.when(cloudServiceInstance.getBindings()).thenReturn(serviceBindings);
            Mockito.when(client.getServiceInstance(serviceName)).thenReturn(cloudServiceInstance);
        }
    }

    private List<CloudServiceExtended> mapToCloudServices() {
        return input.application.services.stream().map(serviceName -> mapToCloudService(serviceName)).collect(Collectors.toList());
    }

    private CloudServiceExtended mapToCloudService(String serviceName) {
        return new SimpleService(serviceName).toCloudService();
    }

    private void prepareServicesToBind() {
        for (String service : this.input.application.services) {
            if (!this.input.existingApplication.services.contains(service)) {
                expectedServicesToBind.add(service);
                continue;
            }
            SimpleBinding existingBindingForApplication = getExistingBindingForApplication(service, this.input.existingApplication.name);
            if (existingBindingForApplication == null) {
                expectedServicesToBind.add(service);
                continue;
            }

            Map<String, Map<String, Object>> currentBindingParameters = input.application.toCloudApp().getBindingParameters();

            boolean existingBindingParametersAreEmptyOrNull = existingBindingForApplication.bindingOptions == null
                || existingBindingForApplication.bindingOptions.isEmpty();

            boolean currentBindingParametersAreNull = currentBindingParameters == null || currentBindingParameters.get(service) == null;

            if (!existingBindingParametersAreEmptyOrNull && (currentBindingParametersAreNull
                || !existingBindingForApplication.bindingOptions.equals(currentBindingParameters.get(service)))) {
                expectedServicesToBind.add(service);
                continue;
            }
            if (!currentBindingParametersAreNull
                && !currentBindingParameters.get(service).equals(existingBindingForApplication.bindingOptions)) {
                expectedServicesToBind.add(service);
                continue;
            }
        }
    }

    private void prepareDiscontinuedServices() {
        List<String> discontinuedServices = input.existingApplication.services.stream().filter(
            (service) -> !input.application.services.contains(service)).collect(Collectors.toList());
        notRequiredServices.addAll(discontinuedServices);
    }

    private SimpleBinding getExistingBindingForApplication(String service, String application) {
        for (SimpleBinding simpleBinding : this.input.existingServiceBindings.get(service)) {
            if (application.equals(simpleBinding.applicationName)) {
                return simpleBinding;
            }
        }
        return null;
    }

    private void prepareContext() {
        StepsUtil.setExistingApp(context, input.existingApplication.toCloudApp());
        CloudApplicationExtended cloudApp = input.application.toCloudApp();
        cloudApp.setModuleName("test");
        StepsUtil.setAppsToDeploy(context, Arrays.asList(cloudApp));
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(cloudApp), context);
        StepsUtil.setServicesToBind(context, mapToCloudServices());
        StepsUtil.setTriggeredServiceOperations(context, Collections.emptyMap());
        context.setVariable(Constants.VAR_APPS_INDEX, 0);
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, "dummy");

    }

    private static class StepInput {
        SimpleApplication application;
        SimpleApplication existingApplication;
        Map<String, List<SimpleBinding>> existingServiceBindings;
        boolean updateStaging;
        boolean updateMemory;
        boolean updateDiskQuota;
        boolean updateUris;
        boolean updateEnv;
    }

    private static class SimpleBinding {
        String applicationName;
        Map<String, Object> bindingOptions;

        CloudServiceBinding toCloudServiceBinding() {
            CloudServiceBinding binding = new CloudServiceBinding();
            binding.setAppGuid(NameUtil.getUUID(applicationName));
            binding.setBindingOptions(bindingOptions);
            return binding;
        }
    }

    private static class SimpleApplication {
        String name;
        List<String> services;
        Map<String, Map<String, Object>> bindingParameters;
        String command;
        List<String> uris;
        String buildpackUrl;
        int memory;
        int instances;
        int diskQuota;

        CloudApplicationExtended toCloudApp() {
            CloudApplicationExtended cloudApp = new CloudApplicationExtended(name, command, buildpackUrl, memory, instances, uris, services,
                AppState.STARTED);
            cloudApp.setMeta(new Meta(NameUtil.getUUID(name), null, null));
            cloudApp.setDiskQuota(diskQuota);
            cloudApp.setStaging(new StagingExtended(command, buildpackUrl, null, 0, "none", null));
            cloudApp.setBindingParameters(bindingParameters);
            return cloudApp;
        }

    }

    private static class SimpleService {
        String name;

        SimpleService(String name) {
            this.name = name;
        }

        CloudServiceExtended toCloudService() {
            return new CloudServiceExtended(new Meta(NameUtil.getUUID(name), null, null), name);
        }
    }

    @Override
    protected UpdateAppStep createStep() {
        return new UpdateAppStep();
    }

}
