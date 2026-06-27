import SwiftUI
import UiShared

@main
struct iOSApp: App {
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
