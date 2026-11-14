#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <algorithm>
#include <cstdint>
#include <cmath>
#include <mutex>
#include <string>
#include <vector>

#include "net.h"
#include "gpu.h"
#include "cpu.h"

namespace {
constexpr const char* kTag = "FUJI_NCNN_SR";
constexpr int kScale = 2;
constexpr int kPrePadding = 10;
constexpr int kDefaultTileSize = 192;

std::mutex gInferenceMutex;

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, kTag, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, kTag, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, kTag, __VA_ARGS__)

class UtfChars {
public:
    UtfChars(JNIEnv* env, jstring value) : env_(env), value_(value) {
        if (value_) chars_ = env_->GetStringUTFChars(value_, nullptr);
    }
    ~UtfChars() {
        if (chars_) env_->ReleaseStringUTFChars(value_, chars_);
    }
    const char* get() const { return chars_ ? chars_ : ""; }
    bool valid() const { return chars_ != nullptr; }
private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_ = nullptr;
};

class BitmapPixels {
public:
    BitmapPixels(JNIEnv* env, jobject bitmap) : env_(env), bitmap_(bitmap) {
        if (AndroidBitmap_getInfo(env_, bitmap_, &info_) != ANDROID_BITMAP_RESULT_SUCCESS) {
            return;
        }
        if (AndroidBitmap_lockPixels(env_, bitmap_, &pixels_) == ANDROID_BITMAP_RESULT_SUCCESS) {
            locked_ = true;
        }
    }
    ~BitmapPixels() {
        if (locked_) AndroidBitmap_unlockPixels(env_, bitmap_);
    }
    bool valid() const { return locked_ && pixels_ != nullptr; }
    const AndroidBitmapInfo& info() const { return info_; }
    void* pixels() const { return pixels_; }
private:
    JNIEnv* env_;
    jobject bitmap_;
    AndroidBitmapInfo info_{};
    void* pixels_ = nullptr;
    bool locked_ = false;
};

struct VulkanGuard {
    bool created = false;
    ~VulkanGuard() {
        if (created) ncnn::destroy_gpu_instance();
    }
};

inline uint8_t clampByte(float value) {
    if (value <= 0.f) return 0;
    if (value >= 255.f) return 255;
    return static_cast<uint8_t>(value + 0.5f);
}

inline void readRgbPixel(
    const uint8_t* row,
    uint32_t x,
    uint32_t format,
    uint8_t& r,
    uint8_t& g,
    uint8_t& b
) {
    if (format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        const uint8_t* p = row + static_cast<size_t>(x) * 4;
        r = p[0];
        g = p[1];
        b = p[2];
    } else {
        const auto* p = reinterpret_cast<const uint16_t*>(row);
        const uint16_t value = p[x];
        const uint8_t r5 = static_cast<uint8_t>((value >> 11) & 0x1f);
        const uint8_t g6 = static_cast<uint8_t>((value >> 5) & 0x3f);
        const uint8_t b5 = static_cast<uint8_t>(value & 0x1f);
        r = static_cast<uint8_t>((r5 << 3) | (r5 >> 2));
        g = static_cast<uint8_t>((g6 << 2) | (g6 >> 4));
        b = static_cast<uint8_t>((b5 << 3) | (b5 >> 2));
    }
}

inline uint16_t packRgb565(uint8_t r, uint8_t g, uint8_t b) {
    return static_cast<uint16_t>(
        ((static_cast<uint16_t>(r) >> 3) << 11) |
        ((static_cast<uint16_t>(g) >> 2) << 5) |
        (static_cast<uint16_t>(b) >> 3)
    );
}

