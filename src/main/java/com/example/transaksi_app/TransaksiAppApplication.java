package com.example.transaksi_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TransaksiAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransaksiAppApplication.class, args);
    }

}
