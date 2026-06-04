package com.enrollment.system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class EnrollmentSystemApplication extends Application {
    
    private ConfigurableApplicationContext applicationContext;
    
    @Override
    public void init() {
        // Initialize Spring context
        applicationContext = new SpringApplicationBuilder(SpringBootApp.class).run();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML with Spring
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        
        Parent root = loader.load();
        Scene scene = new Scene(root);
        
        primaryStage.setTitle("Kinderscape - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        // Close Spring context
        applicationContext.close();
    }

}