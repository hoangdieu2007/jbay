@SuppressWarnings("module")
module a88.jbay {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires annotations;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires java.desktop;
    requires org.slf4j;

    opens a88.jbay.view to javafx.fxml, javafx.graphics;
    exports a88.jbay.view;
    exports a88.jbay.controller.client;
    opens a88.jbay.controller.client to javafx.fxml, javafx.graphics;
    exports a88.jbay.server;
    opens a88.jbay.server to javafx.fxml, javafx.graphics;
    exports a88.jbay.system;
    opens a88.jbay.system to javafx.fxml, javafx.graphics;
    exports a88.jbay.common.network;
    opens a88.jbay.common.network to javafx.fxml, javafx.graphics;
    exports a88.jbay.dao;
    opens a88.jbay.dao to javafx.fxml, javafx.graphics;
    exports a88.jbay.client;
    opens a88.jbay.client to javafx.fxml, javafx.graphics;
    exports a88.jbay.system.update;
    opens a88.jbay.system.update to javafx.fxml, javafx.graphics;
    exports a88.jbay.system.user;
    opens a88.jbay.system.user to javafx.fxml, javafx.graphics;
    exports a88.jbay.common.auction;
    opens a88.jbay.common.auction to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.client.AdminController;
    opens a88.jbay.controller.client.AdminController to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.client.AuctionController;
    opens a88.jbay.controller.client.AuctionController to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.client.EntranceController;
    opens a88.jbay.controller.client.EntranceController to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.client.ServerController;
    opens a88.jbay.controller.client.ServerController to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.client.UserHomeScreenController;
    opens a88.jbay.controller.client.UserHomeScreenController to javafx.fxml, javafx.graphics;
}