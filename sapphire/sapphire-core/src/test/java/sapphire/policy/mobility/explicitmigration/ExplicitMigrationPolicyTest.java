package sapphire.policy.mobility.explicitmigration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import sapphire.common.AppObject;
import sapphire.kernel.common.KernelObjectMigratingException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Malepati Bala Siva Sai Akhil on 23/1/18.
 */

@RunWith(MockitoJUnitRunner.class)
public class ExplicitMigrationPolicyTest {

    public static class ExplicitMigratorTest extends ExplicitMigratorImpl {}

    ExplicitMigrationPolicy.ClientPolicy client;
    ExplicitMigrationPolicy.ServerPolicy server;
    private ExplicitMigratorTest so;
    private AppObject appObject;
    private ArrayList<Object> noParams, oneParam;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        this.client = spy(ExplicitMigrationPolicy.ClientPolicy.class);
        this.server = spy(ExplicitMigrationPolicy.ServerPolicy.class);
        so = new ExplicitMigratorTestStub();
        appObject = new AppObject(so);
        server.$__initialize(appObject);
        this.client.setServer(this.server);
        noParams = new ArrayList<Object>();
        oneParam = new ArrayList<Object>();
    }

    @Test
    public void regularRPC() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        this.client.onRPC(methodName, noParams);
        verify(this.server).onRPC(methodName, noParams);

        // Check that DM methods were not called
        verify(this.server, never()).migrateObject(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
    }

    @Test
    public void retryRegularRPCFromClientTimeoutCaseVerifyException() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        // Mocking the Server Policy such that, would always throw KernelObjectMigratingException onRPC()
        // In order to test the scenario of the exponential backoff retry in this case
        ExplicitMigrationPolicy.ServerPolicy mockServerPolicy = mock(ExplicitMigrationPolicy.ServerPolicy.class);
        when(mockServerPolicy.onRPC(methodName, noParams)).thenThrow(new KernelObjectMigratingException());

        this.client.setServer(mockServerPolicy);

        thrown.expect(KernelObjectMigratingException.class);

        this.client.onRPC(methodName, noParams);
    }

    @Test
    public void retryRegularRPCFromClientTimeoutCaseVerifyRPCCalls() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        // Mocking the Server Policy such that, would always throw KernelObjectMigratingException onRPC()
        // In order to test the scenario of the exponential backoff retry in this case
        ExplicitMigrationPolicy.ServerPolicy mockServerPolicy = mock(ExplicitMigrationPolicy.ServerPolicy.class);
        when(mockServerPolicy.onRPC(methodName, noParams)).thenThrow(new KernelObjectMigratingException());

        this.client.setServer(mockServerPolicy);

        try {
            this.client.onRPC(methodName, noParams);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 7 times, as per the current values which
        // decide the number of exponential backoffs
        verify(mockServerPolicy, times(7)).onRPC(methodName, noParams);

        // Check that DM method i.e migrateObject(...) was not called
        verify(mockServerPolicy, never()).migrateObject((InetSocketAddress)any());
    }

    @Test
    public void retryRegularRPCFromClientSuccessBeforeTimeoutCase() throws Exception {
        String methodName = "public java.lang.String java.lang.Object.toString()";

        // Mocking the Server Policy such that, would throw KernelObjectMigratingException onRPC() first 2 times,
        // then it would handle the onRPC() properly
        // In order to test the scenario where a success happens after a couple of exponential backoff retries
        ExplicitMigrationPolicy.ServerPolicy mockServerPolicy = mock(ExplicitMigrationPolicy.ServerPolicy.class);

        when(mockServerPolicy.onRPC(methodName, noParams)).thenThrow(new KernelObjectMigratingException()).thenThrow(new KernelObjectMigratingException()).thenCallRealMethod();

        this.client.setServer(mockServerPolicy);
        try {
            this.client.onRPC(methodName, noParams);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 3 times, as per the current values which
        // decide the number of exponential backoffs
        verify(mockServerPolicy, times(3)).onRPC(methodName, noParams);

        // Check that DM method i.e migrateObject(...) was not called
        verify(mockServerPolicy, never()).migrateObject((InetSocketAddress)any());
    }

    // ToDo: Mock the oms of KernelServerImpl and getServers() of OMSServer or OMSServerImpl

    // Once getServers() of oms is mocked then following test cases should pass
    // Currently added as ignored test cases which need above mentioned mocking

    @Test @Ignore
    public void basicExplicitMigration() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);

        this.client.onRPC(explicitMigrateObject, oneParam);
        verify(this.server).onRPC(explicitMigrateObject, oneParam);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
    }

    @Test @Ignore
    public void destinationNotFoundExplicitMigrationVerifyException() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr4);

        thrown.expect(NotFoundDestinationKernelServerException.class);
        thrown.expectMessage("The destinations address passed is not present as one of the Kernel Servers");

        this.client.onRPC(explicitMigrateObject, oneParam);
    }

    @Test @Ignore
    public void destinationNotFoundExplicitMigrationVerifyExceptionField() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr4);

        try {
            this.client.onRPC(explicitMigrateObject, oneParam);
        } catch (NotFoundDestinationKernelServerException notFoundDestinationKernelServerException) {
            assertEquals(notFoundDestinationKernelServerException.getNotFoundDestinationAddress(), ExplicitMigrationPolicyTestConstants.kernelServerAddr4);
        }

    }

    @Test @Ignore
    public void destinationNotFoundExplicitMigrationVerifyRPCCalls() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr4);

        this.client.onRPC(explicitMigrateObject, oneParam);
        verify(this.server).onRPC(explicitMigrateObject, oneParam);

        // Check that DM methods were called only once
        verify(this.server, times(1)).migrateObject(ExplicitMigrationPolicyTestConstants.kernelServerAddr4);
    }

    @Test @Ignore
    public void retryMigrateObjectRPCFromClientTimeoutCaseVerifyException() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        thrown.expect(KernelObjectMigratingException.class);

        this.client.onRPC(explicitMigrateObject, oneParam);
    }

    @Test @Ignore
    public void retryMigrateObjectRPCFromClientTimeoutCaseVerifyRPCCalls() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        try {
            this.client.onRPC(explicitMigrateObject, oneParam);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 7 times, as per the current values which
        // decide the number of exponential backoffs
        verify(this.server, times(7)).onRPC(explicitMigrateObject, oneParam);

        // Check that DM method migrateObject(...) was called all the 7 times, the onRPC() was called
        verify(this.server, times(7)).migrateObject(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);
    }

    @Test @Ignore
    public void retryMigrateObjectRPCFromClientSuccessBeforeTimeoutCase() throws Exception {
        String explicitMigrateObject = "public void sapphire.policy.mobility.explicitmigration.ExplicitMigratorImpl.migrateObject() throws java.lang.Exception";

        // After mocking following should be the server list returned from getServers() of OMSServerImpl
        ArrayList<InetSocketAddress> mockedServerList = new ArrayList<InetSocketAddress>();
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr1);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr2);
        mockedServerList.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        // After mocking ExplicitMigrationPolicyTestConstants.localServerAddr should be localServerAddress

        oneParam.add(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);

        try {
            this.client.onRPC(explicitMigrateObject, oneParam);
        } catch (Exception e) {
            // Caught the KernelObjectMigratingException as a user
        }

        // Check that onRPC() of server is called 7 times, as per the current values which
        // decide the number of exponential backoffs
        verify(this.server, times(7)).onRPC(explicitMigrateObject, oneParam);

        // Check that DM method migrateObject(...) was called all the 7 times, the onRPC() was called
        verify(this.server, times(7)).migrateObject(ExplicitMigrationPolicyTestConstants.kernelServerAddr3);
    }
}

// Stub because AppObject expects a stub/subclass of the original class
class ExplicitMigratorTestStub extends ExplicitMigrationPolicyTest.ExplicitMigratorTest implements Serializable {}
