package hello.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Created by christian on 21/11/16.
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public String getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return ((User) authentication.getPrincipal()).getUsername();
    }
}
