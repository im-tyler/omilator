import SwiftUI
import UiShared

@main
struct iOSApp: App {
    init() {
        // Workaround: force TSM (Text Services Manager) initialization on the
        // main thread before Compose tries to render text from a background
        // thread. Without this, iOS simulator crashes with
        // _dispatch_assert_queue_fail in TSMGetInputSourceProperty.
        let _ = UITextInputMode.activeInputModes
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        RootViewControllerKt.RootViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
