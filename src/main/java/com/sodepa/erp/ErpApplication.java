package com.sodepa.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Composant applicatif `ErpApplication` du système ERP SODEPA.
 */
@SpringBootApplication
public class ErpApplication {



    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }

}
