package hello.config.jpa;

import hello.audit.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.userdetails.User;

/**
 * Created by christian on 21/11/16.
 */
@Configuration
@EnableJpaAuditing
class Audit {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }
}
