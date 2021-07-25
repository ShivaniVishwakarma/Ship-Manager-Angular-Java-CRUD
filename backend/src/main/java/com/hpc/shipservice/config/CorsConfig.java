package com.hpc.shipservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Shivani Vishwakarma
 * @apiNote enabling cross origin to allow other origins to access rest endpoints
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * @param corsRegistry updating registry to allow all mappings,
     *                 all methods types(get, post, put, delete, patch, options, head)
     *                 and all external domains
     */
    /*
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedMethods("*")
                .allowedOrigins("*");
    }

     */
    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("*")
                .maxAge(3600L)
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true);
    }
}
