#include <jni.h>
#include <android/log.h>
#include <jpeglib.h>
#include <setjmp.h>
#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>

#define FUJI_TAG "FUJI_JPEG_TILE"
#define FUJI_TILE_ROWS 64

struct FujiJpegError {
    jpeg_error_mgr base;
    jmp_buf jump;
    char message[JMSG_LENGTH_MAX];
};

static void fujiJpegErrorExit(j_common_ptr common) {
    FujiJpegError* error = reinterpret_cast<FujiJpegError*>(common->err);
    (*common->err->format_message)(common, error->message);
    longjmp(error->jump, 1);
}

static int64_t elapsedRealtimeMs() {
    struct timespec value;
    if (clock_gettime(CLOCK_BOOTTIME, &value) != 0) return 0;
    return static_cast<int64_t>(value.tv_sec) * 1000LL +
        static_cast<int64_t>(value.tv_nsec / 1000000LL);
}

static bool readOneSourceRow(
    jpeg_decompress_struct* decoder,
    unsigned char* destination
) {
    JSAMPROW row = destination;
    return jpeg_read_scanlines(decoder, &row, 1) == 1;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_camui_JpegTileEncoder_encode(
    JNIEnv* env,
    jclass,
    jstring sourcePathValue,
    jstring outputPathValue,
    jint targetWidthValue,
    jint targetHeightValue,
    jint cropLeftValue,
    jint cropTopValue,
    jint cropWidthValue,
    jint cropHeightValue,
    jint qualityValue,
    jlong deadlineElapsedMs
) {
    if (sourcePathValue == nullptr || outputPathValue == nullptr) {
        return env->NewStringUTF("Đường dẫn ảnh nguồn hoặc đầu ra rỗng.");
    }

    const char* sourcePath = env->GetStringUTFChars(sourcePathValue, nullptr);
    const char* outputPath = env->GetStringUTFChars(outputPathValue, nullptr);
    if (sourcePath == nullptr || outputPath == nullptr) {
        if (sourcePath != nullptr) env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        if (outputPath != nullptr) env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return env->NewStringUTF("Không đọc được đường dẫn tệp.");
    }

    FILE* input = fopen(sourcePath, "rb");
    if (input == nullptr) {
        jstring result = env->NewStringUTF("Không mở được JPEG nguồn để nâng theo tile.");
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }
    FILE* output = fopen(outputPath, "wb");
    if (output == nullptr) {
        fclose(input);
        jstring result = env->NewStringUTF("Không tạo được tệp JPEG đầu ra theo tile.");
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    jpeg_decompress_struct decoder;
    jpeg_compress_struct encoder;
    FujiJpegError error;
    memset(&decoder, 0, sizeof(decoder));
    memset(&encoder, 0, sizeof(encoder));
    memset(&error, 0, sizeof(error));

    decoder.err = jpeg_std_error(&error.base);
    encoder.err = jpeg_std_error(&error.base);
    error.base.error_exit = fujiJpegErrorExit;

    bool decoderCreated = false;
    bool encoderCreated = false;
    unsigned char* sourceRowA = nullptr;
    unsigned char* sourceRowB = nullptr;
    unsigned char* tileBuffer = nullptr;
    JSAMPROW* tileRows = nullptr;

    const auto cleanup = [&]() {
        if (encoderCreated) {
            jpeg_destroy_compress(&encoder);
            encoderCreated = false;
        }
        if (decoderCreated) {
            jpeg_destroy_decompress(&decoder);
            decoderCreated = false;
        }
        free(sourceRowA);
        free(sourceRowB);
        free(tileBuffer);
        free(tileRows);
        sourceRowA = nullptr;
        sourceRowB = nullptr;
        tileBuffer = nullptr;
        tileRows = nullptr;
        if (input != nullptr) {
            fclose(input);
            input = nullptr;
        }
        if (output != nullptr) {
            fclose(output);
            output = nullptr;
        }
    };

    if (setjmp(error.jump)) {
        char message[JMSG_LENGTH_MAX + 128];
        snprintf(message, sizeof(message), "JPEG codec: %s", error.message);
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    jpeg_create_decompress(&decoder);
    decoderCreated = true;
    jpeg_stdio_src(&decoder, input);
    if (jpeg_read_header(&decoder, TRUE) != JPEG_HEADER_OK) {
        snprintf(error.message, sizeof(error.message), "%s", "JPEG nguồn không có header hợp lệ.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    // Captured input is normalized to EXIF Orientation=1 by the Kotlin capture pipeline.
    decoder.out_color_space = JCS_RGB;
    decoder.dct_method = JDCT_IFAST;
    decoder.do_fancy_upsampling = TRUE;
    jpeg_start_decompress(&decoder);

    const int sourceWidth = static_cast<int>(decoder.output_width);
    const int sourceHeight = static_cast<int>(decoder.output_height);
    const int targetWidth = static_cast<int>(targetWidthValue);
    const int targetHeight = static_cast<int>(targetHeightValue);
    const int cropLeft = static_cast<int>(cropLeftValue);
    const int cropTop = static_cast<int>(cropTopValue);
    const int cropWidth = static_cast<int>(cropWidthValue);
    const int cropHeight = static_cast<int>(cropHeightValue);
    const int quality = static_cast<int>(qualityValue);

    if (decoder.output_components != 3 ||
        sourceWidth < 2 || sourceHeight < 2 ||
        targetWidth < 2 || targetHeight < 2 ||
        cropLeft < 0 || cropTop < 0 || cropWidth < 2 || cropHeight < 2 ||
        cropLeft + cropWidth > sourceWidth ||
        cropTop + cropHeight > sourceHeight ||
        targetWidth > JPEG_MAX_DIMENSION ||
        targetHeight > JPEG_MAX_DIMENSION ||
        quality < 1 || quality > 100) {
        snprintf(error.message, sizeof(error.message), "%s",
            "Kích thước, vùng crop hoặc định dạng màu không hợp lệ.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    if (deadlineElapsedMs > 0 && elapsedRealtimeMs() >= deadlineElapsedMs) {
        snprintf(error.message, sizeof(error.message), "%s", "TIMEOUT: vượt ngân sách nâng ảnh theo tile.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    jpeg_create_compress(&encoder);
    encoderCreated = true;
    jpeg_stdio_dest(&encoder, output);
    encoder.image_width = static_cast<JDIMENSION>(targetWidth);
    encoder.image_height = static_cast<JDIMENSION>(targetHeight);
    encoder.input_components = 3;
    encoder.in_color_space = JCS_RGB;
    jpeg_set_defaults(&encoder);
    jpeg_set_quality(&encoder, quality, TRUE);
    encoder.optimize_coding = TRUE;
    encoder.dct_method = JDCT_IFAST;
    jpeg_start_compress(&encoder, TRUE);

    const size_t sourceRowBytes = static_cast<size_t>(sourceWidth) * 3U;
    const size_t outputRowBytes = static_cast<size_t>(targetWidth) * 3U;
    const size_t tileBytes = outputRowBytes * FUJI_TILE_ROWS;
    if (sourceRowBytes / 3U != static_cast<size_t>(sourceWidth) ||
        outputRowBytes / 3U != static_cast<size_t>(targetWidth) ||
        tileBytes / outputRowBytes != FUJI_TILE_ROWS) {
        snprintf(error.message, sizeof(error.message), "%s", "Tràn kích thước bộ đệm tile.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    sourceRowA = static_cast<unsigned char*>(malloc(sourceRowBytes));
    sourceRowB = static_cast<unsigned char*>(malloc(sourceRowBytes));
    tileBuffer = static_cast<unsigned char*>(malloc(tileBytes));
    tileRows = static_cast<JSAMPROW*>(malloc(sizeof(JSAMPROW) * FUJI_TILE_ROWS));
    if (!sourceRowA || !sourceRowB || !tileBuffer || !tileRows) {
        snprintf(error.message, sizeof(error.message), "%s", "Không đủ bộ nhớ cho hai hàng nguồn và một tile 64 hàng.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    if (!readOneSourceRow(&decoder, sourceRowA)) {
        snprintf(error.message, sizeof(error.message), "%s", "Không giải mã được hàng JPEG nguồn đầu tiên.");
        cleanup();
        remove(outputPath);
        jstring result = env->NewStringUTF(error.message);
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }
    if (!readOneSourceRow(&decoder, sourceRowB)) {
        memcpy(sourceRowB, sourceRowA, sourceRowBytes);
    }
    int cachedTopY = 0;

    for (int tileStartY = 0; tileStartY < targetHeight; tileStartY += FUJI_TILE_ROWS) {
        if (deadlineElapsedMs > 0 && elapsedRealtimeMs() >= deadlineElapsedMs) {
            snprintf(error.message, sizeof(error.message), "%s", "TIMEOUT: vượt ngân sách xử lý một tile JPEG.");
            cleanup();
            remove(outputPath);
            jstring result = env->NewStringUTF(error.message);
            env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
            env->ReleaseStringUTFChars(outputPathValue, outputPath);
            return result;
        }

        const int rowsInTile = (tileStartY + FUJI_TILE_ROWS <= targetHeight)
            ? FUJI_TILE_ROWS : (targetHeight - tileStartY);
        for (int tileRow = 0; tileRow < rowsInTile; ++tileRow) {
            const int outputY = tileStartY + tileRow;
            double sourceY = cropTop +
                ((static_cast<double>(outputY) + 0.5) * cropHeight / targetHeight) - 0.5;
            if (sourceY < cropTop) sourceY = cropTop;
            const double cropBottom = cropTop + cropHeight - 1.0;
            if (sourceY > cropBottom) sourceY = cropBottom;
            int sourceY0 = static_cast<int>(floor(sourceY));
            if (sourceY0 < cropTop) sourceY0 = cropTop;
            if (sourceY0 > cropTop + cropHeight - 1) sourceY0 = cropTop + cropHeight - 1;
            int sourceY1 = sourceY0 + 1;
            if (sourceY1 > cropTop + cropHeight - 1) sourceY1 = sourceY0;
            const double fy = sourceY - sourceY0;

            while (cachedTopY < sourceY0) {
                unsigned char* swap = sourceRowA;
                sourceRowA = sourceRowB;
                sourceRowB = swap;
                ++cachedTopY;
                if (cachedTopY + 1 < sourceHeight) {
                    if (!readOneSourceRow(&decoder, sourceRowB)) {
                        snprintf(error.message, sizeof(error.message), "%s", "JPEG nguồn bị thiếu dữ liệu ở giữa ảnh.");
                        cleanup();
                        remove(outputPath);
                        jstring result = env->NewStringUTF(error.message);
                        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
                        env->ReleaseStringUTFChars(outputPathValue, outputPath);
                        return result;
                    }
                } else {
                    memcpy(sourceRowB, sourceRowA, sourceRowBytes);
                }
            }

            unsigned char* outputRow = tileBuffer + static_cast<size_t>(tileRow) * outputRowBytes;
            for (int x = 0; x < targetWidth; ++x) {
                double sourceX = cropLeft +
                    ((static_cast<double>(x) + 0.5) * cropWidth / targetWidth) - 0.5;
                if (sourceX < cropLeft) sourceX = cropLeft;
                const double cropRight = cropLeft + cropWidth - 1.0;
                if (sourceX > cropRight) sourceX = cropRight;
                int sourceX0 = static_cast<int>(floor(sourceX));
                if (sourceX0 < cropLeft) sourceX0 = cropLeft;
                if (sourceX0 > cropLeft + cropWidth - 1) sourceX0 = cropLeft + cropWidth - 1;
                int sourceX1 = sourceX0 + 1;
                if (sourceX1 > cropLeft + cropWidth - 1) sourceX1 = sourceX0;
                const double fx = sourceX - sourceX0;

                const size_t p00 = static_cast<size_t>(sourceX0) * 3U;
                const size_t p01 = static_cast<size_t>(sourceX1) * 3U;
                const size_t out = static_cast<size_t>(x) * 3U;
                for (int channel = 0; channel < 3; ++channel) {
                    const double top = sourceRowA[p00 + channel] +
                        (sourceRowA[p01 + channel] - sourceRowA[p00 + channel]) * fx;
                    const double bottom = sourceRowB[p00 + channel] +
                        (sourceRowB[p01 + channel] - sourceRowB[p00 + channel]) * fx;
                    double value = top + (bottom - top) * fy;
                    if (value < 0.0) value = 0.0;
                    if (value > 255.0) value = 255.0;
                    outputRow[out + channel] = static_cast<unsigned char>(value + 0.5);
                }
            }
            tileRows[tileRow] = outputRow;
        }

        JDIMENSION written = jpeg_write_scanlines(
            &encoder,
            tileRows,
            static_cast<JDIMENSION>(rowsInTile)
        );
        if (written != static_cast<JDIMENSION>(rowsInTile)) {
            snprintf(error.message, sizeof(error.message), "%s", "JPEG encoder không ghi đủ hàng của tile.");
            cleanup();
            remove(outputPath);
            jstring result = env->NewStringUTF(error.message);
            env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
            env->ReleaseStringUTFChars(outputPathValue, outputPath);
            return result;
        }
        __android_log_print(
            ANDROID_LOG_VERBOSE,
            FUJI_TAG,
            "JPEG_TILE_COMPLETE y=%d rows=%d output=%dx%d",
            tileStartY,
            rowsInTile,
            targetWidth,
            targetHeight
        );
    }

    jpeg_finish_compress(&encoder);
    jpeg_finish_decompress(&decoder);
    cleanup();

    FILE* verify = fopen(outputPath, "rb");
    if (verify == nullptr) {
        remove(outputPath);
        jstring result = env->NewStringUTF("JPEG tile encoder không tạo được tệp đầu ra.");
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }
    fseek(verify, 0, SEEK_END);
    long fileBytes = ftell(verify);
    fclose(verify);
    if (fileBytes <= 0) {
        remove(outputPath);
        jstring result = env->NewStringUTF("JPEG tile encoder tạo ra tệp rỗng.");
        env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
        env->ReleaseStringUTFChars(outputPathValue, outputPath);
        return result;
    }

    __android_log_print(
        ANDROID_LOG_INFO,
        FUJI_TAG,
        "JPEG_TILE_ENCODE_SUCCESS size=%dx%d quality=%d bytes=%ld tileRows=%d",
        targetWidth,
        targetHeight,
        quality,
        fileBytes,
        FUJI_TILE_ROWS
    );
    env->ReleaseStringUTFChars(sourcePathValue, sourcePath);
    env->ReleaseStringUTFChars(outputPathValue, outputPath);
    return nullptr;
}
