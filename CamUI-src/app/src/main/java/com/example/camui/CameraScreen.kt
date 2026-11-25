                                    dynamicRangeText = proDynamicRangeLabel(
                                        hdrEnabled = hdrEnabled,
                                        isPhotoMode = mode == "Photo",
                                        nightShotEnabled = nightShotMode != "off",
                                        rawCaptureActive = imageCapture?.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW ||
                                            imageCapture?.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG,
                                        manualExposure = manualExposure,
                                        manualIsoEnabled = manualIsoEnabled,
                                        manualShutterEnabled = manualShutterEnabled,
                                        nativeImageProcessingEnabled = nativeImageProcessingEnabled,
                                        livePhotoEnabled = livePhotoEnabled
                                    ),