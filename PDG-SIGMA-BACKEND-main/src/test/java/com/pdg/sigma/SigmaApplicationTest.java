package com.pdg.sigma;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SigmaApplicationTest {

    @Test
    void main_startsSpringApplication() {
        try (MockedStatic<SpringApplication> springApp = Mockito.mockStatic(SpringApplication.class)) {
            SigmaApplication.main(new String[]{});
            springApp.verify(() -> SpringApplication.run(SigmaApplication.class, new String[]{}));
        }
    }
}
