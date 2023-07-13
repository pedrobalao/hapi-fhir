package ca.uhn.fhir.jpa.fql.parser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
@SuppressWarnings("SqlDialectInspection")
public class HfqlStatementParserTest {

	private static final FhirContext ourCtx = FhirContext.forR4Cached();

	@Test
	public void testCountAndGroup() {
		String input = """
   		select Count(*), name.given, name.family
   		from Patient
   		group by name.given, name.family
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(3, statement.getSelectClauses().size());
		assertEquals("*", statement.getSelectClauses().get(0).getClause());
		assertEquals(HfqlStatement.SelectClauseOperator.COUNT, statement.getSelectClauses().get(0).getOperator());
		assertEquals("Count(*)", statement.getSelectClauses().get(0).getAlias());
		assertEquals("name.given", statement.getSelectClauses().get(1).getClause());
		assertEquals(HfqlStatement.SelectClauseOperator.SELECT, statement.getSelectClauses().get(1).getOperator());
		assertEquals("name.family", statement.getSelectClauses().get(2).getClause());
		assertEquals(HfqlStatement.SelectClauseOperator.SELECT, statement.getSelectClauses().get(2).getOperator());
		assertEquals(2, statement.getGroupByClauses().size());
		assertThat(statement.getGroupByClauses(), contains("name.given", "name.family"));

	}


	@Test
	public void testFromSelect() {
		String input = """
					from Patient
					select
					   name.given[0],
					   name.family
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(2, statement.getSelectClauses().size());
		assertEquals("name.given[0]", statement.getSelectClauses().get(0).getClause());
		assertEquals("name.given[0]", statement.getSelectClauses().get(0).getAlias());
		assertEquals(HfqlStatement.SelectClauseOperator.SELECT, statement.getSelectClauses().get(0).getOperator());
		assertEquals("name.family", statement.getSelectClauses().get(1).getClause());
		assertEquals("name.family", statement.getSelectClauses().get(1).getAlias());
	}

	@Test
	public void testSelect_ValueWithPrefix() {
		String input = """
					select id
					from Observation
					where value-quantity = 'lt500'
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Observation", statement.getFromResourceName());
		assertEquals(1, statement.getWhereClauses().size());
		assertEquals("value-quantity", statement.getWhereClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getWhereClauses().get(0).getOperator());
		assertThat(statement.getWhereClauses().get(0).getRight(), contains("'lt500'"));

	}

	@Test
	public void testSelectFrom() {
		String input = """
					select
					   name.given[0],
					   name.family
					from Patient
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(2, statement.getSelectClauses().size());
		assertEquals("name.given[0]", statement.getSelectClauses().get(0).getClause());
		assertEquals("name.given[0]", statement.getSelectClauses().get(0).getAlias());
		assertEquals("name.family", statement.getSelectClauses().get(1).getClause());
		assertEquals("name.family", statement.getSelectClauses().get(1).getAlias());
	}

	@Test
	public void testSelectComplexFhirPath_StringConcat() {
		String input = """
					SELECT FullName: Patient.name.given + ' ' + Patient.name.family
					FROM Patient
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(1, statement.getSelectClauses().size());
		assertEquals("Patient.name.given + ' ' + Patient.name.family", statement.getSelectClauses().get(0).getClause());
		assertEquals("FullName", statement.getSelectClauses().get(0).getAlias());

	}

	@Test
	public void testSelectAs() {
		String input = """
					SELECT Patient.name.given + ' ' + Patient.name.family as FullName
					FROM Patient
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(1, statement.getSelectClauses().size());
		assertEquals("Patient.name.given + ' ' + Patient.name.family", statement.getSelectClauses().get(0).getClause());
		assertEquals("FullName", statement.getSelectClauses().get(0).getAlias());

	}

	@Test
	public void testSelectWhere_GreaterThan() {
		String input = """
					select id
					from Observation
					having
					   value.ofType(Quantity).value > 100
			""";

		HfqlStatement statement = parse(input);
		assertEquals(1, statement.getHavingClauses().size());
		assertEquals("value.ofType(Quantity).value > 100", statement.getHavingClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.UNARY_BOOLEAN, statement.getHavingClauses().get(0).getOperator());
		assertEquals(0, statement.getHavingClauses().get(0).getRight().size());
	}

	@Test
	public void testSelectOrderBy() {
		String input = """
					select id, name.family
					from Observation
					order by name.family, count(*)
			""";

		HfqlStatement statement = parse(input);
		assertThat(statement.getSelectClauses().stream().map(t->t.getAlias()).collect(Collectors.toList()), contains(
			"id", "name.family"
		));
		assertEquals(2, statement.getOrderByClauses().size());
		assertEquals("name.family", statement.getOrderByClauses().get(0).getClause());
		assertTrue(statement.getOrderByClauses().get(0).isAscending());
		assertEquals("count(*)", statement.getOrderByClauses().get(1).getClause());
		assertTrue(statement.getOrderByClauses().get(1).isAscending());
	}

