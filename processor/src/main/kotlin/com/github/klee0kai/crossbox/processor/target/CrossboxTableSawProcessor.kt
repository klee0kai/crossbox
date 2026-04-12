@file:OptIn(KspExperimental::class)

package com.github.klee0kai.crossbox.processor.target

import com.github.klee0kai.crossbox.core.CrossboxTableSaw
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class CrossboxTableSawProcessor : TargetFileProcessor {

    override suspend fun findSymbolsToProcess(
        resolver: Resolver,
    ): SymbolsToProcess {

        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(CrossboxTableSaw::class.asClassName().canonicalName)
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

        classDeclaration.getAnnotationsByType(CrossboxTableSaw::class)
            .firstOrNull() ?: return null

        val modelClassName = classDeclaration.toClassName()
        val packageName = fileOwner.packageName.asString().crossboxPackageName

        val tableSawClName = ClassName(packageName, "${classDeclaration.simpleName.getShortName()}TableSaw")

        val properties = classDeclaration.getDeclaredProperties()
            .filter { it.isPublic() }
            .toList()

        val fileSpec = genFileSpec(packageName, tableSawClName.simpleName) {
            genLibComment()

            val tableClazz = ClassName("tech.tablesaw.api", "Table")
            val columnClazz = ClassName("tech.tablesaw.api", "Column")

            // Function to convert Iterable<Model> to Table
            genFun("toTableSaw") {
                receiver(
                    Iterable::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableClazz)

                // Add each property as a column variable
                properties.forEach { property ->
                    val propName = property.simpleName.asString()
                    val propType = property.type.resolve()
                    val columnType = getTableSawColumnType(propType)

                    addStatement(
                        "val %LColumn = %T.create(\"%L\")",
                        propName,
                        columnType,
                        propName
                    )
                }

                beginControlFlow("this.forEach { item ->")
                properties.forEach { property ->
                    val propName = property.simpleName.asString()
                    val propType = property.type.resolve()
                    val columnType = getTableSawColumnType(propType)

                    if (propType.isMarkedNullable) {
                        // For nullable types, check null and use appendMissing() or set value
                        addStatement("%LColumn.appendMissing()", propName)
                        beginControlFlow("if (item.%L != null)", propName)

                        val nonNullType = propType.makeNotNullable()
                        val typeName = nonNullType.toTypeName().toString()

                        if (columnType.simpleName == "DoubleColumn" && (typeName == "java.lang.Number" || typeName == "kotlin.Number")) {
                            // Special case for Number -> Double conversion
                            addCode("    %LColumn.set(%LColumn.size() - 1, (item.%L as Number).toDouble())\n", propName, propName, propName)
                        } else {
                            val nonNullType = propType.makeNotNullable()
                            val typeName = nonNullType.toTypeName().toString()

                            // For different types, use appropriate conversion
                            when {
                                columnType.simpleName == "StringColumn" -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as String)\n", propName, propName, propName)
                                }
                                columnType.simpleName == "LongColumn" -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Long)\n", propName, propName, propName)
                                }
                                columnType.simpleName == "IntColumn" -> {
                                    // IntColumn can accept Int, Short, or Byte - convert to Int
                                    if (typeName == "kotlin.Short" || typeName == "java.lang.Short") {
                                        addCode("    %LColumn.set(%LColumn.size() - 1, (item.%L as Short).toInt())\n", propName, propName, propName)
                                    } else if (typeName == "kotlin.Byte" || typeName == "java.lang.Byte") {
                                        addCode("    %LColumn.set(%LColumn.size() - 1, (item.%L as Byte).toInt())\n", propName, propName, propName)
                                    } else {
                                        addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Int)\n", propName, propName, propName)
                                    }
                                }
                                columnType.simpleName == "DoubleColumn" -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Double)\n", propName, propName, propName)
                                }
                                columnType.simpleName == "FloatColumn" -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Float)\n", propName, propName, propName)
                                }
                                columnType.simpleName == "BooleanColumn" -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Boolean)\n", propName, propName, propName)
                                }
                                else -> {
                                    addCode("    %LColumn.set(%LColumn.size() - 1, item.%L as Any)\n", propName, propName, propName)
                                }
                            }
                        }
                        endControlFlow()
                    } else {
                        // For non-null types, cast if needed (e.g., Number to Double)
                        val nonNullType = propType.makeNotNullable()
                        val typeName = nonNullType.toTypeName().toString()
                        if (typeName == "java.lang.Number" || typeName == "kotlin.Number") {
                            // For Number type, cast to Double
                            addStatement("%LColumn.append((item.%L as Number).toDouble())", propName, propName)
                        } else {
                            addStatement("%LColumn.append(item.%L)", propName, propName)
                        }
                    }
                }
                endControlFlow()

                // Create array of columns and create table
                val columnsArrayLines = properties.joinToString(",\n        ") { property ->
                    "${property.simpleName.asString()}Column"
                }
                addCode("return %T.create(\n        %L\n    )\n", tableClazz, columnsArrayLines)
            }

            // Function to convert Sequence<Model> to Table
            genFun("toTableSaw") {
                receiver(
                    Sequence::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableClazz)

                addStatement("return this.toList().toTableSaw()")
            }

            // Function to convert single item to Table (single row)
            genFun("toTableSaw") {
                receiver(modelClassName)
                returns(tableClazz)

                addStatement("return listOf(this).toTableSaw()")
            }

            // Function to create empty table with correct schema
            genFun("createEmptyTableSaw") {
                receiver(
                    List::class.asClassName()
                        .parameterizedBy(modelClassName)
                )
                returns(tableClazz)

                addCode("return %T.create(\n", tableClazz)
                properties.forEach { property ->
                    val propName = property.simpleName.asString()
                    val propType = property.type.resolve()
                    val columnType = getTableSawColumnType(propType)
                    addCode("        %T.create(\"%L\"),\n", columnType, propName)
                }
                addCode("    )\n")
            }
        }

        return GenSpec(
            fileSpec = fileSpec,
            dependencies = Dependencies(aggregating = false, fileOwner),
        )

    }

    private fun getTableSawColumnType(propType: com.google.devtools.ksp.symbol.KSType): ClassName {
        // Remove nullable annotation
        val nonNullType = propType.makeNotNullable()
        val typeName = nonNullType.toTypeName().toString()
        return when {
            typeName == "kotlin.String" || typeName == "java.lang.String" ->
                ClassName("tech.tablesaw.api", "StringColumn")
            typeName == "kotlin.Int" || typeName == "java.lang.Integer" ->
                ClassName("tech.tablesaw.api", "IntColumn")
            typeName == "kotlin.Double" || typeName == "java.lang.Double" ->
                ClassName("tech.tablesaw.api", "DoubleColumn")
            typeName == "kotlin.Float" || typeName == "java.lang.Float" ->
                ClassName("tech.tablesaw.api", "FloatColumn")
            typeName == "kotlin.Long" || typeName == "java.lang.Long" ->
                ClassName("tech.tablesaw.api", "LongColumn")
            typeName == "kotlin.Boolean" || typeName == "java.lang.Boolean" ->
                ClassName("tech.tablesaw.api", "BooleanColumn")
            typeName == "kotlin.Short" || typeName == "java.lang.Short" ->
                ClassName("tech.tablesaw.api", "IntColumn") // Short uses IntColumn in Tablesaw
            typeName == "kotlin.Byte" || typeName == "java.lang.Byte" ->
                ClassName("tech.tablesaw.api", "IntColumn") // Byte uses IntColumn in Tablesaw
            typeName.startsWith("java.time.LocalDate") ->
                ClassName("tech.tablesaw.api", "DateColumn")
            typeName.startsWith("java.time.LocalDateTime") ->
                ClassName("tech.tablesaw.api", "DateTimeColumn")
            typeName.startsWith("java.time.LocalTime") ->
                ClassName("tech.tablesaw.api", "TimeColumn")
            typeName.startsWith("java.math.BigDecimal") ->
                ClassName("tech.tablesaw.api", "DoubleColumn")
            // Handle java.lang.Number as double
            typeName == "java.lang.Number" || typeName == "kotlin.Number" ->
                ClassName("tech.tablesaw.api", "DoubleColumn")
            else -> ClassName("tech.tablesaw.api", "StringColumn") // fallback
        }
    }

}
