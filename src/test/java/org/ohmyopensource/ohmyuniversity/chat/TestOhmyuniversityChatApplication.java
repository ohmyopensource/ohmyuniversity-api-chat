package org.ohmyopensource.ohmyuniversity.chat;

import org.springframework.boot.SpringApplication;

public class TestOhmyuniversityChatApplication {

	public static void main(String[] args) {
		SpringApplication.from(OhmyuniversityChatApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
