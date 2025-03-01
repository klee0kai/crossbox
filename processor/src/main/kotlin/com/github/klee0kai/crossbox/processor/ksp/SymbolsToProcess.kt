package com.github.klee0kai.crossbox.processor.ksp

import com.google.devtools.ksp.symbol.KSAnnotated

data class SymbolsToProcess(
    val symbolsForProcessing: List<KSAnnotated>,
    val symbolsForReprocessing: List<KSAnnotated>,
)