bool validateBitmap(const AndroidBitmapInfo& info, bool output) {
    if (info.width == 0 || info.height == 0) return false;
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return info.stride >= info.width * 4;
    }
    if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        return info.stride >= info.width * 2;
    }
    LOGE("%s bitmap format %u is not supported", output ? "Output" : "Input", info.format);
    return false;
}

} // namespace

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_camui_NcnnSuperResolution_nativeUpscaleInto(
    JNIEnv* env,
    jobject /* instance */,
    jobject sourceBitmap,
    jobject outputBitmap,
    jstring modelParamPath,
    jstring modelBinPath,
    jint requestedTileSize
) {
    if (!sourceBitmap || !outputBitmap) return JNI_FALSE;

    UtfChars paramPath(env, modelParamPath);
    UtfChars binPath(env, modelBinPath);
    if (!paramPath.valid() || !binPath.valid()) {
        LOGE("Could not read NCNN model paths from Java");
        return JNI_FALSE;
    }

    BitmapPixels source(env, sourceBitmap);
    if (!source.valid()) {
        LOGE("Could not lock source bitmap");
        return JNI_FALSE;
    }
    BitmapPixels output(env, outputBitmap);
    if (!output.valid()) {
        LOGE("Could not lock output bitmap");
        return JNI_FALSE;
    }

    const AndroidBitmapInfo& src = source.info();
    const AndroidBitmapInfo& dst = output.info();
    if (!validateBitmap(src, false) || !validateBitmap(dst, true)) return JNI_FALSE;
    if (src.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
        src.format != ANDROID_BITMAP_FORMAT_RGB_565) return JNI_FALSE;
    if (dst.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGE("Output bitmap must use RGB_565 to control memory use");
        return JNI_FALSE;
    }
    if (static_cast<uint64_t>(src.width) * kScale != dst.width ||
        static_cast<uint64_t>(src.height) * kScale != dst.height) {
        LOGE("Output dimensions do not match x2 model: %ux%u -> %ux%u",
             src.width, src.height, dst.width, dst.height);
        return JNI_FALSE;
    }

    const int tileSize = std::clamp(
        requestedTileSize > 0 ? requestedTileSize : kDefaultTileSize,
        64,
        384
    );

    std::lock_guard<std::mutex> inferenceLock(gInferenceMutex);
    VulkanGuard vulkan;
    ncnn::Net net;

    // Vulkan is preferred. NCNN handles unsupported FP16 paths by falling back to
    // the available device features; CPU inference remains available as a safe fallback.
    const int gpuInitResult = ncnn::create_gpu_instance();
    vulkan.created = (gpuInitResult == 0);
    const int gpuCount = vulkan.created ? ncnn::get_gpu_count() : 0;
    const bool useVulkan = gpuInitResult == 0 && gpuCount > 0;
    if (!useVulkan) {
        LOGW("No Vulkan device found; using NCNN CPU inference");
    }

    ncnn::Option option;
    option.num_threads = std::max(2, std::min(8, ncnn::get_cpu_count()));
    option.use_vulkan_compute = useVulkan;
    option.use_fp16_storage = false;
    option.use_fp16_packed = false;
    option.use_fp16_arithmetic = false;
    if (useVulkan) {
        const ncnn::VulkanDevice* device = ncnn::get_gpu_device(0);
        if (device) {
            option.use_fp16_storage = device->info.support_fp16_storage();
            option.use_fp16_packed = device->info.support_fp16_storage();
            option.use_fp16_arithmetic = device->info.support_fp16_storage();
        }
    }
    net.opt = option;

    if (net.load_param(paramPath.get()) != 0) {
        LOGE("Failed to load NCNN param file: %s", paramPath.get());
        return JNI_FALSE;
    }
    if (net.load_model(binPath.get()) != 0) {
        LOGE("Failed to load NCNN model weights: %s", binPath.get());
        return JNI_FALSE;
    }

    LOGI(
        "Real-ESRGAN x2 ready: input=%ux%u tile=%d Vulkan=%s fp16=%s",
        src.width,
        src.height,
        tileSize,
        useVulkan ? "on" : "off",
        option.use_fp16_storage ? "on" : "off"
    );

    const auto* sourceBase = static_cast<const uint8_t*>(source.pixels());
    auto* outputBase = static_cast<uint8_t*>(output.pixels());
    const float normValues[3] = {1.f / 255.f, 1.f / 255.f, 1.f / 255.f};
    const float denormValues[3] = {255.f, 255.f, 255.f};

    for (uint32_t tileY = 0; tileY < src.height; tileY += static_cast<uint32_t>(tileSize)) {
        const int coreHeight = std::min<int>(tileSize, static_cast<int>(src.height - tileY));
        const int tileHeight = coreHeight + 2 * kPrePadding;

        for (uint32_t tileX = 0; tileX < src.width; tileX += static_cast<uint32_t>(tileSize)) {
            const int coreWidth = std::min<int>(tileSize, static_cast<int>(src.width - tileX));
            const int tileWidth = coreWidth + 2 * kPrePadding;
            std::vector<uint8_t> tilePixels(
                static_cast<size_t>(tileWidth) * tileHeight * 3
            );

            for (int y = 0; y < tileHeight; ++y) {
                const int srcY = std::clamp(
                    static_cast<int>(tileY) + y - kPrePadding,
                    0,
                    static_cast<int>(src.height) - 1
                );
                const uint8_t* srcRow = sourceBase + static_cast<size_t>(srcY) * src.stride;
                uint8_t* tileRow = tilePixels.data() + static_cast<size_t>(y) * tileWidth * 3;
                for (int x = 0; x < tileWidth; ++x) {
                    const int srcX = std::clamp(
                        static_cast<int>(tileX) + x - kPrePadding,
                        0,
                        static_cast<int>(src.width) - 1
                    );
                    uint8_t r, g, b;
                    readRgbPixel(srcRow, static_cast<uint32_t>(srcX), src.format, r, g, b);
                    const size_t index = static_cast<size_t>(x) * 3;
                    tileRow[index] = r;
                    tileRow[index + 1] = g;
                    tileRow[index + 2] = b;
                }
            }

            ncnn::Mat input = ncnn::Mat::from_pixels(
                tilePixels.data(),
                ncnn::Mat::PIXEL_RGB,
                tileWidth,
                tileHeight
            );
            if (input.empty()) {
                LOGE("Could not convert input tile to NCNN Mat");
                return JNI_FALSE;
            }
            input.substract_mean_normalize(nullptr, normValues);

            // Vulkan/FP16/thread settings are attached to net.opt before create_extractor().
            ncnn::Extractor extractor = net.create_extractor();
            if (extractor.input("data", input) != 0) {
                LOGE("NCNN input layer 'data' failed");
                return JNI_FALSE;
            }

            ncnn::Mat result;
            if (extractor.extract("output", result) != 0 || result.empty()) {
                LOGE("NCNN output layer 'output' failed at tile (%u,%u)", tileX, tileY);
                return JNI_FALSE;
            }
            if (result.w != tileWidth * kScale || result.h != tileHeight * kScale || result.c != 3) {
                LOGE(
                    "Unexpected Real-ESRGAN x2 tile result: %dx%dx%d for input %dx%d",
                    result.w, result.h, result.c, tileWidth, tileHeight
                );
                return JNI_FALSE;
            }

            result.substract_mean_normalize(nullptr, denormValues);
            const int resultRowBytes = result.w * 3;
            std::vector<uint8_t> resultPixels(
                static_cast<size_t>(result.h) * resultRowBytes
            );
            result.to_pixels(resultPixels.data(), ncnn::Mat::PIXEL_RGB);

            const int crop = kPrePadding * kScale;
            for (int y = 0; y < coreHeight * kScale; ++y) {
                const int sourceResultY = y + crop;
                const uint8_t* resultRow =
                    resultPixels.data() + static_cast<size_t>(sourceResultY) * resultRowBytes +
                    static_cast<size_t>(crop) * 3;
                uint8_t* outputRow = outputBase +
                    static_cast<size_t>(tileY * kScale + y) * dst.stride +
                    static_cast<size_t>(tileX * kScale) * 2;

                auto* outputPixels = reinterpret_cast<uint16_t*>(outputRow);
                for (int x = 0; x < coreWidth * kScale; ++x) {
                    const size_t sourceIndex = static_cast<size_t>(x) * 3;
                    outputPixels[x] = packRgb565(
                        resultRow[sourceIndex],
                        resultRow[sourceIndex + 1],
                        resultRow[sourceIndex + 2]
                    );
                }
            }
        }

        LOGI(
            "AI super-resolution progress: %u/%u rows",
            std::min<uint32_t>(tileY + static_cast<uint32_t>(tileSize), src.height),
            src.height
        );
    }

    net.clear();
    LOGI("Real-ESRGAN x2 tile processing completed");
    return JNI_TRUE;
}
