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

    opens a88.jbay.view to javafx.fxml, javafx.graphics;
    exports a88.jbay.view;
    exports a88.jbay.controller.client;
    opens a88.jbay.controller.client to javafx.fxml, javafx.graphics;
    exports a88.jbay.controller.server;
    opens a88.jbay.controller.server to javafx.fxml, javafx.graphics;
    exports a88.jbay.dao;
    opens a88.jbay.dao to javafx.fxml, javafx.graphics;
}