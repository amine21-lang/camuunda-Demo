// package com.example.camundademo;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// public class CamundaDemoApplication {

//     public static void main(String[] args) {
//         System.setProperty("polyglot.js.allowAllAccess", "true");
//         SpringApplication.run(CamundaDemoApplication.class, args);
//     }
// }
package com.example.camundademo;

import javax.script.ScriptEngineManager;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CamundaDemoApplication {

    // public static void main(String[] args) {
    //     System.setProperty("polyglot.js.allowAllAccess", "true");
    //     SpringApplication.run(CamundaDemoApplication.class, args);
    // }
    public static void main(String[] args) {
            SpringApplication.run(CamundaDemoApplication.class, args);
    }
    @Bean
    CommandLineRunner scriptEngineCheck() {
        return args -> {
            var mgr = new ScriptEngineManager();
            System.out.println("=== MOTEURS SCRIPT ===");
            mgr.getEngineFactories().forEach(f ->
                System.out.println(f.getEngineName() + " -> " + f.getNames()));
            System.out.println("javascript = " + mgr.getEngineByName("javascript"));
            System.out.println("graal.js   = " + mgr.getEngineByName("graal.js"));
            System.out.println("js         = " + mgr.getEngineByName("js"));

            try {
                var factory = new com.oracle.truffle.js.scriptengine.GraalJSEngineFactory();
                var engine = factory.getScriptEngine();
                System.out.println("Engine = " + engine);
                System.out.println("1+1 = " + engine.eval("1+1"));
            } catch (Throwable t) {
                t.printStackTrace();
            } 
            System.out.println("=== FIN ===");
        };
    }
}