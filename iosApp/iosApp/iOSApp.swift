import SwiftUI
import UIKit
import Shared

/// Hands background URLSession wake-ups to the shared download manager. When a model finishes
/// downloading while ASTRA is suspended or terminated, iOS relaunches the app in the background
/// and calls this; the Kotlin side recreates the session, files the model, then calls back.
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        BackgroundDownloadEvents.shared.handle(identifier: identifier, completionHandler: completionHandler)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
