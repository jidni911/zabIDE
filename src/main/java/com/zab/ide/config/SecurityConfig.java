package com.zab.ide.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
// import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.zab.ide.security.AdminPasswordLoader;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        String adminPassword = AdminPasswordLoader.loadPassword();

        auth.inMemoryAuthentication()
                .withUser("zabadmin")
                .password("{noop}" + adminPassword)
                .roles("ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .cors()
                .and()
                .csrf().disable() // important for uploads
                .authorizeRequests()

                // PUBLIC endpoints
                .antMatchers(
                        "/version",
                        "/selfDownload",
                        "/index.html",
                        "/",
                        "/allFiles",
                        "/download/**",
                        "/upload/progress/**")
                .permitAll()

                // PROTECTED endpoints
                .antMatchers(
                        "/upload",
                        "/upload/**")
                .authenticated()

                .anyRequest().permitAll()
                .and()
                .httpBasic(); // simple user/pass auth
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("Content-Range");
        config.addExposedHeader("Accept-Ranges");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
