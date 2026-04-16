package by.bratukhin.shortener;

import org.springframework.boot.SpringApplication;

public class TestDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReactiveUrlShortenerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
