@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.core.CrossboxRsqlFilter
import com.github.klee0kai.crossbox.core.tools.RsqlTools
import com.github.klee0kai.crossbox.processor.ksp.GenSpec
import com.github.klee0kai.crossbox.processor.ksp.SymbolsToProcess
import com.github.klee0kai.crossbox.processor.ksp.TargetFileProcessor
import com.github.klee0kai.crossbox.processor.poet.crossboxPackageName
import com.github.klee0kai.crossbox.processor.poet.genFileSpec
import com.github.klee0kai.crossbox.processor.poet.genFun
import com.github.klee0kai.crossbox.processor.poet.genLibComment
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

class CrossboxRsqlFilterProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxRsqlFilter::class.asClassName().canonicalName)
            .groupBy { it.validate() }

        return SymbolsToProcess(
            symbolsForProcessing = annotatedSymbols[true].orEmpty(),
            symbolsForReprocessing = annotatedSymbols[false].orEmpty(),
        )

    }

    override suspend fun process(
        targetSymbol: KSAnnotated,
        resolver: Resolver,
        options: Map<String, String>,
        logger: KSPLogger
    ): GenSpec? {
        val fileOwner = targetSymbol.containingFile ?: return null
        val classDeclaration = targetSymbol as? KSClassDeclaration ?: return null

        val rsqlFilterAnn = classDeclaration.getAnnotationsByType(CrossboxRsqlFilter::class)
            .firstOrNull() ?: return null

        val modelClassName = classDeclaration.toClassName()
        val packageName = fileOwner.packageName.asString().crossboxPackageName

        val rsqlParserClName = ClassName(packageName, "${classDeclaration.simpleName.getShortName()}RsqlParser")

        val properties = classDeclaration.getDeclaredProperties()
            .filter { it.isPublic() }
            .toList()

        val fileSpec = genFileSpec(packageName, rsqlParserClName.simpleName) {
            genLibComment()

            genFun("filterByRsqlQuery") {
                receiver(
                    Iterable::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                addParameter("rsqlQuery", String::class.asClassName())
                returns(
                    List::class.asClassName()
                        .parameterizedBy(modelClassName)
                )

                addStatement("val expressions = %T.parseRsqlQuery(rsqlQuery)", RsqlTools::class)
                addStatement("return this.filter { item -> expressions.all { expr -> item.matchesRsqlQuery(expr) } }")
            }

            genFun("filterByRsqlQuery") {
                receiver(
                    Sequence::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                addParameter("rsqlQuery", String::class.asClassName())
                returns(
                    Sequence::class.asClassName()
                        .parameterizedBy(modelClassName)
                )

                addStatement("val expressions = %T.parseRsqlQuery(rsqlQuery)", RsqlTools::class)
                addStatement("return this.filter { item -> expressions.all { expr -> item.matchesRsqlQuery(expr) } }")
            }

            // Generate single item filter function
            genFun("matchesRsqlQuery") {
                receiver(modelClassName)
                addParameter("rsqlQuery", String::class.asClassName())
                returns(Boolean::class.asClassName())

                addStatement("val expressions = %T.parseRsqlQuery(rsqlQuery)", RsqlTools::class)
                beginControlFlow("return expressions.all { expression ->")
                addStatement("val parsed = %T.parseSingleExpression(expression)", RsqlTools::class)
                beginControlFlow("if (parsed.isEmpty())")
                addStatement("return@all true")
                endControlFlow()
                addStatement(
                    "val fieldName = parsed[\"field\"] ?: return@all false"
                )
                addStatement(
                    "val operator = parsed[\"operator\"] ?: return@all false"
                )
                addStatement(
                    "val value = parsed[\"value\"] ?: return@all false"
                )
                addStatement("matchField(fieldName, operator, value)")
                endControlFlow()
            }

            // Helper function to match fields with nested path support
            genFun("matchField") {
                receiver(modelClassName)
                addParameter("fieldPath", String::class.asClassName())
                addParameter("operator", String::class.asClassName())
                addParameter("value", String::class.asClassName())
                returns(Boolean::class.asClassName())

                // First split by dot to get all segments (including [*] parts)
                addStatement("val allSegments = fieldPath.split(\".\")")
                addStatement("val rootSegment = allSegments.getOrNull(0) ?: return false")

                // Check if root is a list access and extract field name
                addStatement("val isRootList = rootSegment.endsWith(\"[*]\")")
                addStatement("val rootField = if (isRootList) rootSegment.dropLast(3) else rootSegment")
                addStatement("val remainingPath = if (isRootList) listOf(\"[*]\") + allSegments.drop(1) else allSegments.drop(1)")

                val fieldMatchCode = CodeBlock.builder()
                fieldMatchCode.beginControlFlow("val fieldValue = when (rootField)")
                properties.forEach { property ->
                    val propName = property.simpleName.asString()
                    fieldMatchCode.addStatement("\"%L\" -> %L", propName, propName)
                }
                fieldMatchCode.addStatement("else -> null")
                fieldMatchCode.endControlFlow()

                addCode(fieldMatchCode.build())
                addStatement("return matchNestedField(fieldValue, remainingPath, operator, value)")
            }

            // Helper function to recursively match nested fields
            genFun("matchNestedField") {
                receiver(modelClassName)
                addParameter("fieldValue", Any::class.asClassName().copy(nullable = true))
                addParameter("remainingPath", List::class.asClassName().parameterizedBy(String::class.asClassName()))
                addParameter("operator", String::class.asClassName())
                addParameter("value", String::class.asClassName())
                returns(Boolean::class.asClassName())

                addStatement("if (fieldValue == null) return false")
                addStatement("if (remainingPath.isEmpty()) return %T.compareValues(fieldValue, operator, value)", RsqlTools::class)

                addStatement("val nextSegment = remainingPath.getOrNull(0) ?: return false")

                // Handle [*] marker specially - it means we need to iterate through a list
                beginControlFlow("if (nextSegment == \"[*]\")")
                beginControlFlow("if (fieldValue !is List<*>)")
                addStatement("return false")
                endControlFlow()
                addStatement("val nextPath = remainingPath.drop(1)")
                addStatement("return fieldValue.any { item -> item != null && matchNestedField(item, nextPath, operator, value) }")
                endControlFlow()

                // For non-list access, extract field name and use reflection
                addStatement("val field = try { fieldValue.javaClass.getDeclaredField(nextSegment) } catch (e: Exception) { null } ?: return false")
                addStatement("field.isAccessible = true")
                addStatement("val nextValue = try { field.get(fieldValue) } catch (e: Exception) { null }")
                addStatement("return matchNestedField(nextValue, remainingPath.drop(1), operator, value)")
            }

        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }

}