	@Test
	public void testSelectOrderBy_Directional() {
		String input = """
					select id, name.family
					from Observation
					order by name.family DESC, id ASC
			""";

		HfqlStatement statement = parse(input);
		assertThat(statement.getSelectClauses().stream().map(t->t.getAlias()).collect(Collectors.toList()), contains(
			"id", "name.family"
		));
		assertEquals(2, statement.getOrderByClauses().size());
		assertEquals("name.family", statement.getOrderByClauses().get(0).getClause());
		assertFalse(statement.getOrderByClauses().get(0).isAscending());
		assertEquals("id", statement.getOrderByClauses().get(1).getClause());
		assertTrue(statement.getOrderByClauses().get(1).isAscending());
	}

	private HfqlStatement parse(String theInput) {
		return new HfqlStatementParser(ourCtx, theInput).parse();
	}

	@Test
	public void testFromWhereSelect() {
		String input = """
			from
			  Patient
			having
			  name.given = 'Foo \\' Chalmers' and
			  name.family = 'blah'
			select
			  name.given[0],
			  name.family
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Patient", statement.getFromResourceName());
		assertEquals(2, statement.getSelectClauses().size());
		assertEquals("name.given[0]", statement.getSelectClauses().get(0).getClause());
		assertEquals("name.family", statement.getSelectClauses().get(1).getClause());
		assertEquals(2, statement.getHavingClauses().size());
		assertEquals("name.given", statement.getHavingClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getHavingClauses().get(0).getOperator());
		assertThat(statement.getHavingClauses().get(0).getRight(), contains("'Foo ' Chalmers'"));
		assertEquals("name.family", statement.getHavingClauses().get(1).getLeft());
		assertThat(statement.getHavingClauses().get(1).getRight(), contains("'blah'"));
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getHavingClauses().get(1).getOperator());
	}

	@Test
	public void testFromSearchWhereSelect() {
		String input = """
			from
			  Observation
			where
			  subject.name in ('foo' | 'bar') and _id='123'
			having
			  status = 'final'
			select
			  id
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Observation", statement.getFromResourceName());
		assertEquals(1, statement.getSelectClauses().size());
		assertEquals("id", statement.getSelectClauses().get(0).getClause());
		assertEquals(2, statement.getWhereClauses().size());
		assertEquals("subject.name", statement.getWhereClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.IN, statement.getWhereClauses().get(0).getOperator());
		assertThat(statement.getWhereClauses().get(0).getRight(), contains("'foo'", "'bar'"));
		assertEquals("_id", statement.getWhereClauses().get(1).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getWhereClauses().get(1).getOperator());
		assertThat(statement.getWhereClauses().get(1).getRight(), contains("'123'"));
		assertEquals(1, statement.getHavingClauses().size());
		assertEquals("status", statement.getHavingClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getHavingClauses().get(0).getOperator());
		assertThat(statement.getHavingClauses().get(0).getRight(), contains("'final'"));

	}

	@Test
	public void testFromWhereSelect_RichSearchExpression() {
		String input = """
			from
			  Observation
			where
			  _has:Observation:subject:device.identifier='1234-5'
			select
			  id
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Observation", statement.getFromResourceName());
		assertEquals(1, statement.getSelectClauses().size());
		assertEquals("id", statement.getSelectClauses().get(0).getClause());
		assertEquals(1, statement.getWhereClauses().size());
		assertEquals("_has:Observation:subject:device.identifier", statement.getWhereClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getWhereClauses().get(0).getOperator());
		assertThat(statement.getWhereClauses().get(0).getRight(), contains("'1234-5'"));

	}

	@Test
	public void testFromSearchWhereSelectLimit() {
		String input = """
			from
			  Observation
			where
			  subject.name in ('foo' | 'bar') and _id='123'
			having
			  status = 'final'
			select
			  id
			limit 123
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Observation", statement.getFromResourceName());
		assertEquals(1, statement.getSelectClauses().size());
		assertEquals("id", statement.getSelectClauses().get(0).getClause());
		assertEquals(2, statement.getWhereClauses().size());
		assertEquals("subject.name", statement.getWhereClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.IN, statement.getWhereClauses().get(0).getOperator());
		assertThat(statement.getWhereClauses().get(0).getRight(), contains("'foo'", "'bar'"));
		assertEquals("_id", statement.getWhereClauses().get(1).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getWhereClauses().get(1).getOperator());
		assertThat(statement.getWhereClauses().get(1).getRight(), contains("'123'"));
		assertEquals(1, statement.getHavingClauses().size());
		assertEquals("status", statement.getHavingClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.EQUALS, statement.getHavingClauses().get(0).getOperator());
		assertThat(statement.getHavingClauses().get(0).getRight(), contains("'final'"));
		assertEquals(123, statement.getLimit());
	}

