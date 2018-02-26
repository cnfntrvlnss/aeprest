package com.zsr.aeprest;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(Application.class, args);
    }
    
    @Bean
    @ConfigurationProperties(prefix="spring.datasource")
    DataSource dataSource() {
    	return new DataSource();
    }
}
