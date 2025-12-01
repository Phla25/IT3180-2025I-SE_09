package BlueMoon.bluemoon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@ComponentScan(basePackages = {"BlueMoon", "BlueMoon.bluemoon"})
public class BluemoonApplication {

	public static void main(String[] args) {
		SpringApplication.run(BluemoonApplication.class, args);
		System.out.println(BCryptPasswordEncoderHelper("AdminPass123"));
		System.out.println(BCryptPasswordEncoderHelper("OffiPass123"));
		System.out.println(BCryptPasswordEncoderHelper("AccPass123"));
		System.out.println(BCryptPasswordEncoderHelper("ResiPass123"));
	}

    private static String BCryptPasswordEncoderHelper(String str) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		return encoder.encode(str);
    }

}