	@Test
	public void testNamedSelectClauseWithFhirPath() {
		String input = """
			select
			   Weight: value.ofType(Quantity).value,
			   Unit: value.ofType(Quantity).unit
			from Observation
			""";

		HfqlStatement statement = parse(input);
		assertEquals("Observation", statement.getFromResourceName());
		assertEquals(2, statement.getSelectClauses().size());
		assertEquals("value.ofType(Quantity).value", statement.getSelectClauses().get(0).getClause());
		assertEquals("Weight", statement.getSelectClauses().get(0).getAlias());
		assertEquals("value.ofType(Quantity).unit", statement.getSelectClauses().get(1).getClause());
		assertEquals("Unit", statement.getSelectClauses().get(1).getAlias());

	}


	@Test
	public void testFromWhereSelect_InClauseAndNamedSelects() {
		// One select with spaces, one without
		String input = """
			from
			    StructureDefinition
			having
			    url in ('foo' | 'bar')
			select
			    Name : name,
			    URL:url
			""";
		HfqlStatement statement = parse(input);
		assertEquals("StructureDefinition", statement.getFromResourceName());
		assertEquals(2, statement.getSelectClauses().size());
		assertEquals("name", statement.getSelectClauses().get(0).getClause());
		assertEquals("Name", statement.getSelectClauses().get(0).getAlias());
		assertEquals("url", statement.getSelectClauses().get(1).getClause());
		assertEquals("URL", statement.getSelectClauses().get(1).getAlias());
		assertEquals(1, statement.getHavingClauses().size());
		assertEquals("url", statement.getHavingClauses().get(0).getLeft());
		assertEquals(HfqlStatement.WhereClauseOperatorEnum.IN, statement.getHavingClauses().get(0).getOperator());
		assertThat(statement.getHavingClauses().get(0).getRight(), contains(
			"'foo'", "'bar'"
		));

	}

	@Test
	public void testError_InvalidStart() {
		String input = """
			blah""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token (expected \"SELECT\") at position [line=0, column=0]: blah", ex.getMessage());
	}

	@Test
	public void testError_InvalidOrder() {
		String input = """
   		select id
   		from Patient
   		order foo
   		""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token (expected \"BY\") at position [line=2, column=6]: foo", ex.getMessage());
	}

	@Test
	public void testError_InvalidFrom() {
		String input = """
			from Blah""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Invalid FROM statement. Unknown resource type 'Blah' at position: [line=0, column=5]", ex.getMessage());
	}

	@Test
	public void testError_InvalidLimit() {
		String input = """
			from Patient
			select name.given
			limit foo
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token (expected integer value) at position [line=2, column=6]: foo", ex.getMessage());
	}

	@Test
	public void testError_InvalidSelect_EqualsParens() {
		String input = """
				from
				  Patient
				where
				  name.given = ('Foo')
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token (expected quoted string) at position [line=3, column=3]: (", ex.getMessage());
	}

	@Test
	public void testError_InvalidSelect_InWithoutParens() {
		String input = """
				from
				  Patient
				where
				  name.given in 'Foo'
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token (expected \"(\") at position [line=3, column=14]: in", ex.getMessage());
	}

	@Test
	public void testError_InvalidSelect_InWithoutPipe() {
		String input = """
				from
				  Patient
				where
				  name.given in ('foo' 'bar')
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token at position [line=3, column=22]: 'bar'", ex.getMessage());
	}

	@Test
	public void testError_InvalidSelect_InWithoutContent() {
		String input = """
				from
				  Patient
				where
				  name.given in
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected end of stream", ex.getMessage());
	}

	@Test
	public void testError_InvalidSelect_InWithoutEnd() {
		String input = """
				from
				  Patient
				where
				  name.given in ('foo' | 'bar'
			""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected end of stream", ex.getMessage());
	}

	@Test
	public void testError_MultipleHaving() {
		String input = """
			from
			  Patient
			having
			  name.given = 'Foo'
			where
			  _id = '123'
			having
			  name.family = 'Foo'
			select
			  name.given[0],
			  name.family
			""";

		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token at position [line=6, column=0]: having", ex.getMessage());
	}

	@Test
	public void testError_MultipleFrom() {
		String input = """
			from
			  Patient
			select
			  name.given[0],
			  name.family
			from
			  Patient
			""";

		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected token at position [line=5, column=0]: from", ex.getMessage());
	}

	@Test
	public void testError_NoText() {
		String input = "  \n  ";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected end of stream (expected \"FROM\")", ex.getMessage());
	}

	@Test
	public void testError_MissingSelect() {
		String input = """
			from Patient where""";
		DataFormatException ex = assertThrows(DataFormatException.class, () -> parse(input));
		assertEquals("Unexpected end of stream (expected \"SELECT\")", ex.getMessage());
	}

}