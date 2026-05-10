package org.ohmyopensource.ohmyuniversity.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OhmyuniversityChatApplicationTests {

	@Test
	void contextLoads() {
	}

}
