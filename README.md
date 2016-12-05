**Simple project to add a dash of security of spring REST JPA**

First we checkout the spring REST JPA at repository [https://github.com/spring-guides/gs-accessing-data-rest.git](https://github.com/spring-guides/gs-accessing-data-rest.git)
in that example we expose REST endpoint in HATEOAS format thank to Spring functionality, 
but we have no control on which user can handle the data,
so everyone can basically do everything and we want that only the creator of record can edit and delete the record itself.

So first we creaate a Security config:
`hello.config.security.WebSecurityConfig`

```java
package hello.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;



/**
 * Created by christian on 21/11/16.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/", "/home").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .permitAll();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .withUser("user").password("password").roles("USER")
                .and()
                .withUser("user2").password("password2").roles("USER");
    }


}

```
with that we have defined two new users "user" and "user2" and ensecured that the endpoint exposed by RESTRepository are secured by Security itself.
After that we rely on JPA features and we define a new abstract model that all other models inherits.

`hello.data.AbsModel`

```java
package hello.data;


import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import javax.annotation.PreDestroy;
import javax.persistence.*;
import javax.validation.ValidationException;

/**
 * Created by christian on 21/11/16.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbsModel<T extends PagingAndSortingRepository> extends AbstractPersistable<Long>{


    @CreatedBy
    private String owner;

    @LastModifiedBy
    private String modifier;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long modifiedAt;

    @Transient
    private String storedOwner;



    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        //this.owner =  owner != null ? owner : this.owner;

        this.owner = owner;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }



    @PostLoad
    public void onPostLoad() {
        this.storedOwner = this.owner;
    }



    @PreUpdate
    public void onPreUpdate() {
        if(!this.storedOwner.equals(this.getModifier())) {
            throw new ValidationException("Trying to modify a data not mine");
        }
    }

    //getCurrentAuditor is not called for delete
    @PreRemove
    public void onPreRemove() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !this.owner.equalsIgnoreCase(((User) authentication.getPrincipal()).getUsername())  ) {
            throw new ValidationException("Trying to delete a data not mine");
        }
    }


}


```

As you can see we define two method:
onPreUpdate: triggered before a modification
onPreRemove: triggered before a deletion
with onPreUpdate we can check if storedOwner (populated by @PostLoad method) is the same of currentModifier.
We need to rely on PostLoad method becuse in actual implementation of jpa during an update only modfifier field are handled, so getOwner is still null.
with onPreRemove jpa doesn't need to populate audit fields so we have to rely on current authentication;
If test doesn't pass, ( user is not the owner of the data ) we throw a ValidationException

**How to configure JPA Auditing**
Configuring JPA auditing in SpringBoot i svery easy, first we need a config class
`hello.config.jpa.Audit`

```java
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
```
where bsically we define an AuditorAware of String, that is the class that define data format to be passed in audited fields.
`hello.audit.SpringSecurityAuditorAware`

```java
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

```

I added two tests to check the correct behaviour of application:
```java
@Test
	public void shouldNotUpdateEntity() throws Exception {

		MvcResult mvcResult = mockMvc.perform(auth2(post("/people")).content(
				"{\"firstName\": \"Frodo\", \"lastName\":\"Baggins\"}")).andExpect(
				status().isCreated()).andReturn();

		String location = mvcResult.getResponse().getHeader("Location");

		mockMvc.perform(auth(put(location)).content(
				"{\"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
				status().isForbidden());

		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
				jsonPath("$.firstName").value("Frodo")).andExpect(
				jsonPath("$.lastName").value("Baggins"));
	}


	@Test
    	public void shouldNotDeleteEntity() throws Exception {

    		MvcResult mvcResult = mockMvc.perform(auth2(post("/people")).content(
    				"{ \"firstName\": \"Bilbo\", \"lastName\":\"Baggins\"}")).andExpect(
    				status().isCreated()).andReturn();

    		String location = mvcResult.getResponse().getHeader("Location");

    		mockMvc.perform(auth(delete(location))).andExpect(status().isForbidden());

    		mockMvc.perform(auth(get(location))).andExpect(status().isOk()).andExpect(
    				jsonPath("$.firstName").value("Bilbo")).andExpect(
    				jsonPath("$.lastName").value("Baggins"));

    	}
```
