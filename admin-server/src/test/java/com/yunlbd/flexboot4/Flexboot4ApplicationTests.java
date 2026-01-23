package com.yunlbd.flexboot4;

import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.SysDictItem;
import com.yunlbd.flexboot4.entity.SysDictType;
import com.yunlbd.flexboot4.query.SearchDtoUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class Flexboot4ApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void relationFilter_shouldFilterOneToManyCollection() {
		SysDictType type = SysDictType.builder()
				.id("1")
				.code("gender")
				.dictItems(List.of(
						SysDictItem.builder().itemCode("male").build(),
						SysDictItem.builder().itemCode("female").build()
				))
				.build();

		SearchDto dto = SearchDtoUtils.create(
				1,
				10,
				"AND",
				List.of(
						item("sysDictType.code", "eq", "gender"),
						item("sysDictItem.itemCode", "eq", "female")
				),
				List.of()
		);

		SearchDtoUtils.filterRelationCollections(dto, SysDictType.class, List.of(type));
		assertEquals(1, type.getDictItems().size());
		assertEquals("female", type.getDictItems().getFirst().getItemCode());
	}

	private static SearchDto.SearchItem item(String field, String op, Object val) {
		SearchDto.SearchItem it = new SearchDto.SearchItem();
		it.setField(field);
		it.setOp(op);
		it.setVal(val);
		return it;
	}

	@Configuration
	static class TestConfig {
		@Bean
		public JavaMailSender javaMailSender() {
			return null; // Mock bean for tests
		}
	}
}
