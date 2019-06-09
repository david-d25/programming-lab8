package ru.david.room.server;

/**
 * Сущность, объединяющая отдельные элементы сервера
 */
public class Hub {
    private ServerConfig config;
    private ServerController controller;
    private Server server;
    private ClientPool clientPool;
    private Logger logger;
    private RequestResolver requestResolver;

    public ServerConfig getConfig() {
        return config;
    }
    public ServerController getController() {
        return controller;
    }
    public Server getServer() {
        return server;
    }
    public ClientPool getClientPool() {
        return clientPool;
    }
    public Logger getLogger() {
        return logger;
    }
    public RequestResolver getRequestResolver() {
        return requestResolver;
    }

    void setConfig(ServerConfig config) {
        this.config = config;
    }
    void setController(ServerController controller) {
        this.controller = controller;
    }
    void setServer(Server server) {
        this.server = server;
    }
    void setClientPool(ClientPool clientPool) {
        this.clientPool = clientPool;
    }
    void setLogger(Logger logger) {
        this.logger = logger;
    }
    void setRequestResolver(RequestResolver requestResolver) {
        this.requestResolver = requestResolver;
    }
}
