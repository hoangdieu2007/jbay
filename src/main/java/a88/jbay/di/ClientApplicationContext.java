package a88.jbay.di;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ResponseHandler;
import a88.jbay.client.ServerConnection;
import a88.jbay.controller.ControllerProvider;
import a88.jbay.view.ViewManager;

public class ClientApplicationContext {
    private static ClientApplicationContext instance;
    private final DependencyInjectionContainer container;

    private ClientApplicationContext() {
        this.container = new DependencyInjectionContainer();
    }

    public static synchronized ClientApplicationContext getInstance() {
        if (instance == null) {
            instance = new ClientApplicationContext();
        }
        return instance;
    }

    public void configure() {
        ViewManager viewManager = ViewManager.getInstance();
        ControllerProvider controllerProvider = ControllerProvider.getInstance();

        ClientSession clientSession = new ClientSession();
        container.registerSingleton(ClientSession.class, clientSession);

        ResponseHandler responseHandler = new ResponseHandler(clientSession, controllerProvider, viewManager);
        container.registerSingleton(ResponseHandler.class, responseHandler);

        ServerConnection serverConnection = new ServerConnection(responseHandler, clientSession, viewManager);
        container.registerSingleton(ServerConnection.class, serverConnection);
    }

    public <T> T getDependency(Class<T> type) {
        return container.getInstance(type);
    }

    public DependencyInjectionContainer getContainer() {
        return container;
    }
}
