package com.github.klee0kai.crossbox.core.tools

object RsqlTools {

    val rsqlRegex by lazy { Regex("([\\w.\\[\\]*]+)\\s*(==|!=|=gt=|=gte=|=lt=|=lte=|=in=|=out=)\\s*(.+)") }

    public fun parseRsqlQuery(
        rsqlQuery: String,
    ): List<String> = rsqlQuery
        .split(";".toRegex())
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    public fun parseSingleExpression(expression: String): Map<String, String> {
        val match = rsqlRegex.find(expression)
        if (match != null) {
            return mapOf(
                "field" to match.groupValues[1].trim(),
                "operator" to match.groupValues[2].trim(),
                "value" to match.groupValues[3].trim()
            )
        }
        return emptyMap()
    }

    public fun compareValues(
        fieldValue: Any,
        `operator`: String,
        compareValue: String,
    ): Boolean = when (operator) {
        "==" -> fieldValue.toString() == compareValue
        "!=" -> fieldValue.toString() != compareValue
        "=gt=" -> try {
            ((fieldValue as? Comparable<Any>)?.compareTo(compareValue.toComparableNumberValue()) ?: 0) > 0
        } catch (e: Exception) {
            false
        }

        "=gte=" -> try {
            ((fieldValue as? Comparable<Any>)?.compareTo(compareValue.toComparableNumberValue()) ?: 0) >= 0
        } catch (e: Exception) {
            false
        }

        "=lt=" -> try {
            ((fieldValue as? Comparable<Any>)?.compareTo(compareValue.toComparableNumberValue()) ?: 0) < 0
        } catch (e: Exception) {
            false
        }

        "=lte=" -> try {
            ((fieldValue as? Comparable<Any>)?.compareTo(compareValue.toComparableNumberValue()) ?: 0) <= 0
        } catch (e: Exception) {
            false
        }

        "=in=" -> compareValue.split(",").map { it.trim() }.contains(fieldValue.toString())
        "=out=" -> !compareValue.split(",").map { it.trim() }.contains(fieldValue.toString())
        else -> false
    }

    private fun String.toComparableNumberValue(): Comparable<Any> = try {
        this.toLong() as Comparable<Any>
    } catch (e: NumberFormatException) {
        this.toDoubleOrNull()?.let { it as Comparable<Any> } ?: (this as Comparable<Any>)
    }

}