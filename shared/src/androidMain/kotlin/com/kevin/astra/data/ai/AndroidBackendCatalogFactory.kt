package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendCatalog

// Android ships both catalog backends unconditionally: the Mock engine and the MediaPipe-backed
// LiteRT-LM runtime (a compile-time dependency), so their default Installed status needs no
// platform override.
actual fun createBackendCatalog(): BackendCatalog = DefaultBackendCatalog()
