module id.my.fernando.simplewebserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires jdk.httpserver;
    requires java.datatransfer;
    requires java.desktop;
    requires java.prefs;

    opens id.my.fernando.simplewebserver to javafx.fxml;
    exports id.my.fernando.simplewebserver;
}