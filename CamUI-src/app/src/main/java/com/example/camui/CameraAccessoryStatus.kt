package com.example.camui

import android.content.Context
import android.hardware.input.InputDevice
import android.hardware.input.InputManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager

internal data class CameraAccessoryStatus(
    val bluetoothShutterConnected: Boolean = false,
    val usbCaptureConnected: Boolean = false,
    val wiredHeadsetConnected: Boolean = false
) {
    val hasAnyConnection: Boolean
        get() = bluetoothShutterConnected || usbCaptureConnected || wiredHeadsetConnected
}

/**
 * Detect externally attached camera controls without treating Bluetooth being
 * enabled as proof of a connected shutter. Android has no universal public API
 * that identifies every BLE camera remote, so the Bluetooth indicator is shown
 * only for a connected external HID input device whose product name explicitly
 * identifies a camera/shutter/selfie remote.
 */
internal fun readCameraAccessoryStatus(context: Context): CameraAccessoryStatus {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    val usbDevices = runCatching { usbManager?.deviceList?.values?.toList().orEmpty() }
        .getOrDefault(emptyList())

    fun isCameraRemoteName(value: String?): Boolean {
        val name = value?.lowercase().orEmpty()
        return name.contains("shutter") ||
            name.contains("selfie") ||
            name.contains("camera remote") ||
            name.contains("camera shutter") ||
            name.contains("photo remote") ||
            name.contains("bluetooth remote") ||
            name.contains("bt remote") ||
            name.contains("自拍") ||
            name.contains("拍照")
    }

    val usbCamera = usbDevices.any { device ->
        device.deviceClass == UsbConstants.USB_CLASS_VIDEO ||
            (0 until device.interfaceCount).any { index ->
                runCatching {
                    device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_VIDEO
                }.getOrDefault(false)
            }
    }

    val usbShutter = usbDevices.any { device ->
        isCameraRemoteName(device.productName) &&
            ((device.deviceClass == UsbConstants.USB_CLASS_HID) ||
                (0 until device.interfaceCount).any { index ->
                    runCatching {
                        device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_HID
                    }.getOrDefault(false)
                })
    }

    val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    val externalCameraRemote = runCatching {
        inputManager?.inputDeviceIds.orEmpty()
            .mapNotNull { InputDevice.getDevice(it) }
            .firstOrNull { device ->
                device.isExternal &&
                    !device.isVirtual &&
                    (device.supportsSource(InputDevice.SOURCE_KEYBOARD) ||
                        device.supportsSource(InputDevice.SOURCE_DPAD)) &&
                    isCameraRemoteName(device.name) &&
                    !device.descriptor.contains("usb", ignoreCase = true)
            } != null
    }.getOrDefault(false)

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val wiredHeadset = runCatching {
        audioManager?.getDevices(AudioManager.GET_DEVICES_ALL).orEmpty().any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }
    }.getOrDefault(false)

    return CameraAccessoryStatus(
        bluetoothShutterConnected = externalCameraRemote && !usbShutter,
        usbCaptureConnected = usbCamera || usbShutter,
        wiredHeadsetConnected = wiredHeadset
    )
}
