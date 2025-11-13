package com.unimag.bustransport.repositories;

import com.unimag.bustransport.BusTransportApplication;
import org.springframework.boot.SpringApplication;

class TestBusTransportApplication {
     public static void main(String[] args) {
         SpringApplication.from(BusTransportApplication::main).with(TestcontainersConfiguration.class).run(args);
     }
}
