package com.netpulse;

import com.netpulse.ui.MainView;
import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {

    private MainView mainView;

    @Override
    public void start(Stage primaryStage) {
        mainView = new MainView(primaryStage);
        mainView.build();
    }

    @Override
    public void stop() {
        // Make sure background probe threads and the SQLite connection close
        // cleanly when the window is closed (Week 4: no leaked threads).
        if (mainView != null) {
            mainView.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
