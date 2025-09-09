package com.kidora.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

// Fully disable data seeding: do not register as a component
// @Component
public class DataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
    // Seeding disabled intentionally (no-op)
    }

}
