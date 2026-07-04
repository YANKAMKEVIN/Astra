import UIKit
import SwiftUI
import Shared

private var didRegisterLiteRtLmBridge = false

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        // Register the real LiteRT-LM engine before any Kotlin ViewModel can request a
        // generation, mirroring how registerIosGmailController() is wired inside
        // MainViewController(). This one has to originate from Swift since it needs to
        // construct the Swift-only LiteRTLM Engine/Conversation types.
        if !didRegisterLiteRtLmBridge {
            // Top-level Kotlin functions are exposed to Swift via a "<FileName>Kt" facade class —
            // this one lives in IosLiteRtLmRuntime.kt, hence IosLiteRtLmRuntimeKt below.
            IosLiteRtLmRuntimeKt.registerNativeLiteRtLmEngine(engine: LiteRtLmBridge())
            didRegisterLiteRtLmBridge = true
        }
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}