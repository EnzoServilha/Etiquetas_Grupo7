package sptech.school.etiquetas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.dynamic=false",
		"etiqueta.email.destinatario=marcos@example.com"
})
class EtiquetasApplicationTests {

	@Test
	void contextLoads() {
	}

}
