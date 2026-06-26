package com.kevin.astra.domain.benchmark

interface BenchmarkRunner {
    suspend fun run(request: BenchmarkRequest): BenchmarkReport
}
