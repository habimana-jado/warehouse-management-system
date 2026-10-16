package rw.afriteck.wms.reporting.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReportingProperties.class)
class ReportingConfig {}
