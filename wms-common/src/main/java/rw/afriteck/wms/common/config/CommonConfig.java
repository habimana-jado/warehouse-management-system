package rw.afriteck.wms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CommonConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }
}
