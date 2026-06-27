@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import kotlin.native.concurrent.ThreadLocal

private class PickerDelegate(
    private val onPicked: (String?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onPicked(url?.path)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(null)
    }
}

@ThreadLocal
private var retainedDelegate: PickerDelegate? = null

fun pickFile(viewController: UIViewController?, onPicked: (String?) -> Unit) {
    val vc = viewController ?: return
    val contentTypes: List<UTType> = listOfNotNull(UTType.typeWithIdentifier("public.item"))
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes)
    picker.allowsMultipleSelection = false
    val delegate = PickerDelegate(onPicked)
    retainedDelegate = delegate
    picker.delegate = delegate
    vc.presentViewController(picker, true, null)
}

fun pickDirectory(viewController: UIViewController?, onPicked: (String?) -> Unit) {
    val vc = viewController ?: return
    val contentTypes: List<UTType> = listOfNotNull(UTType.typeWithIdentifier("public.folder"))
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = contentTypes)
    picker.allowsMultipleSelection = false
    val delegate = PickerDelegate(onPicked)
    retainedDelegate = delegate
    picker.delegate = delegate
    vc.presentViewController(picker, true, null)
}
