package com.github.klee0kai.crossbox.tests.rsql

import com.github.klee0kai.crossbox.core.tools.RsqlTools
import com.github.klee0kai.crossbox.example.DeepRsqlModel
import com.github.klee0kai.crossbox.example.SimpleModel
import com.github.klee0kai.crossbox.example.crossbox.filterByRsqlQuery
import com.github.klee0kai.crossbox.example.crossbox.matchesRsqlQuery
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RSQL Filter Tests")
class RsqlFilterTests {

    @Nested
    @DisplayName("RsqlTools Tests")
    inner class ParserTests {

        @Test
        @DisplayName("parseRsqlQuery splits by semicolon")
        fun testParseRsqlQuerySplitsBySemicolon() {
            val query = "someNameField==Alice;someIdField=gt=100;somePrefixFlagsField!=5"
            val result = RsqlTools.parseRsqlQuery(query)

            Assertions.assertEquals(3, result.size)
            Assertions.assertEquals("someNameField==Alice", result[0])
            Assertions.assertEquals("someIdField=gt=100", result[1])
            Assertions.assertEquals("somePrefixFlagsField!=5", result[2])
        }

        @Test
        @DisplayName("parseRsqlQuery trims whitespace")
        fun testParseRsqlQueryTrimsWhitespace() {
            val query = " someNameField==Alice ; someIdField=gt=100 "
            val result = RsqlTools.parseRsqlQuery(query)

            Assertions.assertEquals(2, result.size)
            Assertions.assertEquals("someNameField==Alice", result[0])
            Assertions.assertEquals("someIdField=gt=100", result[1])
        }

        @Test
        @DisplayName("parseRsqlQuery filters empty expressions")
        fun testParseRsqlQueryFiltersEmpty() {
            val query = "someNameField==Alice;;someIdField=gt=100"
            val result = RsqlTools.parseRsqlQuery(query)

            Assertions.assertEquals(2, result.size)
            Assertions.assertTrue(result.all { it.isNotEmpty() })
        }

        @Test
        @DisplayName("parseSingleExpression parses equality")
        fun testParseSingleExpressionEquality() {
            val expr = "someNameField==Alice"
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertEquals("someNameField", result["field"])
            Assertions.assertEquals("==", result["operator"])
            Assertions.assertEquals("Alice", result["value"])
        }

        @Test
        @DisplayName("parseSingleExpression parses all operators")
        fun testParseSingleExpressionOperators() {
            val operators = listOf("==", "!=", "=gt=", "=gte=", "=lt=", "=lte=", "=in=", "=out=")

            operators.forEach { op ->
                val expr = "field${op}value"
                val result = RsqlTools.parseSingleExpression(expr)

                Assertions.assertEquals("field", result["field"], "Failed for operator: $op")
                Assertions.assertEquals(op, result["operator"], "Failed for operator: $op")
                Assertions.assertEquals("value", result["value"], "Failed for operator: $op")
            }
        }

        @Test
        @DisplayName("parseSingleExpression handles whitespace")
        fun testParseSingleExpressionWhitespace() {
            val expr = "  someNameField  ==  Alice  "
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertEquals("someNameField", result["field"])
            Assertions.assertEquals("==", result["operator"])
            Assertions.assertEquals("Alice", result["value"])
        }

        @Test
        @DisplayName("parseSingleExpression returns empty for invalid expression")
        fun testParseSingleExpressionInvalid() {
            val expr = "invalid expression without operator"
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Single Item Matching Tests")
    inner class SingleItemMatchingTests {

        @Test
        @DisplayName("matchesRsqlQuery with equality operator")
        fun testMatchesRsqlQueryEquality() {
            val item = SimpleModel(someNameField = "Alice", someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==Bob"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with not equal operator")
        fun testMatchesRsqlQueryNotEqual() {
            val item = SimpleModel(someNameField = "Alice", someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField!=Bob"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField!=Alice"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with greater than operator")
        fun testMatchesRsqlQueryGreaterThan() {
            val item = SimpleModel(someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=gt=50"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=gt=100"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=gt=150"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with greater or equal operator")
        fun testMatchesRsqlQueryGreaterOrEqual() {
            val item = SimpleModel(someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=gte=50"))
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=gte=100"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=gte=150"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with less than operator")
        fun testMatchesRsqlQueryLessThan() {
            val item = SimpleModel(someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=lt=150"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=lt=100"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=lt=50"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with less or equal operator")
        fun testMatchesRsqlQueryLessOrEqual() {
            val item = SimpleModel(someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=lte=150"))
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=lte=100"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=lte=50"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with IN operator")
        fun testMatchesRsqlQueryIn() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField=in=Alice,Bob,Charlie"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField=in=Bob,Charlie"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with NOT IN operator")
        fun testMatchesRsqlQueryNotIn() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField=out=Bob,Charlie"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField=out=Alice,Bob"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with multiple conditions (AND logic)")
        fun testMatchesRsqlQueryMultipleConditions() {
            val item = SimpleModel(someNameField = "Alice", someIdField = 100L, somePrefixFlagsField = 10)

            // All conditions match
            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice;someIdField=gt=50;somePrefixFlagsField==10"))

            // One condition fails
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==Alice;someIdField=gt=150;somePrefixFlagsField==10"))

            // Multiple conditions fail
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==Bob;someIdField=lt=50;somePrefixFlagsField==20"))
        }

        @Test
        @DisplayName("matchesRsqlQuery with empty query")
        fun testMatchesRsqlQueryEmptyQuery() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertTrue(item.matchesRsqlQuery(""))
        }

        @Test
        @DisplayName("matchesRsqlQuery with null fields")
        fun testMatchesRsqlQueryNullFields() {
            val item = SimpleModel(someNameField = null, someIdField = 100L)

            // Null field doesn't match equality
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==Alice"))

            // Other fields still work
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField==100"))
        }
    }

    @Nested
    @DisplayName("Collection Filtering Tests")
    inner class CollectionFilteringTests {

        private val models = listOf(
            SimpleModel(someNameField = "Alice", anyCountField = 100, someIdField = 1000L, somePrefixFlagsField = 10),
            SimpleModel(someNameField = "Bob", anyCountField = 200, someIdField = 2000L, somePrefixFlagsField = 20),
            SimpleModel(someNameField = "Charlie", anyCountField = 150, someIdField = 1500L, somePrefixFlagsField = 15),
            SimpleModel(someNameField = "Diana", anyCountField = 300, someIdField = 3000L, somePrefixFlagsField = 30),
        )

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with equality")
        fun testFilterEquality() {
            val result = models.filterByRsqlQuery("someNameField==Alice")

            Assertions.assertEquals(1, result.size)
            Assertions.assertEquals("Alice", result[0].someNameField)
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with greater than")
        fun testFilterGreaterThan() {
            val result = models.filterByRsqlQuery("someIdField=gt=1500")

            Assertions.assertEquals(2, result.size)
            Assertions.assertTrue(result.all { it.someIdField!! > 1500 })
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with range")
        fun testFilterRange() {
            val result = models.filterByRsqlQuery("someIdField=gte=1000;someIdField=lte=2000")

            Assertions.assertEquals(3, result.size)
            Assertions.assertTrue(result.all { it.someIdField!! in 1000..2000 })
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with IN operator")
        fun testFilterIn() {
            val result = models.filterByRsqlQuery("someNameField=in=Alice,Bob")

            Assertions.assertEquals(2, result.size)
            Assertions.assertTrue(result.all { it.someNameField in listOf("Alice", "Bob") })
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with NOT IN operator")
        fun testFilterNotIn() {
            val result = models.filterByRsqlQuery("someNameField=out=Alice,Bob")

            Assertions.assertEquals(2, result.size)
            Assertions.assertTrue(result.all { it.someNameField !in listOf("Alice", "Bob") })
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with multiple conditions")
        fun testFilterMultipleConditions() {
            val result = models.filterByRsqlQuery("someIdField=gte=1500;someNameField!=Alice")

            Assertions.assertEquals(3, result.size)
            Assertions.assertTrue(result.all { it.someIdField!! >= 1500 && it.someNameField != "Alice" })
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with no matches")
        fun testFilterNoMatches() {
            val result = models.filterByRsqlQuery("someNameField==NonExistent")

            Assertions.assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with all matching")
        fun testFilterAllMatch() {
            val result = models.filterByRsqlQuery("someIdField=gt=0")

            Assertions.assertEquals(4, result.size)
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery with empty query")
        fun testFilterEmptyQuery() {
            val result = models.filterByRsqlQuery("")

            Assertions.assertEquals(4, result.size)
        }

        @Test
        @DisplayName("filterBySimpleModelRsqlQuery on empty collection")
        fun testFilterEmptyCollection() {
            val result = emptyList<SimpleModel>().filterByRsqlQuery("name==Alice")

            Assertions.assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Type Conversion Tests")
    inner class TypeConversionTests {

        @Test
        @DisplayName("Numeric comparison with Long")
        fun testNumericComparisonLong() {
            val item = SimpleModel(someIdField = 100L)

            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=gt=99"))
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=lt=101"))
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField==100"))
        }

        @Test
        @DisplayName("Numeric comparison with Short")
        fun testNumericComparisonShort() {
            val item = SimpleModel(somePrefixFlagsField = 10)

            // Short type comparison works with string equality
            Assertions.assertTrue(item.matchesRsqlQuery("somePrefixFlagsField==10"))
            Assertions.assertFalse(item.matchesRsqlQuery("somePrefixFlagsField==11"))
            // String-based IN operator works
            Assertions.assertTrue(item.matchesRsqlQuery("somePrefixFlagsField=in=10,20,30"))
        }

        @Test
        @DisplayName("String comparison with nullable field")
        fun testStringComparisonNullable() {
            val itemWithNull = SimpleModel(someNameField = null)
            val itemWithValue = SimpleModel(someNameField = "Alice")

            Assertions.assertFalse(itemWithNull.matchesRsqlQuery("someNameField==Alice"))
            Assertions.assertTrue(itemWithValue.matchesRsqlQuery("someNameField==Alice"))
        }

        @Test
        @DisplayName("Number type with IN operator")
        fun testNumberInOperator() {
            val item = SimpleModel(somePrefixFlagsField = 10)

            Assertions.assertTrue(item.matchesRsqlQuery("somePrefixFlagsField=in=10,20,30"))
            Assertions.assertFalse(item.matchesRsqlQuery("somePrefixFlagsField=in=20,30,40"))
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("Query with comma in value")
        fun testCommaInValue() {
            val item = SimpleModel(someNameField = "Alice,Bob")

            // Direct equality should work
            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice,Bob"))
        }

        @Test
        @DisplayName("Query with spaces in value")
        fun testSpacesInValue() {
            val item = SimpleModel(someNameField = "Alice Cooper")

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice Cooper"))
        }

        @Test
        @DisplayName("Invalid field name returns false")
        fun testInvalidFieldName() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertFalse(item.matchesRsqlQuery("invalidField==Alice"))
        }

        @Test
        @DisplayName("Multiple expressions with same field")
        fun testMultipleExpressionsSameField() {
            val item = SimpleModel(someIdField = 100L)

            // Both conditions on someIdField must be true
            Assertions.assertTrue(item.matchesRsqlQuery("someIdField=gte=50;someIdField=lte=150"))
            Assertions.assertFalse(item.matchesRsqlQuery("someIdField=gte=100;someIdField=lt=100"))
        }

        @Test
        @DisplayName("Case sensitivity in field names")
        fun testCaseSensitivityFieldNames() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice"))
            Assertions.assertFalse(item.matchesRsqlQuery("somenamefield==Alice"))
            Assertions.assertFalse(item.matchesRsqlQuery("SOMENAMEFD==Alice"))
        }

        @Test
        @DisplayName("Case sensitivity in values")
        fun testCaseSensitivityValues() {
            val item = SimpleModel(someNameField = "Alice")

            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Alice"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==alice"))
            Assertions.assertFalse(item.matchesRsqlQuery("someNameField==ALICE"))
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Complex real-world query")
        fun testComplexQuery() {
            val models = listOf(
                SimpleModel(
                    someNameField = "Alice",
                    anyCountField = 100,
                    someIdField = 1000L,
                    somePrefixFlagsField = 10
                ),
                SimpleModel(someNameField = "Bob", anyCountField = 200, someIdField = 2000L, somePrefixFlagsField = 20),
                SimpleModel(
                    someNameField = "Charlie",
                    anyCountField = 150,
                    someIdField = 1500L,
                    somePrefixFlagsField = 15
                ),
                SimpleModel(
                    someNameField = "Diana",
                    anyCountField = 300,
                    someIdField = 3000L,
                    somePrefixFlagsField = 30
                ),
            )

            // Find: (someIdField >= 1500) AND (someNameField != "Alice") AND (somePrefixFlagsField in [15,20,30])
            val result =
                models.filterByRsqlQuery("someIdField=gte=1500;someNameField!=Alice;somePrefixFlagsField=in=15,20,30")

            Assertions.assertEquals(3, result.size)
            Assertions.assertTrue(result.all { it.someIdField!! >= 1500 })
            Assertions.assertTrue(result.all { it.someNameField != "Alice" })
            Assertions.assertTrue(result.all { it.somePrefixFlagsField!! in listOf<Short>(15, 20, 30) })
        }

        @Test
        @DisplayName("Filter then match single item")
        fun testFilterThenMatch() {
            val models = listOf(
                SimpleModel(someNameField = "Alice", someIdField = 1000L),
                SimpleModel(someNameField = "Bob", someIdField = 2000L),
            )

            val filtered = models.filterByRsqlQuery("someIdField=gt=1500")
            Assertions.assertEquals(1, filtered.size)

            val item = filtered[0]
            Assertions.assertTrue(item.matchesRsqlQuery("someNameField==Bob"))
        }

        @Test
        @DisplayName("Builder pattern usage")
        fun testBuilderPatternUsage() {
            val models = listOf(
                SimpleModel(someNameField = "Alice", someIdField = 1000L),
                SimpleModel(someNameField = "Bob", someIdField = 2000L),
                SimpleModel(someNameField = "Charlie", someIdField = 1500L),
            )

            val query = "someIdField=gte=1000;someIdField=lte=2000"
            val inRange = models.filterByRsqlQuery(query)

            Assertions.assertEquals(3, inRange.size)
            inRange.forEach { model ->
                Assertions.assertTrue(model.matchesRsqlQuery(query))
            }
        }
    }

    @Nested
    @DisplayName("Nested Fields Tests")
    inner class NestedFieldsTests {

        @Test
        @DisplayName("Parse nested field expression")
        fun testParseNestedFieldExpression() {
            val expr = "children[*].someNameField==Alice"
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertEquals("children[*].someNameField", result["field"])
            Assertions.assertEquals("==", result["operator"])
            Assertions.assertEquals("Alice", result["value"])
        }

        @Test
        @DisplayName("Parse dot notation field expression")
        fun testParseDotNotationFieldExpression() {
            val expr = "parent.nestedField==value"
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertEquals("parent.nestedField", result["field"])
            Assertions.assertEquals("==", result["operator"])
            Assertions.assertEquals("value", result["value"])
        }

        @Test
        @DisplayName("Parse deeply nested field expression")
        fun testParseDeeplyNestedFieldExpression() {
            val expr = "level1[*].level2.level3==test"
            val result = RsqlTools.parseSingleExpression(expr)

            Assertions.assertEquals("level1[*].level2.level3", result["field"])
            Assertions.assertEquals("==", result["operator"])
            Assertions.assertEquals("test", result["value"])
        }

        @Test
        @DisplayName("Filter deep models by list field")
        fun testFilterDeepModelsByListField() {
            val model1 = DeepRsqlModel(
                commonId = 1L,
                children = listOf(
                    SimpleModel(someNameField = "Alice", someIdField = 100L),
                    SimpleModel(someNameField = "Bob", someIdField = 200L)
                )
            )
            val model2 = DeepRsqlModel(
                commonId = 2L,
                children = listOf(
                    SimpleModel(someNameField = "Charlie", someIdField = 300L)
                )
            )
            val model3 = DeepRsqlModel(
                commonId = 3L,
                children = null
            )

            val models = listOf(model1, model2, model3)

            // Test matching models that have children with specific name
            val result = models.filterByRsqlQuery("children[*].someNameField==Alice")
            Assertions.assertEquals(1, result.size)
            Assertions.assertEquals(1L, result[0].commonId)
        }

        @Test
        @DisplayName("Filter deep models by list field with numeric comparison")
        fun testFilterDeepModelsByListFieldNumeric() {
            val model1 = DeepRsqlModel(
                commonId = 1L,
                children = listOf(
                    SimpleModel(someNameField = "Alice", someIdField = 100L),
                    SimpleModel(someNameField = "Bob", someIdField = 200L)
                )
            )
            val model2 = DeepRsqlModel(
                commonId = 2L,
                children = listOf(
                    SimpleModel(someNameField = "Charlie", someIdField = 300L)
                )
            )

            val models = listOf(model1, model2)

            // Find models that have at least one child with id > 150
            val result = models.filterByRsqlQuery("children[*].someIdField=gt=150")
            Assertions.assertEquals(2, result.size)
        }

        @Test
        @DisplayName("Filter deep models by recursive list")
        fun testFilterDeepModelsByRecursiveList() {
            val deepChild = DeepRsqlModel(
                commonId = 10L,
                children = listOf(
                    SimpleModel(someNameField = "DeepChild", someIdField = 1000L)
                )
            )
            val model1 = DeepRsqlModel(
                commonId = 1L,
                recursiveChildren = listOf(deepChild)
            )
            val model2 = DeepRsqlModel(
                commonId = 2L,
                recursiveChildren = emptyList()
            )

            val models = listOf(model1, model2)

            // Find models that have recursive children with specific id
            val result = models.filterByRsqlQuery("recursiveChildren[*].commonId==10")
            Assertions.assertEquals(1, result.size)
            Assertions.assertEquals(1L, result[0].commonId)
        }

        @Test
        @DisplayName("Multiple conditions on nested fields")
        fun testMultipleConditionsOnNestedFields() {
            val model1 = DeepRsqlModel(
                commonId = 1L,
                children = listOf(
                    SimpleModel(someNameField = "Alice", someIdField = 100L),
                    SimpleModel(someNameField = "Bob", someIdField = 200L)
                )
            )
            val model2 = DeepRsqlModel(
                commonId = 2L,
                children = listOf(
                    SimpleModel(someNameField = "Charlie", someIdField = 300L)
                )
            )

            val models = listOf(model1, model2)

            // Find models matching multiple conditions
            val result = models.filterByRsqlQuery("commonId=gte=1;children[*].someIdField=gt=150")
            Assertions.assertEquals(2, result.size)
        }

        @Test
        @DisplayName("Empty list doesn't match nested filter")
        fun testEmptyListDoesntMatch() {
            val model = DeepRsqlModel(
                commonId = 1L,
                children = null
            )

            Assertions.assertFalse(model.matchesRsqlQuery("children[*].someNameField==Alice"))
        }

        @Test
        @DisplayName("Nested field on null parent returns false")
        fun testNestedFieldOnNullParent() {
            val model = DeepRsqlModel(
                commonId = 1L,
                children = null
            )

            Assertions.assertFalse(model.matchesRsqlQuery("children[*].someNameField!=Unknown"))
        }
    }

}