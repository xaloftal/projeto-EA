package PSM;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableScheduling
public class CatchItApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatchItApplication.class, args);
    }
}